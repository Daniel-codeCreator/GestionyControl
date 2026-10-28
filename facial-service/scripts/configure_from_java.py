"""Create the local facial-service .env from Java configuration and local SQL Server.

This utility never prints or stores secrets outside facial-service/.env. It is
intended for local setup only; the generated file is ignored by Git.
"""

from __future__ import annotations

import argparse
import base64
import os
import re
import secrets
import subprocess
from pathlib import Path
import os
import re
import xml.etree.ElementTree as ET


SERVICE_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = SERVICE_ROOT.parent
JAVA_CONFIG = PROJECT_ROOT / "src" / "main" / "java" / "universidad" / "asistencia" / "config" / "DatabaseConnection.java"
ENV_FILE = SERVICE_ROOT / ".env"
ENV_TEMPLATE = SERVICE_ROOT / ".env.example"


def _java_default(name: str, fallback: str) -> str:
    source = JAVA_CONFIG.read_text(encoding="utf-8")
    pattern = re.compile(
        rf'private\s+static\s+final\s+String\s+{re.escape(name)}\s*=\s*'
        rf'configuracion\(\s*"[^"]+"\s*,\s*"[^"]+"\s*,\s*"((?:\\.|[^"\\])*)"\s*\)\s*;',
        re.DOTALL,
    )
    match = pattern.search(source)
    return match.group(1) if match else fallback


def _java_connection() -> dict[str, str]:
    url = os.getenv("DB_URL") or _java_default(
        "URL",
        "jdbc:sqlserver://localhost:1433;databaseName=UniversidadAsistenciaDB;",
    )
    match = re.search(r"jdbc:sqlserver://([^:;]+)(?::(\d+))?;", url, re.IGNORECASE)
    database = re.search(r"databaseName=([^;]+)", url, re.IGNORECASE)
    if match is None or database is None:
        raise ValueError("No se pudo interpretar la URL JDBC de DatabaseConnection.java.")
    host = os.getenv("DB_HOST") or match.group(1)
    # Keep Java's local endpoint equivalent while avoiding macOS ODBC's slow
    # localhost/IPv6 fallback in some Homebrew configurations.
    if host.lower() == "localhost":
        host = "127.0.0.1"
    return {
        "DB_HOST": host,
        "DB_PORT": os.getenv("DB_PORT") or match.group(2) or "1433",
        "DB_NAME": os.getenv("DB_NAME") or database.group(1),
        "DB_USER": os.getenv("DB_USER") or _java_default("USER", "sa"),
    }


def _docker_password() -> str | None:
    try:
        containers = subprocess.run(
            ["docker", "ps", "-q"], capture_output=True, text=True, check=True
        ).stdout.splitlines()
    except (OSError, subprocess.CalledProcessError):
        return None
    for container in containers:
        try:
            output = subprocess.run(
                ["docker", "inspect", "--format", "{{range .Config.Env}}{{println .}}{{end}}", container],
                capture_output=True,
                text=True,
                check=True,
            ).stdout
        except (OSError, subprocess.CalledProcessError):
            continue
        for line in output.splitlines():
            key, separator, value = line.partition("=")
            if separator and key in {"SA_PASSWORD", "MSSQL_SA_PASSWORD", "DB_PASSWORD"} and value:
                return value
    return None


def _valid_key(value: str | None) -> bool:
    if not value or value in {"CHANGE_ME", "replace-with-a-32-byte-base64-key"}:
        return False
    try:
        return len(base64.b64decode(value, validate=True)) in {16, 24, 32}
    except Exception:
        return False


def sync_env_from_xml():
    # Rutas posibles del conexion.xml
    xml_paths = [
        os.path.join("src", "main", "resources", "conexion.xml"),
        os.path.join("src", "main", "java", "Proyecto", "universidad", "Config", "conexion.xml"),
        "conexion.xml"
    ]

    xml_file = None
    for path in xml_paths:
        if os.path.exists(path):
            xml_file = path
            break

    if not xml_file:
        print("[sync_env] No se encontro conexion.xml. Manteniendo .env actual.")
        return

    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()
        db_elem = root.find("db")

        if db_elem is None:
            return

        host = db_elem.find("host").text.strip()
        port = db_elem.find("port").text.strip()
        database = db_elem.find("database").text.strip()
        user = db_elem.find("user").text.strip()
        password = db_elem.find("password").text.strip()

        env_path = os.path.join("facial-service", ".env")
        if not os.path.exists(env_path):
            env_path = ".env"

        if not os.path.exists(env_path):
            print("[sync_env] Archivo .env no encontrado.")
            return

        with open(env_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Reemplazar variables manteniendo el resto de configuraciones faciales intactas
        content = re.sub(r"^DB_HOST=.*", f"DB_HOST={host}", content, flags=re.MULTILINE)
        content = re.sub(r"^DB_PORT=.*", f"DB_PORT={port}", content, flags=re.MULTILINE)
        content = re.sub(r"^DB_NAME=.*", f"DB_NAME={database}", content, flags=re.MULTILINE)
        content = re.sub(r"^DB_USER=.*", f"DB_USER={user}", content, flags=re.MULTILINE)
        content = re.sub(r"^DB_PASSWORD=.*", f"DB_PASSWORD={password}", content, flags=re.MULTILINE)

        with open(env_path, "w", encoding="utf-8") as f:
            f.write(content)

        print(f"[sync_env] .env actualizado exitosamente con la BD de conexion.xml ({host}:{port})")

    except Exception as e:
        print(f"[sync_env] Error al sincronizar .env desde XML: {e}")

if __name__ == "__main__":
    sync_env_from_xml()

def _read_values(text: str) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in text.splitlines():
        match = re.match(r"^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$", line)
        if match:
            values[match.group(1)] = match.group(2)
    return values


def _write_values(original: str, values: dict[str, str]) -> None:
    lines = original.splitlines()
    written: set[str] = set()
    output: list[str] = []
    for line in lines:
        match = re.match(r"^(\s*)([A-Za-z_][A-Za-z0-9_]*)(\s*=\s*).*$", line)
        if match and match.group(2) in values:
            key = match.group(2)
            output.append(f"{match.group(1)}{key}{match.group(3)}{values[key]}")
            written.add(key)
        else:
            output.append(line)
    for key, value in values.items():
        if key not in written:
            output.append(f"{key}={value}")
    ENV_FILE.write_text("\n".join(output).rstrip() + "\n", encoding="utf-8")
    try:
        ENV_FILE.chmod(0o600)
    except OSError:
        pass


def main() -> None:
    parser = argparse.ArgumentParser(description="Configura facial-service/.env desde la configuración Java.")
    parser.add_argument(
        "--from-docker",
        action="store_true",
        help="Busca SA_PASSWORD únicamente en variables del contenedor SQL Server local.",
    )
    parser.add_argument(
        "--generate-key",
        action="store_true",
        help="Genera una clave solo si .env no contiene una clave válida.",
    )
    args = parser.parse_args()

    if not JAVA_CONFIG.is_file():
        raise SystemExit(f"No existe la configuración Java: {JAVA_CONFIG}")
    template = ENV_FILE.read_text(encoding="utf-8") if ENV_FILE.is_file() else ENV_TEMPLATE.read_text(encoding="utf-8")
    current = _read_values(template)
    values = _java_connection()
    password = os.getenv("DB_PASSWORD") or current.get("DB_PASSWORD")
    if not password or password in {"CHANGE_ME", "change-me"}:
        password = _docker_password() if args.from_docker else None
    if not password:
        raise SystemExit(
            "No se encontró DB_PASSWORD. Configure la variable de entorno o ejecute con --from-docker "
            "si el contenedor SQL Server local contiene la credencial."
        )
    values["DB_PASSWORD"] = password

    existing_key = current.get("FACIAL_ENCRYPTION_KEY")
    if _valid_key(existing_key):
        values["FACIAL_ENCRYPTION_KEY"] = existing_key or ""
    elif args.generate_key:
        values["FACIAL_ENCRYPTION_KEY"] = base64.b64encode(secrets.token_bytes(32)).decode("ascii")
    else:
        raise SystemExit("No hay una FACIAL_ENCRYPTION_KEY válida. Ejecute con --generate-key.")

    _write_values(template, values)
    print("facial-service/.env configurado desde DatabaseConnection.java y la infraestructura local.")
    print("La contraseña y la clave no se muestran en consola.")


if __name__ == "__main__":
    main()
