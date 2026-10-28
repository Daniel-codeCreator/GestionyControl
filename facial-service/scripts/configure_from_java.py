"""Sincroniza el .env del servicio facial con la configuracion de la base de datos.

La aplicacion Java lee la conexion de conexion.xml (src/main/resources/conexion.xml
o el que este junto al ejecutable). Este script busca ese archivo en las rutas
habituales y copia host, puerto, base y usuario al .env del servicio facial.

Nunca imprime la contrasena y nunca modifica FACIAL_ENCRYPTION_KEY: cambiarla
invalidaria los rostros ya registrados.
"""

from __future__ import annotations

import argparse
import base64
import os
import re
import secrets
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


SERVICE_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = SERVICE_ROOT.parent
ENV_FILE = SERVICE_ROOT / ".env"
ENV_TEMPLATE = SERVICE_ROOT / ".env.example"

# Rutas donde puede estar el XML de conexion, segun como seDistribution se ejecute.
RUTAS_CONFIG = (
    Path.cwd() / "conexion.xml",
    SERVICE_ROOT / "conexion.xml",
    PROJECT_ROOT / "conexion.xml",
    PROJECT_ROOT / "src" / "main" / "resources" / "conexion.xml",
    PROJECT_ROOT / "publicshell" / "conexion.xml",
    PROJECT_ROOT / "dist" / "conexion.xml",
    PROJECT_ROOT / "publicshell" / "facial-service" / "conexion.xml",
    PROJECT_ROOT / "dist" / "facial-service" / "conexion.xml",
)


def _buscar_config_xml() -> Path | None:
    for ruta in RUTAS_CONFIG:
        if ruta.is_file():
            return ruta
    return None


def _leer_xml(ruta: Path) -> dict[str, str]:
    try:
        raiz = ET.parse(ruta).getroot()
    except ET.ParseError as exc:
        raise ValueError(f"El archivo {ruta.name} no es un XML valido: {exc}") from exc

    nodo = raiz.find(".//db")
    if nodo is None:
        raise ValueError(f"El archivo {ruta.name} no contiene el bloque <db>.")

    valores: dict[str, str] = {}
    for campo in ("host", "port", "database", "user", "password"):
        elemento = nodo.find(campo)
        if elemento is not None and elemento.text:
            valores[campo] = elemento.text.strip()
    return valores


def _leer_valores(texto: str) -> dict[str, str]:
    valores: dict[str, str] = {}
    for linea in texto.splitlines():
        coincidencia = re.match(r"^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$", linea)
        if coincidencia:
            valores[coincidencia.group(1)] = coincidencia.group(2)
    return valores


def _escribir_valores(original: str, valores: dict[str, str]) -> str:
    lineas = original.splitlines()
    escritas: set[str] = set()
    salida: list[str] = []

    for linea in lineas:
        coincidencia = re.match(r"^(\s*)([A-Za-z_][A-Za-z0-9_]*)\s*=\s*.*$", linea)
        if coincidencia and coincidencia.group(2) in valores:
            clave = coincidencia.group(2)
            salida.append(f"{coincidencia.group(1)}{clave}={valores[clave]}")
            escritas.add(clave)
        else:
            salida.append(linea)

    for clave, valor in valores.items():
        if clave not in escritas:
            salida.append(f"{clave}={valor}")

    return "\n".join(salida).rstrip() + "\n"


def _base_de_datos() -> tuple[dict[str, str], str]:
    """Devuelve los datos de conexion y de donde se obtuvo el archivo."""
    ruta = _buscar_config_xml()

    if ruta is None:
        # Sin XML se cae a las variables de entorno del propio servicio.
        return (
            {
                "DB_HOST": os.getenv("DB_HOST", "127.0.0.1"),
                "DB_PORT": os.getenv("DB_PORT", "1433"),
                "DB_NAME": os.getenv("DB_NAME", "PROYCPROGRA2"),
                "DB_USER": os.getenv("DB_USER", "sa"),
                "DB_PASSWORD": os.getenv("DB_PASSWORD", ""),
            },
            "variables de entorno",
        )

    datos = _leer_xml(ruta)
    base = {
        "DB_HOST": os.getenv("DB_HOST") or datos.get("host", "127.0.0.1"),
        "DB_PORT": os.getenv("DB_PORT") or datos.get("port", "1433"),
        "DB_NAME": os.getenv("DB_NAME") or datos.get("database", "PROYCPROGRA2"),
        "DB_USER": os.getenv("DB_USER") or datos.get("user", "sa"),
        "DB_PASSWORD": os.getenv("DB_PASSWORD") or datos.get("password", ""),
    }
    return base, str(ruta)


def _password_del_contenedor() -> str | None:
    try:
        contenedores = subprocess.run(
            ["docker", "ps", "-q"], capture_output=True, text=True, check=True
        ).stdout.splitlines()
    except (OSError, subprocess.CalledProcessError):
        return None

    for contenedor in contenedores:
        try:
            salida = subprocess.run(
                ["docker", "inspect", "--format", "{{range .Config.Env}}{{println .}}{{end}}", contenedor],
                capture_output=True,
                text=True,
                check=True,
            ).stdout
        except (OSError, subprocess.CalledProcessError):
            continue

        for linea in salida.splitlines():
            clave, separador, valor = linea.partition("=")
            if separador and clave in {"SA_PASSWORD", "MSSQL_SA_PASSWORD", "DB_PASSWORD"} and valor:
                return valor
    return None


def _clave_valida(valor: str | None) -> bool:
    if not valor or valor in {"CHANGE_ME", "replace-with-a-32-byte-base64-key"}:
        return False
    try:
        return len(base64.b64decode(valor, validate=True)) in {16, 24, 32}
    except Exception:
        return False


def main() -> None:
    analizador = argparse.ArgumentParser(
        description="Configura facial-service/.env con la base de datos de conexion.xml."
    )
    analizador.add_argument(
        "--from-docker",
        action="store_true",
        help="Busca SA_PASSWORD únicamente en variables del contenedor SQL Server local.",
    )
    analizador.add_argument(
        "--generate-key",
        action="store_true",
        help="Genera una clave solo si .env no contiene una clave válida.",
    )
    argumentos = analizador.parse_args()

    plantilla = ENV_FILE.read_text(encoding="utf-8") if ENV_FILE.is_file() else ENV_TEMPLATE.read_text(encoding="utf-8")
    actuales = _leer_valores(plantilla)

    base, origen = _base_de_datos()
    valores = dict(base)

    if not valores.get("DB_PASSWORD"):
        valores["DB_PASSWORD"] = actuales.get("DB_PASSWORD", "")
        if not valores["DB_PASSWORD"] and argumentos.from_docker:
            valores["DB_PASSWORD"] = _password_del_contenedor() or ""

    if not valores.get("DB_PASSWORD"):
        raise SystemExit(
            "No se encontro la contraseña de la base. Revise conexion.xml o use --from-docker."
        )

    clave_actual = actuales.get("FACIAL_ENCRYPTION_KEY")
    if _clave_valida(clave_actual):
        valores["FACIAL_ENCRYPTION_KEY"] = clave_actual
    elif argumentos.generate_key:
        valores["FACIAL_ENCRYPTION_KEY"] = base64.b64encode(secrets.token_bytes(32)).decode("ascii")
    else:
        raise SystemExit("No hay una FACIAL_ENCRYPTION_KEY válida. Ejecute con --generate-key.")

    contenido = _escribir_valores(plantilla, valores)
    ENV_FILE.write_text(contenido, encoding="utf-8")
    try:
        ENV_FILE.chmod(0o600)
    except OSError:
        pass

    print(f"facial-service/.env configurado desde {origen}.")
    print("La contraseña y la clave no se muestran en consola.")


if __name__ == "__main__":
    main()
