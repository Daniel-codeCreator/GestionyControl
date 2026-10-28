@echo off
title SIGELAB - Sistema de Gestion y Control
cd /d "%~dp0"

if not exist "facial-service\.venv\Scripts\python.exe" (
  echo [1/2] Preparando el servicio facial ^(solo la primera vez^). Puede tardar varios minutos.
  powershell -ExecutionPolicy Bypass -File "facial-service\setup-facial.ps1"
)

echo [2/2] Iniciando SIGELAB...
start "SIGELAB - Servicio Facial" /min "%~dp0facial-service\iniciar-facial.bat"
timeout /t 8 /nobreak >nul
java -cp "SIGELAB.jar;lib/*" Proyecto.universidad.main

if errorlevel 1 (
  echo.
  echo La aplicacion termino con error. Revise que Java 21 este instalado y que conexion.xml sea correcto.
  pause
)
