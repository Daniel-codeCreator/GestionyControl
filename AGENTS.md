# AGENTS.md

## What this is

- Java 21 Swing desktop app (Maven) for university lab/equipment management, plus a local Python FastAPI facial-recognition sidecar. UI text and most identifiers are Spanish.
- Branch `Rama/Cristian` tracks `main`; keep the diff small and aligned with `main` unless the user asks for a new module.
- No root `README.md`; `facial-service/README.md` is the only detailed doc. No CI, lint, formatter, or Java test suite exists.

## Layout

- `src/main/java/Proyecto/universidad/` — `main.java` is the entrypoint; `view/` holds Swing screens, `service/` + `repository/` raw JDBC, `integration/facial/` the HTTP client. `controller/` is mostly empty.
- `src/main/java/Proyecto/universidad/Config/tablas.sql` — complete schema + seed for `PROYCPROGRA2`. `Config/SP.sql` holds `dbo.CrearUsuario`.
- `facial-service/` — FastAPI (`app/`), operational scripts (`scripts/`), unit tests, `sql/001_create_facial_tables.sql` (idempotent, requires `Usuario`).
- `docker-compose.yaml` — SQL Server only; it now expects `MSSQL_SA_PASSWORD` in the environment and publishes `${MSSQL_PORT:-1433}`.

## Commands

Java:

- Build/run through IntelliJ (JDK 21). No Maven wrapper and `mvn` may not be on `PATH`.
- Main class: `Proyecto.universidad.main` (creates the default admin). `Login.main` also exists.

Facial service — always run from `facial-service/`:

```powershell
.\setup-facial.ps1              # Python 3.12 venv, deps, .env, doctor
.\start-facial.ps1              # uvicorn on 127.0.0.1:8765
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m scripts.doctor
.\.venv\Scripts\python.exe -m scripts.migrate
.\enroll-user.ps1 1             # enrols the face of user 1
```

## Setup and runtime gotchas

- Actual `facial-service/.env` uses `DB_NAME=PROYCPROGRA2`; `.env.example` still says `UniversidadAsistenciaDB` — stale. The schema in `tablas.sql` is authoritative.
- `facial-service/.env` is tracked by Git and holds the DB password and `FACIAL_ENCRYPTION_KEY`. Rotating that key makes every stored embedding unreadable.
- Login requires username + password **and** facial verification (RN-01). A missing face profile returns `404 PERFIL_NO_REGISTRADO`; the Python side answers `503 BD_NO_DISPONIBLE` (ODBC/SQL Server down) and `503 MODELO_NO_DISPONIBLE` (insightface/model missing).
- `VERIFY_TIMEOUT_SECONDS` (20 by default) starts counting while the webcam is still opening, so the first seconds of the preview window can be consumed by camera startup and the capture ends with `TIMEOUT_FRONT`. Raise it in `.env` when the camera is slow.
- `FacialAuthClient` auto-starts the sidecar from `facial-service/.venv/Scripts/python.exe` (Windows-only path) and waits up to 45 s for port 8765; on macOS/Linux start the service manually.
- Each enroll/verify request spawns `scripts.interactive_face` as a child process so OpenCV HighGUI runs on its MainThread. Keep that boundary.
- Swing screens depend on IntelliJ GUI Designer `.form` files: a plain `javac`/`mvn` build leaves the bound fields null and most windows render blank. Run it in IntelliJ and keep the Java field names in sync with the `.form` bindings.
- Known bug: `FacialRepository.get_user` returns the numeric `IdRol` as `rol`, while `FaceService.delete_profile` compares it to `"ADMIN"`, so profile deletion always returns 403.

## Conventions

- Repositories use raw JDBC, one connection per operation, try-with-resources and `SQLException` wrapped in `RuntimeException`; services validate the business rules and views orchestrate.
- Audit trail: `AuditoriaRepository.registrar(...)` (own connection) or `registrar(conn, ...)` to write inside an existing transaction. Registered actions: `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `PRESTAMO_CREADO`, `EQUIPO_DEVUELTO`, `EQUIPO_ENVIADO_MANTENIMIENTO`, `MANTENIMIENTO_FINALIZADO`, `USUARIO_DESACTIVADO`.
- `UsuarioRepository.crearUsuario` receives the BCrypt hash, never plain text; `UsuarioRepository.desactivarUsuario` does the logical deactivation and writes its audit row in the same transaction.
- Keep enum/state strings uppercase and synchronized with the `CHECK` constraints in the SQL scripts.
- Python tests fake the repository/engine and must not need a webcam or database.
