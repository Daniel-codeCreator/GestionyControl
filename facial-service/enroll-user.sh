#!/usr/bin/env bash
set -euo pipefail

SERVICE_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PYTHON_BIN="$SERVICE_ROOT/.venv/bin/python"
if [[ ! -x "$PYTHON_BIN" ]]; then
  echo "Falta .venv. Ejecute ./setup-facial-macos.sh o ./setup-facial-linux.sh."
  exit 1
fi
if [[ $# -eq 2 && "$1" == "--user-id" && "$2" =~ ^[0-9]+$ ]]; then
  exec "$PYTHON_BIN" -m scripts.enroll_user --user-id "$2"
fi
if [[ $# -eq 3 && "$1" == "--user-id" && "$2" =~ ^[0-9]+$ && "$3" == "--replace" ]]; then
  exec "$PYTHON_BIN" -m scripts.enroll_user --user-id "$2" --replace
fi
if [[ $# -eq 2 && "$2" == "--replace" ]]; then
  if [[ "$1" =~ ^[0-9]+$ ]]; then
    exec "$PYTHON_BIN" -m scripts.enroll_user --user-id "$1" --replace
  fi
  exec "$PYTHON_BIN" -m scripts.enroll_user --username "$1" --replace
fi
if [[ $# -ne 1 ]]; then
  echo "Uso: ./enroll-user.sh <user-id|username> [--replace] | ./enroll-user.sh --user-id <id>"
  exit 2
fi
if [[ "$1" =~ ^[0-9]+$ ]]; then
  exec "$PYTHON_BIN" -m scripts.enroll_user --user-id "$1"
fi
exec "$PYTHON_BIN" -m scripts.enroll_user --username "$1"
