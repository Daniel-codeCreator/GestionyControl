# AGENTS.md

## What this is

- Java 21 Swing desktop app (Maven) for university lab/equipment management, plus a local Python FastAPI facial-recognition sidecar. UI text and most identifiers are Spanish.
- There is no root `README.md`; `facial-service/README.md` is the only detailed doc (its `../README.md` link is dead). No CI, lint, formatter, or Java test suite exists.

## Layout

- `src/main/java/Proyecto/universidad/` — `main.java` is the entrypoint; `view/` holds Swing screens, `service/` + `repository/` raw JDBC, `integration/facial/` the HTTP client. `controller/` is mostly empty; flow logic lives in views.
- `src/main/java/Proyecto/universidad/Config/tablas.sql` — complete schema + seed data for `PROYCPROGRA2`. Run it before the app or facial migration.
- `facial-service/` — FastAPI (`app/`), operational scripts (`scripts/`), unit tests, and `sql/001_create_facial_tables.sql` (idempotent, requires `Usuario` to exist).
- `docker-compose.yaml` — SQL Server only; no database initialization.

## Commands

Java:

- Build/run through IntelliJ (JDK 21). There is no Maven wrapper and `mvn` may not be on `PATH`.
- `pom.xml` compiles with `--enable-preview`; if classes are built with Maven, run them with `--enable-preview` too.
- Main class: `Proyecto.universidad.main` (creates the default admin). `Login.main` also exists.

Facial service — always run from `facial-service/`:

```powershell
.\setup-facial.ps1              # Python 3.12 venv, deps, .env, doctor
.\start-facial.ps1              # uvicorn on 127.0.0.1:8765
.\.venv\Scripts\python.exe -m pytest -q
.\.venv\Scripts\python.exe -m pytest tests/test_service.py -q
.\.venv\Scripts\python.exe -m scripts.doctor
.\.venv\Scripts\python.exe -m scripts.migrate
```

macOS/Linux equivalents use `setup-facial-macos.sh` / `setup-facial-linux.sh`, `.venv/bin/python`, and `./start-facial.sh`.

## Setup and runtime gotchas

- `docker compose up -d` publishes SQL Server on host port **1400**, while the Java app and `.env` default to **1433**. Override `DB_URL`/`DB_PORT` or change the compose mapping. Compose also uses a different SA password than the Java default.
- Actual `facial-service/.env` uses `DB_NAME=PROYCPROGRA2`; `.env.example` and the README still say `UniversidadAsistenciaDB` — stale. The schema in `tablas.sql` is authoritative.
- `facial-service/.env` is tracked by Git and contains the DB password and `FACIAL_ENCRYPTION_KEY`. Changing that key makes every stored facial embedding unreadable; don't rotate it without re-enrolling users.
- Login requires username/password **plus** a successful facial verification. There is no bypass, so testing login requires a running facial service, a camera, and an enrolled profile. If `admin` is absent, startup creates `admin`/`admin`.
- A newly created account cannot log in until an admin enrolls its face via the facial service.
- `setup-facial.ps1` hard-requires Python 3.12 via `py -3.12`, although the service itself supports 3.11/3.12.
- `FacialAuthClient` auto-starts the sidecar using `facial-service/.venv/Scripts/python.exe`, a Windows-only path. On macOS/Linux start it manually. It waits up to 45 s for port 8765 to answer and skips spawning if something already listens there.
- The facial API answers `503 BD_NO_DISPONIBLE` when ODBC/SQL Server is unreachable (`app/main.py`), and `503 MODELO_NO_DISPONIBLE` when insightface/model is missing — both are surfaced by the Java client as login error messages.
- `scripts/configure_from_java.py` still targets the removed `universidad/asistencia/config/DatabaseConnection.java`, so `setup-facial-macos.sh` fails during `.env` configuration. Configure `.env` manually.
- Each facial enroll/verify request spawns `scripts.interactive_face` as a child process so OpenCV HighGUI runs on its MainThread. Keep that boundary.
- Swing screens depend on IntelliJ GUI Designer `.form` files. A plain `mvn`/`javac` build leaves the bound fields null, so most windows render blank; compile/run in IntelliJ (or commit generated UI code). Keep Java field names in sync with the `.form` bindings.
- Known bug: `FacialRepository.get_user` returns the numeric `IdRol` as `rol`, while `FaceService.delete_profile` compares it to `"ADMIN"`, so profile deletion always returns 403.

## Conventions

- Repositories follow raw JDBC with one connection per method, try-with-resources, and `SQLException` wrapped in `RuntimeException`; services validate and views orchestrate.
- Login audit does not INSERT directly from Java: `AuditoriaRepository.registrarLogin` calls `dbo.sp_RegistrarAuditoriaLogin`. User creation goes through `dbo.sp_CrearUsuario`, which also writes the `USUARIO_CREADO` audit row inside the same transaction. Both procedures live in `Config/tablas.sql`; existing databases need those batches re-run.
- `UsuarioRepository.crearUsuario` receives the BCrypt hash, never the plain password; hashing happens in `UsuarioService`.
- `view/CrearUsuario.java` + `CrearUsuario.form` (opened from the Login screen's "Crear nueva cuenta" button) follows the standard layer stack through `controller/CrearUsuarioController.java`, and it carries a code-built fallback panel for compiles without IntelliJ GUI instrumentation. `enums/RolUsuario` maps role names to the seed `IdRol` values.
- Keep enum/state strings uppercase and synchronized with the `CHECK` constraints in both SQL schemas.
- Python tests fake the repository/engine and must not need a webcam or database. Real enroll/verify needs a camera and SQL Server.
