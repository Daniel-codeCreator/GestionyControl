"""
Referencia de la API que consume la app Android de SIGELAB (gateway entre el celular y SQL Server).

Cada endpoint reproduce el repositorio equivalente de la app de escritorio (GestionyControl,
src/main/java/Proyecto/universidad/repository y service): mismas tablas y columnas de
Config/tablas.sql, mismas validaciones, mismas transacciones y los mismos textos de auditoría.
Así el celular y el escritorio dejan la base exactamente igual.

Se monta junto al facial-service o como servicio aparte, en el PC (puerto 8000):

    from mobile_gateway import router as movil
    app.include_router(movil)

Configuración por variables de entorno (nunca en el código ni en Git):
    SIGELAB_SQLSERVER   cadena ODBC, p. ej.
                        "DRIVER={ODBC Driver 18 for SQL Server};SERVER=host,puerto;DATABASE=PROYCPROGRA2;"
                        "UID=sigelab_api;PWD=...;Encrypt=yes;TrustServerCertificate=yes"
                        Use un usuario de SQL Server con permisos solo sobre PROYCPROGRA2, no "sa".
    SIGELAB_JWT_SECRET  clave aleatoria larga para firmar los tokens.

Dependencias: fastapi, python-multipart, pyodbc, bcrypt, PyJWT
"""
from __future__ import annotations

import datetime as dt
import os
from typing import Optional

import bcrypt
import jwt
import pyodbc
from fastapi import APIRouter, Depends, File, Form, Header, HTTPException, UploadFile
from pydantic import BaseModel

router = APIRouter()

JWT_SECRET = os.environ["SIGELAB_JWT_SECRET"]
CONEXION = os.environ["SIGELAB_SQLSERVER"]

PREAUTH = dt.timedelta(minutes=3)
SESION = dt.timedelta(hours=8)
ROLES_OPERATIVOS = ("ADMIN", "ENCARGADO")
TODOS_LOS_ROLES = ("ADMIN", "ENCARGADO", "DOCENTE")

# Mismo texto que Login.java: no se distingue contraseña incorrecta de usuario inactivo.
MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos, o usuario inactivo."


# --------------------------------------------------------------------------- utilidades

def db():
    conn = pyodbc.connect(CONEXION, autocommit=False)
    try:
        yield conn
    finally:
        conn.close()


def auditar(conn, id_usuario: Optional[int], accion: str, entidad: str, id_registro: Optional[int],
            descripcion: str, confirmar: bool = True) -> None:
    """AuditoriaRepository.registrar. confirmar=False cuando forma parte de una transacción de negocio (RN-18)."""
    conn.execute(
        """INSERT INTO Auditoria (IdUsuario, FechaHora, Accion, Entidad, IdRegistro, Descripcion)
           VALUES (?, SYSDATETIME(), ?, ?, ?, ?)""",
        id_usuario, accion, entidad, id_registro, descripcion[:500],
    )
    if confirmar:
        conn.commit()


def emitir_token(id_usuario: int, nombre_usuario: str, rol: str, tipo: str, duracion: dt.timedelta) -> str:
    ahora = dt.datetime.now(dt.timezone.utc)
    datos = {"sub": str(id_usuario), "usr": nombre_usuario, "rol": rol.upper(), "typ": tipo, "iat": ahora, "exp": ahora + duracion}
    return jwt.encode(datos, JWT_SECRET, algorithm="HS256")


def leer_token(authorization: Optional[str], tipo: str) -> dict:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "Falta el token")
    try:
        datos = jwt.decode(authorization[7:], JWT_SECRET, algorithms=["HS256"])
    except jwt.PyJWTError:
        raise HTTPException(401, "Token inválido o expirado")
    if datos.get("typ") != tipo:
        raise HTTPException(401, "Tipo de token incorrecto")
    return datos


def sesion(authorization: Optional[str] = Header(None)) -> dict:
    return leer_token(authorization, "access")


def requiere_rol(*roles: str):
    def dependencia(datos: dict = Depends(sesion)) -> dict:
        if datos["rol"] not in roles:
            raise HTTPException(403, "Acceso Denegado para tu rol.")
        return datos
    return dependencia


def obligatorio(valor: Optional[str], mensaje: str) -> str:
    if valor is None or not valor.strip():
        raise HTTPException(422, mensaje)
    return valor.strip()


def estado_equipo(conn, id_equipo: int) -> Optional[str]:
    """EquipoRepository.obtenerPorId: el estado real en la BD, no el de la pantalla."""
    fila = conn.execute("SELECT Estado FROM Equipo WHERE IdEquipo = ?", id_equipo).fetchone()
    return fila.Estado.strip().upper() if fila else None


# --------------------------------------------------------------- biometría (adaptar)

class RostroNoDetectado(Exception):
    pass


class PerfilNoRegistrado(Exception):
    pass


def comparar_con_perfil(conn, id_usuario: int, jpeg: bytes) -> tuple[bool, float]:
    """
    El facial-service actual captura con la webcam del PC; para el celular hay que comparar la
    imagen recibida reutilizando sus funciones (facial-service/app):
      1. frame = cv2.imdecode(np.frombuffer(jpeg, np.uint8), cv2.IMREAD_COLOR)
      2. caras = FaceEngine.detect(frame); sin cara → RostroNoDetectado
         evaluate_face(frame, cara, settings) para la calidad
      3. FacialRepository.get_profile(id_usuario) → embedding guardado (sin perfil → PerfilNoRegistrado)
      4. similitud de FaceEngine.embedding(cara) contra el perfil y comparar con FACE_MATCH_THRESHOLD
    La prueba de vida de escritorio usa varias poses (LivenessSession.observe): con una sola foto no
    se puede aplicar; para exigirla, la app debe enviar varias fotos (frente, izquierda, derecha).
    """
    raise NotImplementedError


def registrar_perfil(conn, id_usuario: int, jpeg: bytes) -> None:
    """Como /face/enroll de escritorio, pero con la imagen recibida: embedding → FacialRepository.save_profile."""
    raise NotImplementedError


# ------------------------------------------------------------------- autenticación

class LoginIn(BaseModel):
    usuario: str
    contrasena: str


@router.post("/api/auth/login")
def login(body: LoginIn, conn=Depends(db)):
    """Paso 1 = AuthService.autenticar. Responde un token de pre-autenticación que solo sirve para /face/verify."""
    fila = conn.execute(
        """SELECT u.IdUsuario, u.Nombre, u.Apellido, u.NombreUsuario, u.Contrasena, u.Estado, r.Nombre AS Rol
           FROM Usuario u JOIN Rol r ON r.IdRol = u.IdRol
           WHERE LOWER(RTRIM(LTRIM(u.NombreUsuario))) = LOWER(RTRIM(LTRIM(?)))""",
        body.usuario,
    ).fetchone()

    # El procedimiento CrearUsuario guarda Estado = 'Activo': se compara sin distinguir mayúsculas.
    valido = (
        fila is not None
        and (fila.Estado or "").strip().upper() == "ACTIVO"
        # Hash de jBCrypt ($2a$, costo 12): bcrypt de Python lo verifica sin cambios.
        and bcrypt.checkpw(body.contrasena.encode(), fila.Contrasena.strip().encode())
    )
    if not valido:
        auditar(conn, None, "LOGIN_FALLIDO", "Usuario", None,
                f"Intento de login fallido para '{body.usuario[:50]}'. Motivo: Usuario/contraseña incorrectos o usuario inactivo")
        raise HTTPException(401, MENSAJE_CREDENCIALES)

    return {
        "id_usuario": fila.IdUsuario,
        "nombre_usuario": fila.NombreUsuario,
        "nombre_completo": f"{fila.Nombre} {fila.Apellido}".strip(),
        "rol": fila.Rol,
        "pre_auth_token": emitir_token(fila.IdUsuario, fila.NombreUsuario, fila.Rol, "preauth", PREAUTH),
    }


@router.post("/face/verify")
def face_verify(
    id_usuario: int = Form(...),
    imagen: UploadFile = File(...),
    authorization: Optional[str] = Header(None),
    conn=Depends(db),
):
    """Paso 2. Si coincide, emite el token de sesión. Auditoría con los textos de AuthService."""
    datos = leer_token(authorization, "preauth")
    if int(datos["sub"]) != id_usuario:
        raise HTTPException(401, "El token no corresponde a este usuario")
    usuario = datos["usr"]

    try:
        coincide, score = comparar_con_perfil(conn, id_usuario, imagen.file.read())
    except RostroNoDetectado:
        auditar(conn, id_usuario, "LOGIN_FALLIDO", "Usuario", id_usuario,
                f"Intento de login fallido para '{usuario}'. Motivo: Fallo biométrico: No se detectó un rostro")
        raise HTTPException(422, "No se detectó un rostro en la imagen")
    except PerfilNoRegistrado:
        auditar(conn, id_usuario, "LOGIN_FALLIDO", "Usuario", id_usuario,
                f"Intento de login fallido para '{usuario}'. Motivo: Fallo biométrico: Usuario sin rostro registrado")
        raise HTTPException(404, "El usuario no tiene un rostro registrado")

    if not coincide:
        auditar(conn, id_usuario, "LOGIN_FALLIDO", "Usuario", id_usuario,
                f"Intento de login fallido para '{usuario}'. Motivo: Fallo biométrico: Verificación facial no coincidente.")
        return {"match": False, "score": score, "access_token": None, "detail": "Verificación facial no coincidente."}

    auditar(conn, id_usuario, "LOGIN_EXITOSO", "Usuario", id_usuario,
            f"Inicio de sesión exitoso con validación facial aprobada para usuario: {usuario}")
    return {
        "match": True,
        "score": score,
        "access_token": emitir_token(id_usuario, usuario, datos["rol"], "access", SESION),
        "detail": None,
    }


class EventoIn(BaseModel):
    accion: str
    nombre_usuario: str
    id_usuario: Optional[int] = None
    detalle: str
    fecha_cliente: str
    origen: str = "ANDROID"


@router.post("/api/auditoria/eventos", status_code=204)
def evento_del_dispositivo(body: EventoIn, conn=Depends(db)):
    """
    Fallos de login que ocurrieron en el teléfono sin llegar al servidor (sin red, permiso de
    cámara negado, cancelación). Sin token, por eso solo acepta LOGIN_FALLIDO.
    """
    if body.accion != "LOGIN_FALLIDO":
        raise HTTPException(400, "Solo se aceptan eventos LOGIN_FALLIDO")
    auditar(
        conn, body.id_usuario, "LOGIN_FALLIDO", "Usuario", body.id_usuario,
        f"Intento de login fallido para '{body.nombre_usuario[:50]}'. Motivo: {body.detalle} "
        f"[{body.origen[:20]}, {body.fecha_cliente[:40]}]",
    )


# ----------------------------------------------------------------------- usuarios

@router.get("/api/usuarios")
def listar_usuarios(_: dict = Depends(requiere_rol("ADMIN")), conn=Depends(db)):
    """Tabla "USUARIOS REGISTRADOS" (UsuarioRepository.listarTodos, sin la contraseña)."""
    filas = conn.execute(
        """SELECT u.IdUsuario, u.Nombre, u.Apellido, u.NombreUsuario, u.Correo, r.Nombre AS Rol, u.Estado
           FROM Usuario u LEFT JOIN Rol r ON r.IdRol = u.IdRol"""
    ).fetchall()
    return [
        {"id_usuario": f.IdUsuario, "nombre": f.Nombre, "apellido": f.Apellido, "nombre_usuario": f.NombreUsuario,
         "correo": f.Correo, "rol": f.Rol, "estado": f.Estado}
        for f in filas
    ]


class UsuarioIn(BaseModel):
    nombre: str
    apellido: str
    nombre_usuario: str
    correo: str
    contrasena: str
    rol: str


@router.post("/api/usuarios", status_code=201)
def crear_usuario(body: UsuarioIn, _: dict = Depends(requiere_rol("ADMIN")), conn=Depends(db)):
    """UsuarioService.registrarUsuario: BCrypt costo 12 y el procedimiento CrearUsuario."""
    nombre = obligatorio(body.nombre, "El nombre es obligatorio.")
    apellido = obligatorio(body.apellido, "El apellido es obligatorio.")
    usuario = obligatorio(body.nombre_usuario, "El nombre de usuario es obligatorio.")
    obligatorio(body.contrasena, "La contraseña es obligatoria.")
    correo = obligatorio(body.correo, "El correo electrónico es obligatorio.")
    id_rol = conn.execute("SELECT IdRol FROM Rol WHERE UPPER(Nombre) = ? AND Estado = 'ACTIVO'", body.rol.upper()).fetchval()
    if id_rol is None:
        raise HTTPException(422, "Debe seleccionar un rol.")

    # Prefijo $2a$ y costo 12, igual que jBCrypt en escritorio ($2b$ lo rechaza jBCrypt).
    # Se cifra la contraseña tal cual: el login la compara sin recortar espacios. (UsuarioService de
    # escritorio la recorta al crear y no al validar, así que una contraseña con espacios no le funciona.)
    hash_contrasena = bcrypt.hashpw(body.contrasena.encode(), bcrypt.gensalt(rounds=12, prefix=b"2a")).decode()
    try:
        id_usuario = conn.execute(
            "{CALL CrearUsuario(?, ?, ?, ?, ?, ?)}", nombre, apellido, usuario, hash_contrasena, correo, id_rol,
        ).fetchval()
        conn.commit()
    except pyodbc.Error as e:
        conn.rollback()
        mensaje = str(e).lower()
        # RAISERROR del procedimiento: 'el nombre de usuario ya existe' / 'el correo ya tiene un usuario registrado'.
        if "ya existe" in mensaje or "ya tiene un usuario" in mensaje or "unique" in mensaje:
            raise HTTPException(409, "El nombre de usuario o el correo ya están registrados.")
        raise
    return {"id_usuario": int(id_usuario)}


@router.post("/face/register")
def face_register(
    id_usuario: int = Form(...),
    imagen: UploadFile = File(...),
    _: dict = Depends(requiere_rol("ADMIN")),
    conn=Depends(db),
):
    try:
        registrar_perfil(conn, id_usuario, imagen.file.read())
    except RostroNoDetectado:
        return {"ok": False, "detail": "No se detectó un rostro en la imagen"}
    return {"ok": True, "detail": None}


# ----------------------------------------------------------------------- dashboard

@router.get("/api/dashboard/metricas")
def metricas(_: dict = Depends(requiere_rol(*TODOS_LOS_ROLES)), conn=Depends(db)):
    """DashboardRepository.obtenerResumen."""
    conteos = {"DISPONIBLE": 0, "PRESTADO": 0, "DANADO": 0, "MANTENIMIENTO": 0, "BAJA": 0}
    for f in conn.execute("SELECT Estado, COUNT(*) AS Cantidad FROM Equipo GROUP BY Estado").fetchall():
        estado = f.Estado.strip().upper()
        if estado in conteos:
            conteos[estado] = f.Cantidad
    activos = conn.execute("SELECT COUNT(*) FROM Prestamo WHERE Estado = 'ACTIVO'").fetchval()
    # RN-17: vencido = fecha esperada ya pasó y todavía hay equipos pendientes.
    vencidos = conn.execute(
        """SELECT COUNT(*) FROM Prestamo p
           WHERE p.Estado = 'ACTIVO' AND p.FechaEsperadaDevolucion < SYSDATETIME()
             AND EXISTS (SELECT 1 FROM DetallePrestamo dp WHERE dp.IdPrestamo = p.IdPrestamo AND dp.Estado = 'PENDIENTE')"""
    ).fetchval()
    return {
        "total_equipos": sum(conteos.values()),
        "disponibles": conteos["DISPONIBLE"],
        "prestados": conteos["PRESTADO"],
        "danados": conteos["DANADO"],
        "en_mantenimiento": conteos["MANTENIMIENTO"],
        "de_baja": conteos["BAJA"],
        "prestamos_activos": activos,
        "prestamos_vencidos": vencidos,
    }


# --------------------------------------------------------------------- laboratorios

def _laboratorio_json(f) -> dict:
    return {"id_laboratorio": f.IdLaboratorio, "codigo": f.Codigo, "nombre": f.Nombre,
            "ubicacion": f.Ubicacion, "capacidad": f.Capacidad, "estado": f.Estado}


_SELECT_LABORATORIO = "SELECT IdLaboratorio, Codigo, Nombre, Ubicacion, Capacidad, Estado FROM Laboratorio"


class LaboratorioIn(BaseModel):
    codigo: Optional[str] = None
    nombre: str
    ubicacion: Optional[str] = None
    capacidad: int
    estado: Optional[str] = None


def _validar_laboratorio(body: LaboratorioIn) -> str:
    nombre = obligatorio(body.nombre, "El nombre del laboratorio es obligatorio.")
    if body.capacidad <= 0:
        raise HTTPException(422, "La capacidad debe ser un número mayor que cero.")
    return nombre


@router.get("/api/laboratorios")
def laboratorios(_: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    return [_laboratorio_json(f) for f in conn.execute(_SELECT_LABORATORIO).fetchall()]


@router.post("/api/laboratorios", status_code=201)
def crear_laboratorio(body: LaboratorioIn, _: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """LaboratorioService.registrarLaboratorio: el código LAB-00001 se genera aquí, nunca lo manda la app."""
    nombre = _validar_laboratorio(body)
    try:
        # Mismo cálculo que LaboratorioRepository.generarSiguienteCodigo, con bloqueo para que dos
        # altas simultáneas no obtengan el mismo número.
        siguiente = conn.execute(
            """SELECT ISNULL(MAX(CAST(SUBSTRING(Codigo, 5, 5) AS INT)), 0) + 1
               FROM Laboratorio WITH (UPDLOCK, HOLDLOCK)
               WHERE Codigo LIKE 'LAB-[0-9][0-9][0-9][0-9][0-9]'"""
        ).fetchval()
        codigo = f"LAB-{siguiente:05d}"
        id_lab = conn.execute(
            """INSERT INTO Laboratorio (Codigo, Nombre, Ubicacion, Capacidad, Estado)
               OUTPUT INSERTED.IdLaboratorio VALUES (?, ?, ?, ?, 'ACTIVO')""",
            codigo, nombre, (body.ubicacion or "").strip(), body.capacidad,
        ).fetchval()
        conn.commit()
    except pyodbc.IntegrityError:
        conn.rollback()
        raise HTTPException(409, "No se pudo guardar el laboratorio. Verifique que el nombre no esté repetido.")
    return _laboratorio_json(conn.execute(_SELECT_LABORATORIO + " WHERE IdLaboratorio = ?", id_lab).fetchone())


@router.put("/api/laboratorios/{id_laboratorio}")
def actualizar_laboratorio(id_laboratorio: int, body: LaboratorioIn, _: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)),
                           conn=Depends(db)):
    """LaboratorioService.actualizarLaboratorio. El código no se cambia desde el celular."""
    nombre = _validar_laboratorio(body)
    estado = (body.estado or "ACTIVO").strip().upper()
    if estado not in ("ACTIVO", "INACTIVO"):
        raise HTTPException(422, "Estado inválido")
    filas = conn.execute(
        "UPDATE Laboratorio SET Nombre = ?, Ubicacion = ?, Capacidad = ?, Estado = ? WHERE IdLaboratorio = ?",
        nombre, (body.ubicacion or "").strip(), body.capacidad, estado, id_laboratorio,
    ).rowcount
    conn.commit()
    if filas == 0:
        raise HTTPException(404, "No se pudo actualizar el laboratorio.")
    return _laboratorio_json(conn.execute(_SELECT_LABORATORIO + " WHERE IdLaboratorio = ?", id_laboratorio).fetchone())


# ------------------------------------------------------------------ tipos de equipo

class TipoEquipoIn(BaseModel):
    nombre: str
    descripcion: Optional[str] = None


@router.get("/api/tipos-equipo")
def tipos_equipo(_: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    filas = conn.execute("SELECT IdTipoEquipo, Nombre, Descripcion FROM TipoEquipo").fetchall()
    return [{"id_tipo_equipo": f.IdTipoEquipo, "nombre": f.Nombre, "descripcion": f.Descripcion} for f in filas]


@router.post("/api/tipos-equipo", status_code=201)
def crear_tipo_equipo(body: TipoEquipoIn, _: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """TipoEquipoService.registrarTipoEquipo (escritorio no edita ni elimina tipos)."""
    nombre = obligatorio(body.nombre, "El nombre del tipo de equipo es obligatorio.")
    descripcion = (body.descripcion or "").strip()
    try:
        id_tipo = conn.execute(
            "INSERT INTO TipoEquipo (Nombre, Descripcion) OUTPUT INSERTED.IdTipoEquipo VALUES (?, ?)", nombre, descripcion,
        ).fetchval()
        conn.commit()
    except pyodbc.IntegrityError:  # UQ_TipoEquipo_Nombre
        conn.rollback()
        raise HTTPException(409, f"Ya existe el tipo «{nombre}»")
    return {"id_tipo_equipo": id_tipo, "nombre": nombre, "descripcion": descripcion}


# -------------------------------------------------------------------------- equipos

_SELECT_EQUIPO = """SELECT e.IdEquipo, e.CodigoInventario, e.Nombre, e.IdTipoEquipo, t.Nombre AS TipoNombre,
                           e.IdLaboratorio, l.Nombre AS LabNombre, e.Estado, e.Marca, e.Modelo, e.NumeroSerie
                    FROM Equipo e
                    INNER JOIN TipoEquipo t ON e.IdTipoEquipo = t.IdTipoEquipo
                    INNER JOIN Laboratorio l ON e.IdLaboratorio = l.IdLaboratorio"""


def _equipo_json(f) -> dict:
    return {
        "id_equipo": f.IdEquipo, "codigo_inventario": f.CodigoInventario, "nombre": f.Nombre,
        "id_tipo_equipo": f.IdTipoEquipo, "tipo": f.TipoNombre, "id_laboratorio": f.IdLaboratorio,
        "laboratorio": f.LabNombre, "estado": f.Estado.strip().upper(),
        "marca": f.Marca, "modelo": f.Modelo, "numero_serie": f.NumeroSerie,
    }


@router.get("/api/equipos")
def listar_equipos(_: dict = Depends(requiere_rol(*TODOS_LOS_ROLES)), conn=Depends(db)):
    """EquipoRepository.listarTodos."""
    return [_equipo_json(f) for f in conn.execute(_SELECT_EQUIPO).fetchall()]


@router.get("/api/equipos/codigo/{codigo}")
def equipo_por_codigo(codigo: str, _: dict = Depends(requiere_rol(*TODOS_LOS_ROLES)), conn=Depends(db)):
    """Lectura del QR (QrService.identificarEquipo). Solo consulta: no cambia el estado (RN-19)."""
    fila = conn.execute(_SELECT_EQUIPO + " WHERE e.CodigoInventario = ?", codigo.strip()).fetchone()
    if fila is None:
        raise HTTPException(404, f"El QR corresponde al código \"{codigo.strip()}\", que no existe en el sistema.")
    return _equipo_json(fila)


class EquipoIn(BaseModel):
    nombre: str
    id_tipo_equipo: int
    id_laboratorio: int
    marca: str = ""
    modelo: str = ""
    numero_serie: str = ""


def _validar_equipo(body: EquipoIn) -> str:
    nombre = obligatorio(body.nombre, "El nombre del equipo es obligatorio.")
    if body.id_tipo_equipo <= 0:
        raise HTTPException(422, "Debe seleccionar un tipo de equipo.")
    if body.id_laboratorio <= 0:
        raise HTTPException(422, "Debe seleccionar un laboratorio.")
    return nombre


@router.post("/api/equipos", status_code=201)
def crear_equipo(body: EquipoIn, _: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """EquipoService.registrarEquipo: código EQP00001 generado y estado inicial DISPONIBLE."""
    nombre = _validar_equipo(body)
    try:
        # Mismo cálculo que EquipoRepository.obtenerSiguienteCodigo, con bloqueo contra altas simultáneas.
        siguiente = conn.execute(
            """SELECT ISNULL(MAX(CAST(RIGHT(CodigoInventario, 5) AS INT)), 0) + 1
               FROM Equipo WITH (UPDLOCK, HOLDLOCK) WHERE CodigoInventario LIKE 'EQP%'"""
        ).fetchval()
        id_equipo = conn.execute(
            """INSERT INTO Equipo (CodigoInventario, Nombre, IdTipoEquipo, IdLaboratorio, Estado, Marca, Modelo, NumeroSerie)
               OUTPUT INSERTED.IdEquipo VALUES (?, ?, ?, ?, 'DISPONIBLE', ?, ?, ?)""",
            f"EQP{siguiente:05d}", nombre, body.id_tipo_equipo, body.id_laboratorio,
            body.marca.strip(), body.modelo.strip(), body.numero_serie.strip(),
        ).fetchval()
        conn.commit()
    except pyodbc.IntegrityError:
        conn.rollback()
        raise HTTPException(409, "No se pudo guardar el equipo en la base de datos.")
    return _equipo_json(conn.execute(_SELECT_EQUIPO + " WHERE e.IdEquipo = ?", id_equipo).fetchone())


@router.put("/api/equipos/{id_equipo}")
def actualizar_equipo(id_equipo: int, body: EquipoIn, _: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """
    EquipoService.actualizarEquipo. Diferencia intencional: no cambia el código ni el estado;
    el estado solo lo mueven préstamos, devoluciones y mantenimientos.
    """
    nombre = _validar_equipo(body)
    filas = conn.execute(
        """UPDATE Equipo SET Nombre = ?, IdTipoEquipo = ?, IdLaboratorio = ?, Marca = ?, Modelo = ?, NumeroSerie = ?
           WHERE IdEquipo = ?""",
        nombre, body.id_tipo_equipo, body.id_laboratorio, body.marca.strip(), body.modelo.strip(),
        body.numero_serie.strip(), id_equipo,
    ).rowcount
    conn.commit()
    if filas == 0:
        raise HTTPException(404, "No se pudo actualizar el equipo.")
    return _equipo_json(conn.execute(_SELECT_EQUIPO + " WHERE e.IdEquipo = ?", id_equipo).fetchone())


# ----------------------------------------------------------- préstamos y devoluciones

@router.get("/api/prestamos")
def prestamos_activos(_: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """Préstamos ACTIVOS con todos sus detalles (PrestamoRepository.listarActivos agrupado por préstamo)."""
    filas = conn.execute(
        """SELECT p.IdPrestamo, p.Solicitante, p.FechaPrestamo, p.FechaEsperadaDevolucion, p.Estado, p.Observaciones,
                  dp.IdEquipo, dp.Estado AS EstadoDetalle, e.CodigoInventario, e.Nombre AS EquipoNombre
           FROM Prestamo p
           INNER JOIN DetallePrestamo dp ON p.IdPrestamo = dp.IdPrestamo
           INNER JOIN Equipo e ON dp.IdEquipo = e.IdEquipo
           WHERE p.Estado = 'ACTIVO'
           ORDER BY p.IdPrestamo, e.CodigoInventario"""
    ).fetchall()
    prestamos: dict[int, dict] = {}
    for f in filas:
        prestamo = prestamos.setdefault(f.IdPrestamo, {
            "id_prestamo": f.IdPrestamo, "solicitante": f.Solicitante, "fecha_prestamo": f.FechaPrestamo,
            "fecha_devolucion_prevista": f.FechaEsperadaDevolucion, "estado": f.Estado,
            "observaciones": f.Observaciones, "detalles": [],
        })
        prestamo["detalles"].append({
            "id_equipo": f.IdEquipo, "codigo_inventario": f.CodigoInventario, "nombre": f.EquipoNombre,
            "devuelto": f.EstadoDetalle.strip().upper() == "DEVUELTO",
        })
    return list(prestamos.values())


class PrestamoIn(BaseModel):
    solicitante: str
    ids_equipos: list[int]
    fecha_devolucion_prevista: dt.datetime
    observaciones: str


@router.post("/api/prestamos", status_code=201)
def crear_prestamo(body: PrestamoIn, usuario: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """PrestamoService.realizarPrestamoMultiple + PrestamoRepository.registrarPrestamoMultiple."""
    id_usuario = int(usuario["sub"])
    solicitante = obligatorio(body.solicitante, "El nombre del solicitante es obligatorio.")
    if len(solicitante) > 150:  # Prestamo.Solicitante VARCHAR(150)
        raise HTTPException(422, "El solicitante admite máximo 150 caracteres.")
    if not body.ids_equipos:
        raise HTTPException(422, "Debe agregar al menos un equipo al préstamo.")  # RN-05
    observaciones = obligatorio(body.observaciones, "Las observaciones del préstamo son obligatorias.")
    if len(observaciones) > 500:
        raise HTTPException(422, "Las observaciones admiten máximo 500 caracteres.")
    if body.fecha_devolucion_prevista.date() < dt.date.today():
        raise HTTPException(422, "La fecha de devolucion no puede ser menor a la de hoy")
    if len(set(body.ids_equipos)) != len(body.ids_equipos):
        raise HTTPException(422, "Hay equipos duplicados en la selección.")  # RN-06

    try:
        id_prestamo = conn.execute(
            """INSERT INTO Prestamo (Solicitante, FechaPrestamo, FechaEsperadaDevolucion, IdUsuarioRegistra, Estado, Observaciones)
               OUTPUT INSERTED.IdPrestamo VALUES (?, SYSDATETIME(), ?, ?, 'ACTIVO', ?)""",
            solicitante, body.fecha_devolucion_prevista, id_usuario, observaciones,
        ).fetchval()
        for id_equipo in body.ids_equipos:
            # RN-07/08/20: la disponibilidad se verifica en la BD. El WHERE Estado = 'DISPONIBLE' además
            # evita que dos usuarios presten el mismo equipo a la vez.
            if conn.execute(
                "UPDATE Equipo SET Estado = 'PRESTADO' WHERE IdEquipo = ? AND Estado = 'DISPONIBLE'", id_equipo,
            ).rowcount != 1:
                fila = conn.execute("SELECT CodigoInventario, Nombre, Estado FROM Equipo WHERE IdEquipo = ?", id_equipo).fetchone()
                conn.rollback()
                if fila is None:
                    raise HTTPException(409, f"El equipo {id_equipo} ya no existe.")
                raise HTTPException(409, f"El equipo {fila.CodigoInventario} ({fila.Nombre}) no está disponible. "
                                         f"Estado actual: {fila.Estado}")
            conn.execute("INSERT INTO DetallePrestamo (IdPrestamo, IdEquipo, Estado) VALUES (?, ?, 'PENDIENTE')",
                         id_prestamo, id_equipo)
        auditar(conn, id_usuario, "PRESTAMO_CREADO", "Prestamo", id_prestamo,
                f"Préstamo a {solicitante} con {len(body.ids_equipos)} equipo(s).", confirmar=False)
        conn.commit()
    except HTTPException:
        raise
    except Exception:
        conn.rollback()
        raise
    return {"id_prestamo": id_prestamo}


class ItemDevolucionIn(BaseModel):
    id_equipo: int
    condicion: str = "BUENO"
    observacion: Optional[str] = None


class DevolucionIn(BaseModel):
    id_prestamo: int
    items: list[ItemDevolucionIn]


@router.post("/api/devoluciones")
def registrar_devolucion(body: DevolucionIn, usuario: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """PrestamoService.procesarDevolucion + PrestamoRepository.registrarDevolucion, por cada equipo."""
    id_usuario = int(usuario["sub"])
    if not body.items:
        raise HTTPException(422, "Seleccione al menos un equipo a devolver.")
    estado_prestamo = conn.execute("SELECT Estado FROM Prestamo WHERE IdPrestamo = ?", body.id_prestamo).fetchval()
    if estado_prestamo is None:
        raise HTTPException(404, "El préstamo no existe.")
    if estado_prestamo.strip().upper() != "ACTIVO":
        raise HTTPException(409, f"El préstamo #{body.id_prestamo} no está ACTIVO.")
    try:
        for item in body.items:
            condicion = (item.condicion or "BUENO").strip().upper()
            if condicion not in ("BUENO", "DANADO"):
                raise HTTPException(422, "Condición inválida: use BUENO o DANADO")
            id_detalle = conn.execute(
                "SELECT IdDetallePrestamo FROM DetallePrestamo WHERE IdPrestamo = ? AND IdEquipo = ? AND Estado = 'PENDIENTE'",
                body.id_prestamo, item.id_equipo,
            ).fetchval()
            if id_detalle is None:  # RN-11: no se devuelve dos veces
                conn.rollback()
                raise HTTPException(409, "No se pudo procesar la devolución. Verifique si el equipo ya fue devuelto.")
            conn.execute(
                """INSERT INTO Devolucion (IdDetallePrestamo, FechaDevolucion, Condicion, Observacion, IdUsuarioRecibe)
                   VALUES (?, SYSDATETIME(), ?, ?, ?)""",
                id_detalle, condicion, item.observacion, id_usuario,
            )
            conn.execute("UPDATE DetallePrestamo SET Estado = 'DEVUELTO' WHERE IdDetallePrestamo = ?", id_detalle)
            # RN-12 / RN-13
            conn.execute("UPDATE Equipo SET Estado = ? WHERE IdEquipo = ?",
                         "DANADO" if condicion == "DANADO" else "DISPONIBLE", item.id_equipo)
            auditar(conn, id_usuario, "EQUIPO_DEVUELTO", "Equipo", item.id_equipo,
                    f"Equipo devuelto en condición {condicion} (préstamo {body.id_prestamo}).", confirmar=False)

        # RN-16: sin detalles pendientes el préstamo pasa a FINALIZADO.
        pendientes = conn.execute(
            "SELECT COUNT(*) FROM DetallePrestamo WHERE IdPrestamo = ? AND Estado = 'PENDIENTE'", body.id_prestamo,
        ).fetchval()
        if pendientes == 0:
            conn.execute("UPDATE Prestamo SET Estado = 'FINALIZADO' WHERE IdPrestamo = ?", body.id_prestamo)
        conn.commit()
    except HTTPException:
        raise
    except Exception:
        conn.rollback()
        raise
    return {"prestamo_finalizado": pendientes == 0, "equipos_pendientes": pendientes}


# ---------------------------------------------------------------------- mantenimiento

_SELECT_MANTENIMIENTO = """SELECT m.IdMantenimiento, m.IdEquipo, e.CodigoInventario, e.Nombre AS EquipoNombre,
                                  m.Tipo, m.FechaIngreso, m.Descripcion, m.Responsable, m.FechaSalida,
                                  m.Resultado, m.Observaciones
                           FROM Mantenimiento m INNER JOIN Equipo e ON m.IdEquipo = e.IdEquipo"""


def _mantenimiento_json(f) -> dict:
    return {
        "id_mantenimiento": f.IdMantenimiento, "id_equipo": f.IdEquipo, "codigo_inventario": f.CodigoInventario,
        "nombre_equipo": f.EquipoNombre, "tipo": f.Tipo, "fecha_ingreso": f.FechaIngreso,
        "descripcion": f.Descripcion, "responsable": f.Responsable, "fecha_salida": f.FechaSalida,
        "resultado": f.Resultado, "observaciones": f.Observaciones,
    }


@router.get("/api/mantenimientos")
def mantenimientos_en_proceso(_: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """MantenimientoRepository.listarEnProceso."""
    filas = conn.execute(
        _SELECT_MANTENIMIENTO + " WHERE m.Estado = 'EN_PROCESO' AND m.FechaSalida IS NULL ORDER BY m.FechaIngreso"
    ).fetchall()
    return [_mantenimiento_json(f) for f in filas]


class IngresoIn(BaseModel):
    id_equipo: int
    tipo: str
    descripcion: str
    responsable: str


@router.post("/api/mantenimientos", status_code=204)
def ingresar_mantenimiento(body: IngresoIn, usuario: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """MantenimientoService.ingresarAMantenimiento + MantenimientoRepository.registrarIngreso."""
    id_usuario = int(usuario["sub"])
    tipo = (body.tipo or "").strip().upper()
    if tipo not in ("PREVENTIVO", "CORRECTIVO"):
        raise HTTPException(422, "Seleccione un tipo de mantenimiento válido (PREVENTIVO o CORRECTIVO).")
    descripcion = obligatorio(body.descripcion, "Debe ingresar una descripción del problema o mantenimiento.")
    responsable = obligatorio(body.responsable, "Debe indicar el nombre del técnico o responsable.")
    if len(descripcion) > 500 or len(responsable) > 150:
        raise HTTPException(422, "La descripción admite 500 caracteres y el responsable 150.")
    estado = estado_equipo(conn, body.id_equipo)
    if estado is None:
        raise HTTPException(404, "El equipo seleccionado no existe.")
    if estado == "PRESTADO":
        raise HTTPException(409, "Un equipo PRESTADO no puede enviarse a mantenimiento. Debe devolverse primero.")
    if estado == "BAJA":
        raise HTTPException(409, "Un equipo en BAJA ya no puede ingresar a mantenimiento.")
    if estado == "MANTENIMIENTO":
        raise HTTPException(409, "El equipo ya se encuentra en mantenimiento actualmente.")
    # MantenimientoView solo ofrece DISPONIBLES para PREVENTIVO y DAÑADOS para CORRECTIVO.
    if estado != ("DISPONIBLE" if tipo == "PREVENTIVO" else "DANADO"):
        raise HTTPException(409, "Un mantenimiento PREVENTIVO es para equipos DISPONIBLES; uno CORRECTIVO, para equipos DAÑADOS.")

    try:
        id_mantenimiento = conn.execute(
            """INSERT INTO Mantenimiento (IdEquipo, FechaIngreso, Tipo, Descripcion, Responsable, Estado, IdUsuarioRegistra)
               OUTPUT INSERTED.IdMantenimiento VALUES (?, SYSDATETIME(), ?, ?, ?, 'EN_PROCESO', ?)""",
            body.id_equipo, tipo, descripcion, responsable, id_usuario,
        ).fetchval()
        # Solo cambia si el equipo sigue DISPONIBLE o DANADO (evita carreras entre usuarios).
        if conn.execute(
            "UPDATE Equipo SET Estado = 'MANTENIMIENTO' WHERE IdEquipo = ? AND Estado IN ('DISPONIBLE', 'DANADO')",
            body.id_equipo,
        ).rowcount != 1:
            conn.rollback()
            raise HTTPException(409, "El equipo ya no está DISPONIBLE ni DANADO.")
        auditar(conn, id_usuario, "EQUIPO_ENVIADO_MANTENIMIENTO", "Mantenimiento", id_mantenimiento,
                f"Equipo ID {body.id_equipo} enviado a mantenimiento {tipo}. Responsable: {responsable}", confirmar=False)
        conn.commit()
    except HTTPException:
        raise
    except Exception:
        conn.rollback()
        raise


class FinalizarIn(BaseModel):
    resultado: str
    observaciones: Optional[str] = None


@router.put("/api/mantenimientos/{id_mantenimiento}/finalizar", status_code=204)
def finalizar_mantenimiento(id_mantenimiento: int, body: FinalizarIn,
                            usuario: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)), conn=Depends(db)):
    """MantenimientoService.cerrarMantenimiento + MantenimientoRepository.finalizarMantenimiento."""
    id_usuario = int(usuario["sub"])
    resultado = (body.resultado or "").strip().upper()
    if resultado not in ("REPARADO", "NO_REPARABLE"):
        raise HTTPException(422, "Indique el resultado del cierre: REPARADO o NO_REPARABLE.")
    fila = conn.execute("SELECT IdEquipo, FechaSalida FROM Mantenimiento WHERE IdMantenimiento = ?", id_mantenimiento).fetchone()
    if fila is None:
        raise HTTPException(404, "El mantenimiento seleccionado no existe.")
    if fila.FechaSalida is not None:
        raise HTTPException(409, "Este mantenimiento ya fue finalizado anteriormente.")
    observaciones = (body.observaciones or "").strip() or None
    nuevo_estado = "DISPONIBLE" if resultado == "REPARADO" else "BAJA"  # RN-15

    try:
        if conn.execute(
            """UPDATE Mantenimiento SET FechaSalida = SYSDATETIME(), Resultado = ?, Estado = 'FINALIZADO', Observaciones = ?
               WHERE IdMantenimiento = ? AND Estado = 'EN_PROCESO'""",
            resultado, observaciones, id_mantenimiento,
        ).rowcount != 1:
            conn.rollback()
            raise HTTPException(409, "El mantenimiento ya no está EN_PROCESO.")
        # Un equipo en BAJA nunca vuelve a DISPONIBLE.
        if conn.execute(
            "UPDATE Equipo SET Estado = ? WHERE IdEquipo = ? AND Estado = 'MANTENIMIENTO'", nuevo_estado, fila.IdEquipo,
        ).rowcount != 1:
            conn.rollback()
            raise HTTPException(409, "El equipo no se encuentra en estado MANTENIMIENTO.")
        auditar(conn, id_usuario, "MANTENIMIENTO_FINALIZADO", "Mantenimiento", id_mantenimiento,
                f"Mantenimiento finalizado como {resultado}. Equipo ID {fila.IdEquipo} pasa a {nuevo_estado}", confirmar=False)
        conn.commit()
    except HTTPException:
        raise
    except Exception:
        conn.rollback()
        raise


# --------------------------------------------------------------------------- reportes

@router.get("/api/reportes/mantenimientos")
def historial_mantenimiento(codigo: Optional[str] = None, _: dict = Depends(requiere_rol(*ROLES_OPERATIVOS)),
                            conn=Depends(db)):
    """"Consultar Historial" de ReportesView: todos, o filtrados por código de inventario."""
    if codigo and codigo.strip():
        filas = conn.execute(_SELECT_MANTENIMIENTO + " WHERE e.CodigoInventario LIKE ? ORDER BY m.FechaIngreso DESC",
                             f"%{codigo.strip()}%").fetchall()
    else:
        filas = conn.execute(_SELECT_MANTENIMIENTO + " ORDER BY m.FechaIngreso DESC").fetchall()
    return [_mantenimiento_json(f) for f in filas]


@router.get("/api/auditoria")
def consultar_auditoria(
    desde: dt.date,
    hasta: dt.date,
    accion: Optional[str] = None,
    limite: int = 200,
    _: dict = Depends(requiere_rol("ADMIN")),
    conn=Depends(db),
):
    """Agregado de la app (escritorio no tiene este reporte): tabla Auditoria, solo ADMIN."""
    limite = max(1, min(limite, 200))
    sql = """SELECT TOP (?) a.IdAuditoria, a.FechaHora, a.Accion, a.Entidad, a.Descripcion, u.NombreUsuario
             FROM Auditoria a LEFT JOIN Usuario u ON u.IdUsuario = a.IdUsuario
             WHERE a.FechaHora >= ? AND a.FechaHora < DATEADD(DAY, 1, CAST(? AS DATE))"""
    parametros: list = [limite, desde, hasta]
    if accion:
        sql += " AND a.Accion = ?"
        parametros.append(accion)
    sql += " ORDER BY a.FechaHora DESC"
    return [
        {"id_auditoria": f.IdAuditoria, "fecha": f.FechaHora, "accion": f.Accion, "entidad": f.Entidad,
         "usuario": f.NombreUsuario, "detalle": f.Descripcion}
        for f in conn.execute(sql, *parametros).fetchall()
    ]
