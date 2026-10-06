#!/usr/bin/env bash
set -euo pipefail

SERVICE_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_BIN="$SERVICE_ROOT/.venv/bin/python"
if [[ ! -x "$PYTHON_BIN" ]]; then
  echo "Falta .venv. Ejecute ./setup-facial-macos.sh o ./setup-facial-linux.sh."
  exit 1
fi
cd "$SERVICE_ROOT"
"$PYTHON_BIN" -m scripts.doctor
"$PYTHON_BIN" -m scripts.health_check
echo "READY FOR DEMO"
