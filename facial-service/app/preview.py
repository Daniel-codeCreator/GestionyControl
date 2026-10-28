from __future__ import annotations

import logging
from dataclasses import dataclass

import cv2
import numpy as np

from .errors import CaptureCancelledError, PreviewUnavailableError


logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class FaceOverlay:
    face: object
    color: tuple[int, int, int]


class PreviewWindow:
    """Small HighGUI adapter owned by the runner's main thread."""

    def __init__(self, title: str):
        self.title = title
        self.initialized = False

    def _initialize(self) -> None:
        if self.initialized:
            return
        gui_line = next(
            (line.strip() for line in cv2.getBuildInformation().splitlines() if "GUI:" in line),
            "GUI: unknown",
        )
        if "NONE" in gui_line.upper():
            raise PreviewUnavailableError(
                "La instalación de OpenCV no incluye soporte GUI para mostrar la cámara."
            )
        try:
            cv2.namedWindow(self.title, cv2.WINDOW_NORMAL)
            cv2.resizeWindow(self.title, 960, 540)
            self._centrar_y_elevar()
            cv2.waitKey(1)
        except cv2.error as exc:
            raise PreviewUnavailableError(
                f"No se pudo inicializar la ventana de cámara ({gui_line}). "
                "Ejecute python -m scripts.preview_test para diagnosticarla."
            ) from exc
        self.initialized = True
        logger.info("Preview initialized title=%s", self.title)

    def _centrar_y_elevar(self) -> None:
        """Centra la ventana de la cámara en la pantalla y la trae al frente.

        Sin esto la ventana de OpenCV aparece en una posicion arbitraria y
        puede quedar detrás de la ventana de la aplicación Java.
        """
        try:
            import ctypes

            user32 = ctypes.windll.user32
            try:
                user32.SetProcessDPIAware()
            except Exception:
                pass

            ancho_pantalla = user32.GetSystemMetrics(0)
            alto_pantalla = user32.GetSystemMetrics(1)

            x = max(0, (ancho_pantalla - 960) // 2)
            y = max(0, (alto_pantalla - 540) // 2)
            cv2.moveWindow(self.title, x, y)

            # Traer la ventana al frente (Windows bloquea el foco a veces).
            hwnd = user32.FindWindowW(None, self.title)
            if hwnd:
                user32.ShowWindow(hwnd, 5)  # SW_SHOW
                user32.SetWindowPos(hwnd, -1, 0, 0, 0, 0, 0x0001 | 0x0002)  # HWND_TOPMOST
                user32.SetWindowPos(hwnd, -2, 0, 0, 0, 0, 0x0001 | 0x0002)  # HWND_NOTOPMOST
                user32.SetForegroundWindow(hwnd)

            logger.info("Preview window centered at %sx%s", x, y)
        except Exception as exc:  # pragma: no cover - depende del sistema
            logger.warning("No se pudo centrar la ventana de la cámara: %s", exc)

    def show(self, frame: np.ndarray, overlays: list[FaceOverlay], lines: list[str]) -> int:
        self._initialize()
        preview = frame.copy()
        for overlay in overlays:
            bbox = np.asarray(getattr(overlay.face, "bbox", []), dtype=int)
            if bbox.size != 4:
                continue
            x1, y1, x2, y2 = bbox.tolist()
            cv2.rectangle(preview, (x1, y1), (x2, y2), overlay.color, 3)
        self._draw_overlay(preview, lines)
        try:
            cv2.imshow(self.title, preview)
            key = cv2.waitKey(1) & 0xFF
            if self._is_closed():
                raise CaptureCancelledError()
            return key
        except CaptureCancelledError:
            raise
        except cv2.error as exc:
            raise PreviewUnavailableError(
                "La ventana de cámara dejó de estar disponible; la captura fue detenida."
            ) from exc

    def _is_closed(self) -> bool:
        try:
            visible = cv2.getWindowProperty(self.title, cv2.WND_PROP_VISIBLE)
            return visible < 1
        except cv2.error as exc:
            raise PreviewUnavailableError("No se pudo comprobar la ventana de cámara.") from exc

    @staticmethod
    def _draw_overlay(frame: np.ndarray, lines: list[str]) -> None:
        line_height = 29
        max_lines = min(len(lines), 14)
        panel_height = 15 + max_lines * line_height
        panel = frame[0:panel_height, 0:min(frame.shape[1], 760)]
        if panel.size:
            dark = np.zeros_like(panel)
            cv2.addWeighted(panel, 0.35, dark, 0.65, 0, panel)
        for index, line in enumerate(lines[:max_lines]):
            color = (0, 255, 255) if index == 0 else (255, 255, 255)
            cv2.putText(
                frame,
                line,
                (18, 30 + index * line_height),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.68,
                color,
                2,
                cv2.LINE_AA,
            )

    def close(self) -> None:
        if not self.initialized:
            return
        try:
            cv2.destroyWindow(self.title)
            cv2.waitKey(1)
        except cv2.error:
            pass
        self.initialized = False


def preview_probe(frame: np.ndarray, title: str = "Universidad Asistencia - Preview Test") -> bool:
    window = PreviewWindow(title)
    try:
        window.show(frame, [], ["PREVIEW TEST", "Video en vivo detectado", "ESC - Cerrar"])
        return True
    finally:
        window.close()
