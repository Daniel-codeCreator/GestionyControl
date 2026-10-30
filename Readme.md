# SIGELAB - Sistema de Gestión y Control de Laboratorios

Proyecto final de **Programación II** desarrollado como una aplicación de escritorio para gestionar laboratorios, equipos, préstamos, devoluciones, mantenimiento, auditoría y autenticación con reconocimiento facial.

Repositorio: https://github.com/Daniel-codeCreator/GestionyControl

> Este README documenta únicamente la aplicación de escritorio SIGELAB, la base de datos SQL Server, el módulo QR y el servicio local de reconocimiento facial.

---

## Integrantes

- **Daniel Boc**
- **Integrante 2:** _Agregar nombre completo_
- **Integrante 3:** _Agregar nombre completo_

> Antes de la entrega final, complete esta sección con los nombres de todos los integrantes del grupo.

---

## Objetivo del proyecto

SIGELAB permite administrar el ciclo de vida de los equipos utilizados en laboratorios. El sistema controla su disponibilidad, préstamos, devoluciones parciales, mantenimiento, códigos QR, auditoría y autenticación de usuarios.

El inicio de sesión utiliza dos factores:

1. **Usuario + contraseña**, validada con BCrypt.
2. **Reconocimiento facial**, mediante un servicio local desarrollado en Python/FastAPI.

---

## Funciones principales

- Inicio de sesión con usuario, contraseña y reconocimiento facial.
- Registro facial asociado al `IdUsuario`.
- Gestión de usuarios y roles.
- Gestión de laboratorios con código único generado por el sistema.
- Gestión de tipos de equipo.
- Registro y consulta de equipos.
- Estados de equipo:
  - `DISPONIBLE`
  - `PRESTADO`
  - `DANADO`
  - `MANTENIMIENTO`
  - `BAJA`
- Generación y lectura de códigos QR mediante ZXing.
- Préstamos con uno o varios equipos mediante `DetallePrestamo`.
- Bloqueo de préstamo cuando el equipo no está disponible.
- Devoluciones en condición `BUENO` o `DANADO`.
- Devoluciones parciales.
- Mantenimiento preventivo y correctivo.
- Auditoría de operaciones importantes.
- Dashboard con métricas del sistema.
- Consultas y reportes.

---

## Tecnologías utilizadas

### Aplicación de escritorio

- **Java 21**
- **Maven**
- **Java Swing**
- **JDBC**
- **Microsoft SQL Server**
- **BCrypt** para contraseñas
- **ZXing** para códigos QR
- **JCalendar** para manejo de fechas en la interfaz

### Reconocimiento facial

- **Python 3.12** recomendado en Windows
- Python 3.11/3.12 soportado por el servicio facial
- **FastAPI**
- **OpenCV**
- **InsightFace / ArcFace**
- **ONNX Runtime CPU**
- **PyODBC**
- **Cryptography**

### Base de datos

- **SQL Server 2022**
- Microsoft ODBC Driver 18 for SQL Server, requerido por el servicio facial

---

## Dependencias Maven

El archivo `pom.xml` incluye las siguientes dependencias principales:

| Dependencia | Uso |
|---|---|
| `mssql-jdbc` | Conexión JDBC con SQL Server |
| `jbcrypt` | Hash y validación de contraseñas |
| `jcalendar` | Componentes de fecha |
| `zxing-core` | Generación y lectura QR |
| `zxing-javase` | Integración QR con Java SE |

Maven descarga estas dependencias automáticamente al cargar el proyecto.

---

## Arquitectura general

La aplicación está organizada por responsabilidades:

```text
Usuario
  |
  v
Java Swing - View
  |
  v
Controller / Service
  |
  v
Repository
  |
  v
JDBC
  |
  v
SQL Server
```

Para reconocimiento facial:

```text
SIGELAB Java
   |
   v
FacialAuthClient
   |
   | HTTP local
   v
facial-service - FastAPI
   |
   +--> OpenCV / cámara
   +--> InsightFace / ArcFace
   |
   v
SQL Server - PerfilFacial
```

El servicio facial escucha de forma predeterminada en:

```text
http://127.0.0.1:8765
```

---

## Estructura principal del proyecto

```text
GestionyControl/
|
|-- pom.xml
|-- docker-compose.yaml
|-- ImagenesQR/
|-- facial-service/
|   |-- app/
|   |-- scripts/
|   |-- sql/
|   |-- tests/
|   |-- requirements.txt
|   |-- setup-facial.ps1
|   |-- start-facial.ps1
|   `-- .env.example
|
`-- src/main/
    |-- java/Proyecto/universidad/
    |   |-- Config/
    |   |-- controller/
    |   |-- enums/
    |   |-- integration/facial/
    |   |-- model/
    |   |-- repository/
    |   |-- service/
    |   `-- view/
    |
    `-- resources/
        `-- conexion.xml
```

---

# Requisitos previos

Antes de ejecutar SIGELAB instale:

1. **JDK 21**.
2. **Maven**.
3. **IntelliJ IDEA** o un IDE compatible con Maven y Java Swing.
4. **SQL Server 2022** o acceso a un SQL Server compatible.
5. **Python 3.12** para el servicio facial en Windows.
6. **Microsoft ODBC Driver 18 for SQL Server**.
7. Una cámara o webcam disponible para reconocimiento facial.

Opcionalmente puede utilizar Docker Desktop para levantar SQL Server localmente.

---

# 1. Clonar el proyecto

```bash
git clone https://github.com/Daniel-codeCreator/GestionyControl.git
cd GestionyControl
```

En IntelliJ IDEA:

1. Seleccione **File > Open**.
2. Abra la carpeta del proyecto.
3. IntelliJ detectará `pom.xml`.
4. Espere a que Maven descargue las dependencias.
5. Compruebe que el SDK del proyecto sea **Java 21**.

---

# 2. Crear la base de datos

El script principal se encuentra en:

```text
src/main/java/Proyecto/universidad/Config/tablas.sql
```

Este script:

- crea la base `PROYCPROGRA2`;
- crea las tablas principales;
- crea claves primarias y foráneas;
- crea restricciones `UNIQUE` y `CHECK`;
- crea índices;
- inserta los roles iniciales;
- inserta los tipos de equipo iniciales.

## Tablas principales

- `Rol`
- `Usuario`
- `Laboratorio`
- `TipoEquipo`
- `Equipo`
- `Prestamo`
- `DetallePrestamo`
- `Devolucion`
- `Mantenimiento`
- `Auditoria`

### Procedimiento almacenado

Después de crear las tablas, ejecute:

```text
src/main/java/Proyecto/universidad/Config/SP.sql
```

El proyecto incluye el procedimiento almacenado `CrearUsuario`.

## Orden recomendado

En SQL Server Management Studio:

1. Conéctese al servidor SQL Server.
2. Abra `tablas.sql`.
3. Ejecute el script completo.
4. Compruebe que exista `PROYCPROGRA2`.
5. Abra `SP.sql`.
6. Ejecute el script.
7. Compruebe que las tablas y el procedimiento almacenado fueron creados correctamente.

---

# 3. SQL Server local con Docker - opcional

El repositorio incluye `docker-compose.yaml` para levantar SQL Server 2022.

Defina la contraseña antes de iniciar el contenedor.

### PowerShell

```powershell
$env:MSSQL_SA_PASSWORD="SU_PASSWORD_SEGURO"
docker compose up -d
```

El puerto predeterminado es:

```text
1433
```

Después ejecute `tablas.sql` y `SP.sql` sobre ese servidor.

Para detener el contenedor:

```powershell
docker compose down
```

Los datos permanecen en el volumen de Docker mientras no se elimine el volumen.

---

# 4. Configurar la conexión a SQL Server

La aplicación Java carga la conexión desde:

```text
src/main/resources/conexion.xml
```

El formato es:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<conexion>
    <db>
        <host>localhost</host>
        <port>1433</port>
        <database>PROYCPROGRA2</database>
        <user>USUARIO_SQL</user>
        <password>PASSWORD_SQL</password>
    </db>
</conexion>
```

### Ejemplo para una base remota

```xml
<conexion>
    <db>
        <host>SERVIDOR_REMOTO</host>
        <port>PUERTO_SQL</port>
        <database>PROYCPROGRA2</database>
        <user>USUARIO_SQL</user>
        <password>PASSWORD_SQL</password>
    </db>
</conexion>
```

El archivo `connection.java` lee este XML y construye la conexión JDBC.

> **Seguridad:** no publique contraseñas reales en GitHub. Utilice credenciales de desarrollo o un mecanismo seguro para secretos antes de publicar el repositorio.

---

# 5. Preparar el componente de reconocimiento facial

El servicio facial está en:

```text
facial-service/
```

La aplicación Java se comunica con este servicio mediante `FacialAuthClient`.

## Primera instalación en Windows

Abra PowerShell en la raíz del proyecto:

```powershell
cd facial-service
.\setup-facial.ps1
```

El script:

- verifica Python 3.12;
- crea `.venv`;
- instala `requirements.txt`;
- prepara OpenCV;
- crea `.env` desde `.env.example` si no existe;
- ejecuta un diagnóstico inicial.

---

## Configurar `.env`

Si es necesario, copie la plantilla:

```powershell
Copy-Item .env.example .env
```

Configure al menos:

```env
DB_HOST=localhost
DB_PORT=1433
DB_NAME=PROYCPROGRA2
DB_USER=USUARIO_SQL
DB_PASSWORD=PASSWORD_SQL
DB_DRIVER=ODBC Driver 18 for SQL Server
DB_ENCRYPT=yes
DB_TRUST_SERVER_CERTIFICATE=yes

FACIAL_HOST=127.0.0.1
FACIAL_PORT=8765

CAMERA_INDEX=
CAMERA_WIDTH=1280
CAMERA_HEIGHT=720
SHOW_PREVIEW=true

FACIAL_ENCRYPTION_KEY=CLAVE_GENERADA
```

Para una base remota use el mismo host, puerto, base y credenciales configurados para la aplicación Java.

### Generar la clave de cifrado facial

Desde `facial-service`:

```powershell
.\.venv\Scripts\python.exe -m scripts.generate_key
```

Copie el resultado a:

```env
FACIAL_ENCRYPTION_KEY=...
```

**No cambie esta clave después de registrar rostros**, porque los embeddings existentes fueron cifrados con ella.

---

# 6. Crear las tablas del componente facial

La base principal debe existir antes de ejecutar la migración facial.

Desde `facial-service`:

```powershell
.\.venv\Scripts\python.exe -m scripts.migrate
```

La migración crea las estructuras necesarias para el reconocimiento facial, incluyendo los perfiles faciales y su auditoría.

El servicio almacena embeddings faciales cifrados; no necesita guardar fotografías del usuario.

---

# 7. Verificar el servicio facial

Ejecute:

```powershell
.\.venv\Scripts\python.exe -m scripts.doctor
```

El diagnóstico revisa, entre otros elementos:

- versión de Python;
- entorno virtual;
- dependencias;
- OpenCV;
- InsightFace;
- ONNX Runtime;
- ODBC Driver 18;
- conexión a SQL Server;
- cámara;
- modelo facial;
- clave de cifrado;
- puerto del servicio.

Para iniciar el servicio manualmente:

```powershell
.\start-facial.ps1
```

La aplicación Java también intenta iniciar automáticamente el servicio facial cuando encuentra el entorno virtual correspondiente.

---

# 8. Ejecutar la aplicación

## Desde IntelliJ IDEA

1. Abra el proyecto.
2. Verifique que Maven haya terminado de cargar las dependencias.
3. Verifique la configuración de `conexion.xml`.
4. Verifique que la base `PROYCPROGRA2` esté disponible.
5. Verifique que el componente facial haya sido configurado.
6. Abra:

```text
src/main/java/Proyecto/universidad/main.java
```

7. Ejecute la clase `main` mediante **Run**.

La ventana de inicio de sesión de SIGELAB deberá mostrarse.

## Verificación Maven

Desde la raíz puede comprobar la compilación con:

```bash
mvn clean compile
```

---

# 9. Usuario de prueba

Al iniciar la aplicación, `main.java` comprueba si existe el usuario administrador. Si no existe, crea automáticamente:

```text
Usuario: admin
Contraseña: admin
Rol: ADMIN
Estado: ACTIVO
```

El registro utiliza BCrypt para guardar la contraseña en forma de hash.

> El usuario necesita un perfil facial registrado para completar el inicio de sesión, ya que las credenciales por sí solas no son suficientes.

---

# 10. Registrar el rostro del usuario

En la pantalla de Login:

1. Ingrese el usuario y la contraseña.
2. Presione **Registrar Rostro**.
3. El sistema valida primero las credenciales.
4. `FacialAuthClient` consulta el estado facial del `IdUsuario`.
5. Si todavía no existe un perfil, se inicia la captura mediante la cámara.
6. El servicio obtiene las muestras necesarias y registra el embedding cifrado asociado al usuario.

Después del registro ya puede utilizar **Ingresar**.

El flujo de autenticación es:

```text
Usuario + contraseña
        |
        v
AuthService / BCrypt
        |
        v
Usuario ACTIVO
        |
        v
FacialAuthClient
        |
        v
POST /face/verify
        |
        v
Reconocimiento facial
        |
        +-- Correcto --> LOGIN_EXITOSO --> Menú
        |
        `-- Incorrecto -> LOGIN_FALLIDO --> Acceso denegado
```

---

# 11. API del servicio facial

Endpoints principales:

```text
GET    /health
GET    /face/status/{userId}
POST   /face/enroll
POST   /face/verify
DELETE /face/profile/{userId}
```

El servicio utiliza de forma predeterminada:

```text
127.0.0.1:8765
```

---

# 12. Cámara para reconocimiento facial

La cámara puede ser una webcam integrada, webcam USB o una cámara virtual reconocida por el sistema operativo.

Variables útiles:

```env
CAMERA_INDEX=
CAMERA_NAME_HINT=
CAMERA_WIDTH=1280
CAMERA_HEIGHT=720
SHOW_PREVIEW=true
```

Si `CAMERA_INDEX` queda vacío, el servicio intenta seleccionar una cámara disponible.

Si necesita forzar otra cámara:

```env
CAMERA_INDEX=1
```

Esto puede ser útil cuando un teléfono es utilizado como webcam virtual.

---

# 13. Código QR

Cada equipo puede generar un QR que representa su código único de inventario.

El módulo QR utiliza ZXing.

Flujo esperado:

```text
Leer QR
   |
   v
Obtener CodigoInventario
   |
   v
Buscar equipo en SQL Server
   |
   v
Mostrar equipo y estado
```

La lectura QR identifica el equipo; por sí sola no modifica su estado.

Los archivos generados se almacenan en:

```text
ImagenesQR/
```

---

# 14. Reglas principales del sistema

El proyecto aplica reglas de negocio como:

- No se permite iniciar sesión con un usuario inactivo.
- El reconocimiento facial debe aprobarse para completar el login.
- El código de laboratorio debe ser único.
- El código de inventario debe ser único.
- Un préstamo debe incluir al menos un equipo.
- Un mismo equipo no puede repetirse dentro del mismo préstamo.
- Solo equipos `DISPONIBLE` pueden prestarse.
- Al confirmar un préstamo, el equipo cambia a `PRESTADO`.
- Un equipo solo puede devolverse si está pendiente en un préstamo activo.
- Un equipo no puede devolverse dos veces.
- Devolución `BUENO` cambia el equipo a `DISPONIBLE`.
- Devolución `DANADO` cambia el equipo a `DANADO`.
- Las devoluciones parciales mantienen el préstamo activo mientras existan equipos pendientes.
- Mantenimiento bloquea el préstamo del equipo.
- Al finalizar mantenimiento el equipo queda `DISPONIBLE` o `BAJA` según el resultado.
- Las acciones relevantes quedan registradas en auditoría.

---

# 15. Flujo general de estados del equipo

```text
               PRESTAMO
DISPONIBLE -----------------> PRESTADO
    ^                           |
    |                           |
    | devolución BUENO          | devolución DANADO
    |                           |
    +---------------------------+-----> DANADO
                                         |
                                         | mantenimiento
                                         v
                                  MANTENIMIENTO
                                    |       |
                              reparado      no reparable
                                    |       |
                                    v       v
                              DISPONIBLE   BAJA
```

También puede enviarse un equipo `DISPONIBLE` a mantenimiento preventivo.

---

# 16. Dashboard

El menú principal obtiene desde SQL Server métricas como:

- total de equipos;
- equipos disponibles;
- equipos prestados;
- equipos dañados;
- equipos en mantenimiento;
- equipos dados de baja;
- préstamos activos;
- préstamos vencidos.

Un préstamo se considera vencido cuando su fecha esperada ya pasó y todavía posee equipos pendientes.

---

# 17. Roles iniciales

`tablas.sql` crea los siguientes roles:

| Rol | Descripción |
|---|---|
| `ADMIN` | Administrador con acceso completo al sistema |
| `ENCARGADO` | Gestión operativa de equipos, préstamos, devoluciones y mantenimiento |
| `DOCENTE` | Usuario para consultas o solicitudes |

---

# 18. Datos iniciales

`tablas.sql` registra inicialmente tipos de equipo como:

- Laptop
- Cámara
- Proyector
- Router
- Switch
- Tablet
- Monitor
- Kit Electrónico

---

# 19. Auditoría

El sistema maneja eventos importantes como:

```text
LOGIN_EXITOSO
LOGIN_FALLIDO
PRESTAMO_CREADO
EQUIPO_DEVUELTO
EQUIPO_ENVIADO_MANTENIMIENTO
MANTENIMIENTO_FINALIZADO
```

La tabla `Auditoria` almacena usuario, fecha/hora, acción, entidad, ID del registro y descripción.

---

# 20. Solución de problemas

## No se puede conectar a SQL Server

Compruebe:

- `host` en `conexion.xml`;
- puerto de SQL Server;
- nombre de base `PROYCPROGRA2`;
- usuario y contraseña;
- firewall si utiliza un servidor remoto;
- que SQL Server esté ejecutándose.

## El servicio facial no inicia

Ejecute:

```powershell
cd facial-service
.\.venv\Scripts\python.exe -m scripts.doctor
```

Compruebe también:

```text
FACIAL_HOST=127.0.0.1
FACIAL_PORT=8765
```

## No detecta la cámara

- Cierre aplicaciones que puedan estar utilizando la webcam.
- Revise permisos de cámara del sistema operativo.
- Pruebe otro `CAMERA_INDEX`.
- Compruebe resolución e iluminación.
- Ejecute el diagnóstico de cámara del servicio facial.

## El rostro registrado ya no puede verificarse

Compruebe que no haya cambiado:

```env
FACIAL_ENCRYPTION_KEY
```

Los embeddings almacenados necesitan la misma clave utilizada durante el registro.

## El QR no se puede leer

- Compruebe que el archivo QR sea válido.
- Compruebe que el código exista en `Equipo.CodigoInventario`.
- Revise que la imagen tenga suficiente resolución.

---

# 21. Pruebas recomendadas para la defensa

Antes de presentar el proyecto se recomienda verificar:

1. Usuario válido + contraseña válida + rostro correcto.
2. Contraseña válida + rostro incorrecto.
3. Intento de registrar un código de equipo duplicado.
4. Préstamo de un equipo disponible.
5. Intento de prestar nuevamente un equipo prestado.
6. Intento de prestar un equipo dañado.
7. Intento de prestar un equipo en mantenimiento.
8. Intento de prestar un equipo de baja.
9. Lectura QR de equipo disponible.
10. Lectura QR de equipo no disponible.
11. Devolución en condición `BUENO`.
12. Devolución en condición `DANADO`.
13. Intento de devolver dos veces el mismo equipo.
14. Devolución parcial de un préstamo con varios equipos.
15. Enviar un equipo dañado a mantenimiento.
16. Finalizar mantenimiento como reparado.
17. Finalizar mantenimiento como no reparable.
18. Verificar un préstamo vencido en consultas/reportes.

---

# 22. Seguridad

- Las contraseñas de usuarios se almacenan utilizando BCrypt.
- El servicio facial no necesita almacenar fotografías de los usuarios.
- Los embeddings faciales se almacenan cifrados.
- No publique `FACIAL_ENCRYPTION_KEY`.
- No publique credenciales reales de SQL Server.
- No publique archivos `.env` con secretos.
- Si una contraseña ya fue compartida públicamente, cámbiela antes de entregar o desplegar el proyecto.

Se recomienda agregar al `.gitignore`:

```gitignore
facial-service/.env
src/main/resources/conexion.xml
```

Si se ignora `conexion.xml`, puede conservarse en el repositorio una plantilla como `conexion.example.xml` sin secretos.

---

# 23. Resumen de ejecución rápida - Windows

```text
1. Instalar JDK 21, Maven, SQL Server 2022, Python 3.12 y ODBC Driver 18.
2. Clonar el repositorio.
3. Ejecutar tablas.sql.
4. Ejecutar SP.sql.
5. Configurar src/main/resources/conexion.xml.
6. Entrar a facial-service.
7. Ejecutar .\setup-facial.ps1.
8. Configurar .env y FACIAL_ENCRYPTION_KEY.
9. Ejecutar python -m scripts.migrate desde el entorno virtual.
10. Ejecutar python -m scripts.doctor.
11. Abrir el proyecto en IntelliJ con Java 21.
12. Ejecutar Proyecto.universidad.main.
13. Usar admin / admin si el usuario todavía no existe.
14. Registrar el rostro del usuario.
15. Iniciar sesión y probar los módulos de SIGELAB.
```

---

## Licencia / uso académico

Proyecto desarrollado con fines académicos para el curso de **Programación II** de la **Universidad Mariano Gálvez de Guatemala**.
