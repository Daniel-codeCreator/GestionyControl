from __future__ import annotations

import json
import logging
import platform
import re
import subprocess
import threading
from contextlib import contextmanager
from dataclasses import dataclass
from pathlib import Path
from typing import Iterator

import cv2

from .config import Settings, settings
from .errors import CameraBusyError, CameraUnavailableError


logger = logging.getLogger(__name__)
CAMERA_LOCK = threading.Lock()


@dataclass(frozen=True)
class CameraInfo:
    index: int
    name: str
    backend: int
    backend_name: str
    can_read_frame: bool
    reason: str = ""


def camera_backends() -> list[int]:
    names = {
        "Windows": ("CAP_DSHOW", "CAP_MSMF"),
        "Darwin": ("CAP_AVFOUNDATION",),
        "Linux": ("CAP_V4L2",),
    }
    backends: list[int] = []
    for name in names.get(platform.system(), ()):
        backend = getattr(cv2, name, None)
        if backend is not None and backend not in backends:
            backends.append(backend)
    backends.append(getattr(cv2, "CAP_ANY", 0))
    return backends


def backend_name(backend: int) -> str:
    names = {
        getattr(cv2, "CAP_AVFOUNDATION", -1): "AVFoundation",
        getattr(cv2, "CAP_DSHOW", -1): "DirectShow",
        getattr(cv2, "CAP_MSMF", -1): "Media Foundation",
        getattr(cv2, "CAP_V4L2", -1): "V4L2",
        getattr(cv2, "CAP_ANY", 0): "Any",
    }
    return names.get(backend, str(backend))


def mac_camera_names() -> list[str]:
    """Best-effort camera names without adding an AVFoundation dependency."""
    if platform.system() != "Darwin":
        return []
    try:
        completed = subprocess.run(
            ["system_profiler", "SPCameraDataType"],
            capture_output=True,
            text=True,
            timeout=5,
            check=False,
        )
    except (OSError, subprocess.SubprocessError):
        return []
    names: list[str] = []
    for line in completed.stdout.splitlines():
        match = re.match(r"^\s{4}([^:]+):\s*$", line)
        if match and match.group(1).strip() not in {"Camera", ""}:
            names.append(match.group(1).strip())
    return names


def _camera_name(index: int, names: list[str]) -> str:
    return names[index] if index < len(names) else f"Camera index {index}"


def _name_score(name: str, configuration: Settings) -> tuple[int, str]:
    lowered = name.lower()
    hint = configuration.camera_name_hint.lower()
    if hint and hint in lowered:
        return (-100, f"CAMERA_NAME_HINT={configuration.camera_name_hint}")
    if any(token in lowered for token in ("iphone", "continuity", "ipad")):
        return (100, "Continuity Camera queda como último fallback")
    if any(token in lowered for token in ("facetime", "macbook", "built-in", "built in", "mac camera")):
        return (0, "cámara integrada preferida")
    return (50, "cámara disponible")


def _candidate_indices(configuration: Settings, names: list[str]) -> list[tuple[int, str]]:
    if configuration.camera_index is not None:
        return [(configuration.camera_index, _camera_name(configuration.camera_index, names))]
    count = len(names) if names else 6
    candidates = [(index, _camera_name(index, names)) for index in range(count)]
    return sorted(candidates, key=lambda item: _name_score(item[1], configuration)[0])


def _read_usable_frames(capture: cv2.VideoCapture, attempts: int = 3) -> tuple[bool, float]:
    """Lee unos frames de calentamiento y dice si traen imagen real.

    Algunas camaras (sobre todo UVC con DirectShow) entregan el buffer vacio
    cuando se fuerza una resolucion que el driver no soporta de verdad: el
    frame llega completamente negro aunque la camara este funcionando.
    """
    mean = 0.0
    for _ in range(attempts):
        ok, frame = capture.read()
        if not ok or frame is None:
            return False, 0.0
        mean = float(frame.mean())
        if mean >= 3.0:
            return True, mean
    return False, mean


# Camaras que devolvieron frames negros con la resolucion forzada.
# Se guardan en disco porque cada sesion facial corre en un proceso nuevo.
_MARCADOR_NATIVA = Path(__file__).resolve().parents[1] / "tmp" / "camara_resolucion_nativa.json"


def _cargar_nativas() -> set[tuple[int, int]]:
    try:
        datos = json.loads(_MARCADOR_NATIVA.read_text(encoding="utf-8"))
        return {(int(indice), int(backend)) for indice, backend in datos.get("nativas", [])}
    except (OSError, ValueError, TypeError, AttributeError):
        return set()


def _guardar_nativas(nativas: set[tuple[int, int]]) -> None:
    try:
        _MARCADOR_NATIVA.parent.mkdir(parents=True, exist_ok=True)
        _MARCADOR_NATIVA.write_text(
            json.dumps({"nativas": [[indice, backend] for indice, backend in sorted(nativas)]}),
            encoding="utf-8",
        )
    except OSError:
        logger.warning("No se pudo guardar la preferencia de resolución de la cámara.")


_NATIVAS = _cargar_nativas()


def _abrir_nativo(index: int, backend: int) -> cv2.VideoCapture | None:
    """Abre la cámara con el formato que el dispositivo entregue por defecto."""
    capture = cv2.VideoCapture(index, backend)
    if not capture.isOpened():
        capture.release()
        return None
    return capture


def _abrir_con_configuracion(
        index: int,
        backend: int,
        configuration: Settings
) -> cv2.VideoCapture | None:
    """Abre la cámara forzando el tamaño configurado en el .env."""
    capture = cv2.VideoCapture(index, backend)
    if not capture.isOpened():
        capture.release()
        return None
    capture.set(cv2.CAP_PROP_FRAME_WIDTH, configuration.camera_width)
    capture.set(cv2.CAP_PROP_FRAME_HEIGHT, configuration.camera_height)
    return capture


def _open(
        index: int,
        backend: int,
        configuration: Settings,
        adaptativo: bool = True
) -> tuple[cv2.VideoCapture, bool] | None:
    """Abre la cámara adaptándose al dispositivo.

    1) Se abre en el formato nativo y se comprueba que llegue imagen.
    2) Si el dispositivo lo permite, se aplica el tamaño configurado en el .env.
    3) Si ese tamaño rompe la imagen, se vuelve al nativo y se recuerda, para
       no volver a perder tiempo en las siguientes sesiones.
    Así funciona con cualquier cámara, sin importar el modelo ni el driver.
    """
    ya_sabe_nativa = (index, backend) in _NATIVAS

    if ya_sabe_nativa:
        capture = _abrir_nativo(index, backend)
        if capture is None:
            return None
        usable, mean = _read_usable_frames(capture)
        if usable:
            return capture, True
        capture.release()
        return None

    capture = _abrir_nativo(index, backend)
    if capture is None:
        return None

    usable, mean = _read_usable_frames(capture)

    if usable:
        if not adaptativo:
            return capture, True

        # Probamos el tamaño configurado sobre la misma sesión.
        capture.set(cv2.CAP_PROP_FRAME_WIDTH, configuration.camera_width)
        capture.set(cv2.CAP_PROP_FRAME_HEIGHT, configuration.camera_height)

        usable_configurado, mean_configurado = _read_usable_frames(capture)
        if usable_configurado:
            return capture, True

        # El dispositivo no aguanta ese tamaño: reabrimos en modo nativo.
        capture.release()
        capture = _abrir_nativo(index, backend)
        if capture is None:
            return None

        usable, mean = _read_usable_frames(capture)
        if not usable:
            capture.release()
            return None

        _NATIVAS.add((index, backend))
        _guardar_nativas(_NATIVAS)
        logger.warning(
            "La camara %s no entrega imagen a %sx%s con %s; se usa su resolucion nativa %sx%s.",
            index,
            configuration.camera_width,
            configuration.camera_height,
            backend_name(backend),
            int(capture.get(cv2.CAP_PROP_FRAME_WIDTH)),
            int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT)),
        )
        return capture, True

    # El modo nativo tampoco dio imagen: probamos el tamaño configurado.
    capture.release()
    capture = _abrir_con_configuracion(index, backend, configuration)
    if capture is None:
        return None

    usable, mean = _read_usable_frames(capture)
    if not usable:
        capture.release()
        return None

    return capture, True


def list_cameras(
        configuration: Settings = settings,
        max_indices: int = 8,
        detener_en_el_primero: bool = False
) -> list[CameraInfo]:
    names = mac_camera_names()
    indices = _candidate_indices(configuration, names)
    if configuration.camera_index is None:
        indices = indices[:max_indices]
    found: list[CameraInfo] = []
    for index, name in indices:
        for backend in camera_backends():
            opened = _open(index, backend, configuration)
            if opened is None:
                continue
            capture, can_read = opened
            try:
                actual_backend = capture.getBackendName() if hasattr(capture, "getBackendName") else backend_name(backend)
            except cv2.error:
                actual_backend = backend_name(backend)
            capture.release()
            _, reason = _name_score(name, configuration)
            if configuration.camera_index is not None:
                reason = "CAMERA_INDEX explícito"
            info = CameraInfo(index, name, backend, actual_backend, can_read, reason)
            found.append(info)
            # No hace falta seguir escaneando índices: la primera cámara
            # utilizable es la que se usará y cada apertura extra tarda segundos.
            if detener_en_el_primero and can_read:
                return found
            break
    return found


def select_camera(configuration: Settings = settings) -> CameraInfo:
    cameras = list_cameras(configuration, detener_en_el_primero=True)
    usable = [camera for camera in cameras if camera.can_read_frame]
    if usable:
        selected = usable[0]
        logger.info(
            "Selected camera index=%s name=%s backend=%s reason=%s",
            selected.index,
            selected.name,
            selected.backend_name,
            selected.reason,
        )
        return selected
    if configuration.camera_index is not None:
        detail = f"No se pudo leer la cámara configurada {configuration.camera_index}."
    else:
        detail = "No se encontró una cámara que entregue frames."
    raise CameraUnavailableError(detail)


@contextmanager
def open_camera_session(configuration: Settings = settings) -> Iterator[tuple[cv2.VideoCapture, CameraInfo]]:
    if not CAMERA_LOCK.acquire(blocking=False):
        raise CameraBusyError()
    capture: cv2.VideoCapture | None = None
    try:
        selected = select_camera(configuration)
        opened = _open(selected.index, selected.backend, configuration)
        if opened is None:
            raise CameraUnavailableError(f"La cámara seleccionada dejó de estar disponible: {selected.name}.")
        capture, _ = opened
        logger.info(
            "Camera opened index=%s name=%s backend=%s resolution=%sx%s",
            selected.index,
            selected.name,
            selected.backend_name,
            int(capture.get(cv2.CAP_PROP_FRAME_WIDTH)),
            int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT)),
        )
        yield capture, selected
    finally:
        if capture is not None:
            capture.release()
        try:
            cv2.destroyAllWindows()
        except cv2.error:
            pass
        CAMERA_LOCK.release()


@contextmanager
def open_camera(configuration: Settings = settings) -> Iterator[cv2.VideoCapture]:
    with open_camera_session(configuration) as (capture, _):
        yield capture
