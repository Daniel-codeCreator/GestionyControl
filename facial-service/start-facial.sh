#!/usr/bin/env bash
set -euo pipefail
SERVICE_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SERVICE_ROOT"
PYTHON_BIN="$SERVICE_ROOT/.venv/bin/python"
if [[ ! -x "$PYTHON_BIN" ]]; then
  echo "Cree primero el entorno con: ./setup-facial-macos.sh o ./setup-facial-linux.sh"
  exit 1
fi
if [[ ! -f "$SERVICE_ROOT/.env" ]]; then
  echo "Falta facial-service/.env. Copie .env.example a .env y configure sus valores locales."
  exit 1
fi
if command -v brew >/dev/null 2>&1; then
  OPENSSL_PREFIX="$(brew --prefix openssl@3 2>/dev/null || true)"
  if [[ -n "$OPENSSL_PREFIX" ]]; then
    export DYLD_LIBRARY_PATH="$OPENSSL_PREFIX/lib${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"
  fi
fi
exec "$PYTHON_BIN" -m scripts.start
