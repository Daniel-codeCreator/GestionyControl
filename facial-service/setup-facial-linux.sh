#!/usr/bin/env bash
set -euo pipefail

SERVICE_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_BIN="${PYTHON_BIN:-python3.12}"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  echo "No se encontró $PYTHON_BIN. Instale Python 3.12 y python3.12-venv con el gestor de paquetes de su distribución."
  exit 1
fi

cd "$SERVICE_ROOT"
"$PYTHON_BIN" -m venv .venv
VENV_PYTHON="$SERVICE_ROOT/.venv/bin/python"
"$VENV_PYTHON" -m pip install --upgrade pip
"$VENV_PYTHON" -m pip install -r requirements.txt
if "$VENV_PYTHON" -m pip show opencv-python-headless >/dev/null 2>&1; then
  "$VENV_PYTHON" -m pip uninstall -y opencv-python-headless
  "$VENV_PYTHON" -m pip install --force-reinstall --no-deps opencv-python==4.10.0.84
fi
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Se creó .env desde .env.example. Configure DB_PASSWORD y FACIAL_ENCRYPTION_KEY antes de usar el servicio."
fi
echo "Diagnóstico inicial (puede mostrar FAIL si SQL Server, ODBC, webcam o el modelo aún no están listos):"
"$VENV_PYTHON" -m scripts.doctor || true
