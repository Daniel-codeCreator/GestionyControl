# SIGELAB Android

Versión móvil de SIGELAB (Sistema de Gestión y Control): Kotlin, Jetpack Compose, MVVM + Clean Architecture, Hilt, Retrofit, Room, CameraX y WorkManager. Sigue el esquema y las reglas de la app de escritorio (repositorio GestionyControl) sin modificarla.

## Arquitectura

```
┌──────────── Android ─────────────┐        ┌──────── PC / servidor ───────┐
│ ui (Compose + ViewModel)         │        │                              │
│   └─ domain (casos de uso,       │  HTTP  │ API móvil (FastAPI, :8000)   │
│        reglas, modelos)          │──────▶ │  /api/...  negocio           │──▶ SQL Server
│          └─ data (Retrofit,      │  JWT   │  /face/... biometría ──▶ facial-service (:8765)
│               Room, WorkManager) │        │                              │
└──────────────────────────────────┘        └──────────────────────────────┘
```

**La app nunca se conecta directo a SQL Server.** Un driver JDBC en el APK obligaría a guardar el usuario y la contraseña de la base dentro de la app, y cualquiera puede extraerlos. Todo pasa por la API móvil, que aplica BCrypt, roles, transacciones y auditoría. [backend-referencia/mobile_gateway.py](backend-referencia/mobile_gateway.py) es la referencia de esa API: cada endpoint ejecuta las mismas consultas, transacciones y textos de auditoría que el repositorio equivalente de escritorio.

```
app/src/main/java/com/universidad/sigelab/
├── core/          di (Hilt), network (apiCall → AppError, AuthInterceptor), session
├── domain/        Kotlin puro: modelos del esquema real, reglas, casos de uso
├── data/          remote (SigelabApi, FacialApi, DTO), local (Room), repository, sync
└── ui/
    ├── login/         credenciales → rostro
    ├── dashboard/     métricas + menú por rol
    ├── catalogos/     Gestión de Laboratorios y de Tipos de Equipo
    ├── equipos/       lista, ficha con QR, alta/actualización, escáner QR
    ├── prestamos/     Préstamos y Devoluciones
    ├── mantenimiento/ en proceso, registrar ingreso, cierre
    ├── reportes/      equipos por estado, vencidos, historial de mantenimiento, auditoría
    ├── usuarios/      Crear Usuario + usuarios registrados
    ├── camera/        cámara frontal (rostro) y escáner QR (ML Kit)
    └── navigation/    la sesión decide Login ↔ Dashboard
```

## Paridad con la app de escritorio

La app se ajustó a `Config/tablas.sql`, `Config/SP.sql` y a los `service`/`repository` de escritorio (rama `main`):

| Módulo | Igual que escritorio | Diferencia intencional |
|---|---|---|
| Login | BCrypt `$2a$`; mensaje «Usuario o contraseña incorrectos, o usuario inactivo.»; auditoría `LOGIN_EXITOSO`/`LOGIN_FALLIDO` con los mismos textos de motivo | El rostro se captura con la cámara del celular (ver *Pendiente en el backend*) |
| Laboratorios | Código automático `LAB-00001`; Nombre, Ubicación, Capacidad (> 0) y Estado ACTIVO/INACTIVO; Guardar y Actualizar; no se eliminan | El código no se edita al actualizar |
| Tipos de Equipo | Nombre y Descripción; solo altas | Avisa antes de enviar si el nombre ya existe (la base tiene UNIQUE) |
| Equipos | Código automático `EQP00001`; Nombre, Tipo, Laboratorio, Marca, Modelo, Serie; nace `DISPONIBLE`; QR = código de inventario, archivo `QR_<código>` | El estado no se cambia a mano al actualizar: solo con préstamos, devoluciones y mantenimiento |
| Préstamos | «Estudiante / Usuario» libre, «Fecha Fin Estimada» desde hoy, observaciones obligatorias, varios equipos DISPONIBLES sin repetir, QR agrega a la selección; devolución por equipo con «Condición Entrega» BUENO/DANADO | La fecha vence a las 23:59 del día elegido, para cumplir `CK_Prestamo_Fechas` |
| Mantenimiento | Tipo PREVENTIVO (lista DISPONIBLES) o CORRECTIVO (lista DAÑADOS), Responsable y Descripción obligatorios; cierre REPARADO → DISPONIBLE, NO_REPARABLE → BAJA con observaciones opcionales | Segunda confirmación antes de dar de baja |
| Reportes | Equipos por estado (TODOS o uno), Préstamos vencidos, Historial de mantenimiento por código | CSV para compartir; pestaña Auditoría solo para ADMIN |
| Crear Usuario | Procedimiento `CrearUsuario`, campos obligatorios con los mismos mensajes, tabla de usuarios registrados, nace ACTIVO | Contraseña de 8+ caracteres con letras y números; el ADMIN registra el rostro al crear el usuario |

Acciones de auditoría que genera la API móvil, iguales a escritorio: `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `PRESTAMO_CREADO`, `EQUIPO_DEVUELTO`, `EQUIPO_ENVIADO_MANTENIMIENTO`, `MANTENIMIENTO_FINALIZADO`.

## Login de dos factores

1. `POST /api/auth/login` con usuario y contraseña. El servidor compara con BCrypt, exige `Estado = ACTIVO` (sin distinguir mayúsculas: el procedimiento `CrearUsuario` guarda `Activo`) y responde un **token de pre-autenticación** de 3 minutos que solo sirve para el paso 2.
2. La cámara frontal toma la foto y la app llama a `POST /face/verify` (multipart: `id_usuario` + `imagen`) con ese token. Si el rostro coincide, el servidor emite el token de sesión.

| Dónde falla | Quién registra `LOGIN_FALLIDO` |
|---|---|
| Credenciales, usuario inactivo, rostro no coincide | La API, en la misma petición |
| Sin red, error 5xx, permiso de cámara negado, cancelación | La app: guarda el evento en Room y WorkManager lo envía a `POST /api/auditoria/eventos` cuando hay red |

## Contrato de API

Base: `SIGELAB_API_URL` (`…/api/`) y `FACIAL_API_URL` (la misma API móvil). JSON en snake_case; errores `{"detail": "..."}` con los mensajes de escritorio.

| Método y ruta | Rol | Equivale en escritorio a |
|---|---|---|
| `POST auth/login` | — | `AuthService.autenticar` |
| `POST face/verify`, `POST face/register` | pre-auth / ADMIN | `FacialAuthClient` (ver pendiente) |
| `POST auditoria/eventos` | — | solo `LOGIN_FALLIDO` del dispositivo |
| `GET dashboard/metricas` | todos | `DashboardRepository.obtenerResumen` |
| `GET/POST laboratorios`, `PUT laboratorios/{id}` | ADMIN, ENCARGADO | `LaboratorioRepository` (código `LAB-00001` en el servidor) |
| `GET/POST tipos-equipo` | ADMIN, ENCARGADO | `TipoEquipoRepository` |
| `GET/POST equipos`, `PUT equipos/{id}` | todos (GET) / ADMIN, ENCARGADO | `EquipoRepository` (código `EQP00001` en el servidor) |
| `GET equipos/codigo/{codigo}` | todos | `QrService.identificarEquipo`; **no cambia el estado** |
| `GET prestamos`, `POST prestamos`, `POST devoluciones` | ADMIN, ENCARGADO | `PrestamoRepository` (transacción + auditoría) |
| `GET/POST mantenimientos`, `PUT mantenimientos/{id}/finalizar` | ADMIN, ENCARGADO | `MantenimientoRepository` (transacción + auditoría) |
| `GET reportes/mantenimientos?codigo=` | ADMIN, ENCARGADO | «Consultar Historial» de `ReportesView` |
| `GET auditoria?desde=&hasta=&accion=&limite=` | ADMIN | agregado de la app |
| `GET/POST usuarios` | ADMIN | `UsuarioRepository.listarTodos` / procedimiento `CrearUsuario` |

Las reglas se validan dos veces: en la app (`domain/usecase`, para responder al instante) y en la API dentro de la transacción. La API es la que decide.

## Roles

| Módulo | ADMIN | ENCARGADO | DOCENTE |
|---|---|---|---|
| Laboratorios, Tipos, Préstamos, Mantenimiento, Reportes | total | total | — |
| Gestión de Equipos | total | total | solo consulta |
| Crear Usuario | total | — | — |

Está en `Rol.acceso()`. En escritorio solo Crear Usuario está restringido (ADMIN); la app además limita al DOCENTE según los requisitos del proyecto.

## Ejecutar

1. Abra la carpeta `sigelab-android` en Android Studio. El proyecto usa Gradle 8.13 y el plugin de Android 8.13.2, que necesitan un JDK 17 a 23: elija un JDK 21 en *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* (el Java 25 que trae Android Studio 2026 no sirve con Gradle 8.13). Desde la consola: `gradlew.bat assembleDebug` con `JAVA_HOME` apuntando a ese JDK.
2. Levante en el PC el servicio facial (puerto 8765) y la API móvil (puerto 8000). La app solo habla con la API móvil, también para `/face/*`; el servicio facial no tiene autenticación y debe quedarse escuchando solo en `127.0.0.1`.
3. Conecte el celular por USB con la depuración USB activada y ejecute `adb reverse tcp:8000 tcp:8000`; hay que repetirlo cada vez que se reconecta el cable. Así el `localhost` del teléfono llega al PC sin abrir el firewall. En el emulador funciona igual. Para usar otra dirección (por ejemplo la IP del PC en la Wi-Fi), defina `sigelab.api.url` y `sigelab.facial.url` en `local.properties` y agregue esa IP en `app/src/debug/res/xml/network_security_config.xml`.
4. En release se exige HTTPS: cambie las URL de `release` por las del servidor real.

Pruebas del dominio: clic derecho en `app/src/test` → *Run Tests*, o `gradlew.bat :app:testDebugUnitTest`.

## Modo demo (solo versiones debug)

Para recorrer y presentar la app sin backend, active **Modo demo** en el login. Un servidor simulado dentro de la app ([ServidorDemo.kt](app/src/debug/java/com/universidad/sigelab/core/demo/ServidorDemo.kt)) responde las mismas rutas y el mismo JSON que la API móvil, con las reglas de escritorio y datos de ejemplo: 3 laboratorios, los 8 tipos de `tablas.sql`, 12 equipos en todos los estados, un préstamo vencido, un equipo en mantenimiento y auditoría.

| Usuario | Rol | Contraseña |
|---|---|---|
| `admin.demo` | ADMIN | `Demo1234` |
| `encargado.demo` | ENCARGADO | `Demo1234` |
| `docente.demo` | DOCENTE | `Demo1234` |
| `inactivo.demo` | (inactivo, para mostrar el rechazo) | `Demo1234` |

- El paso del rostro acepta cualquier foto. Los cambios (préstamos, devoluciones, mantenimientos, altas) se ven en el dashboard y los reportes, y se pierden al cerrar la app.
- Al cambiar de modo se vacían las listas guardadas en el teléfono, para no mezclar datos de ejemplo con los reales. Los LOGIN_FALLIDO reales pendientes no se envían al demo: esperan al servidor real.
- El código del demo está en `app/src/debug`: la versión release no lo incluye y no muestra el interruptor.
- En el emulador, si la cámara se cuelga, inícielo con cámaras simuladas: `emulator -avd <nombre> -camera-back emulated -camera-front emulated`.

## Estado

- **Hecho:** todos los módulos del menú de escritorio (Laboratorios, Tipos de Equipo, Equipos con QR, Préstamos y Devoluciones, Mantenimiento, Crear Usuario, Reportes), login de dos pasos, dashboard, caché y bandeja de auditoría en Room, reglas de negocio con pruebas.
- **Pendiente en el backend (requiere permiso del equipo):** la API móvil todavía no existe en el repositorio; `backend-referencia/mobile_gateway.py` es su referencia y no se ha ejecutado. El facial-service actual abre la webcam del PC y no recibe fotos: hay que implementar `comparar_con_perfil` y `registrar_perfil` con las funciones de `facial-service/app` (`FaceEngine`, `FacialRepository`). Su prueba de vida usa varias poses, así que exigirla desde el celular requiere enviar varias fotos.
