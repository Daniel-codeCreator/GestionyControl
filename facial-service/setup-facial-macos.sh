#!/usr/bin/env bash
set -Eeuo pipefail

setup_incomplete() {
  echo "SETUP INCOMPLETE: el paso anterior falló. Revise el mensaje anterior y vuelva a ejecutar este script."
}
trap setup_incomplete ERR

SERVICE_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SERVICE_ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "SETUP INCOMPLETE: este script solo se puede ejecutar en macOS."
  exit 1
fi
MAC_ARCH="$(uname -m)"
echo "macOS detectado: $MAC_ARCH"

if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew no está instalado. Se ejecutará el instalador oficial."
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  if [[ -x /opt/homebrew/bin/brew ]]; then
    eval "$(/opt/homebrew/bin/brew shellenv)"
  elif [[ -x /usr/local/bin/brew ]]; then
    eval "$(/usr/local/bin/brew shellenv)"
  fi
fi
if ! command -v brew >/dev/null 2>&1; then
  echo "SETUP INCOMPLETE: Homebrew no está disponible después del instalador."
  exit 1
fi

echo "Homebrew: $(brew --version | head -n 1)"
brew doctor || true

if ! brew list --versions python@3.12 >/dev/null 2>&1; then
  brew install python@3.12
fi
PYTHON_BIN="$(brew --prefix python@3.12)/bin/python3.12"

JDK_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
if [[ -z "$JDK_HOME" && -x "$(brew --prefix openjdk@21 2>/dev/null)/libexec/openjdk.jdk/Contents/Home/bin/java" ]]; then
  JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
fi
if [[ -z "$JDK_HOME" ]]; then
  brew install openjdk@21
  JDK_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
fi
export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$(brew --prefix)/bin:$PATH"

if ! brew list --versions maven >/dev/null 2>&1; then
  brew install maven
fi
if ! brew list --versions unixodbc >/dev/null 2>&1; then
  brew install unixodbc
fi

if ! odbcinst -q -d 2>/dev/null | grep -Fq "ODBC Driver 18 for SQL Server"; then
  brew tap microsoft/mssql-release https://github.com/Microsoft/homebrew-mssql-release
  brew trust --formula microsoft/mssql-release/msodbcsql18 2>/dev/null || true
  HOMEBREW_ACCEPT_EULA=Y brew install msodbcsql18
fi
OPENSSL_PREFIX="$(brew --prefix openssl@3 2>/dev/null || true)"
if [[ -n "$OPENSSL_PREFIX" ]]; then
  export DYLD_LIBRARY_PATH="$OPENSSL_PREFIX/lib${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"
fi

echo "Python: $($PYTHON_BIN --version)"
echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Maven: $(mvn --version | head -n 1)"
odbcinst -q -d

VENV_PYTHON="$SERVICE_ROOT/.venv/bin/python"
if [[ -x "$VENV_PYTHON" ]]; then
  VENV_VERSION="$($VENV_PYTHON -c 'import platform, sys; print(f"{sys.version_info[0]}.{sys.version_info[1]} {platform.machine()}")')"
  if [[ "$VENV_VERSION" != "3.12 $MAC_ARCH" ]]; then
    INCOMPATIBLE_VENV_BACKUP="$(mktemp -d -t universidad-asistencia-venv)"
    mv "$SERVICE_ROOT/.venv" "$INCOMPATIBLE_VENV_BACKUP/.venv"
    echo "Se apartó el .venv incompatible en $INCOMPATIBLE_VENV_BACKUP."
  fi
fi

"$PYTHON_BIN" -m venv .venv
VENV_PYTHON="$SERVICE_ROOT/.venv/bin/python"
"$VENV_PYTHON" -m pip install --upgrade pip
"$VENV_PYTHON" -m pip install --upgrade setuptools wheel
"$VENV_PYTHON" -m pip install -r requirements.txt
# InsightFace pulls albumentations, whose current release pulls the headless
# OpenCV wheel. HighGUI is mandatory for this service; remove the headless
# distribution and restore the GUI wheel after dependency resolution.
if "$VENV_PYTHON" -m pip show opencv-python-headless >/dev/null 2>&1; then
  "$VENV_PYTHON" -m pip uninstall -y opencv-python-headless
  "$VENV_PYTHON" -m pip install --force-reinstall --no-deps opencv-python==4.10.0.84
fi
"$VENV_PYTHON" -m scripts.configure_from_java --from-docker --generate-key
echo "Aplicando migración facial idempotente:"
"$VENV_PYTHON" -m scripts.migrate
echo "Preparando modelo buffalo_l y ejecutando diagnóstico:"
if ! "$VENV_PYTHON" -m scripts.doctor --prepare-model; then
  echo "SETUP INCOMPLETE: revise los elementos FAIL/WAIT del doctor."
  exit 1
fi
trap - ERR
echo "SETUP COMPLETED"
echo "Siguiente paso: ./start-facial.sh y, en otra terminal, ./demo-check.sh"
