package com.universidad.sigelab.core.demo

import com.google.gson.Gson
import com.universidad.sigelab.data.remote.dto.CrearPrestamoRequest
import com.universidad.sigelab.data.remote.dto.CrearPrestamoResponse
import com.universidad.sigelab.data.remote.dto.CrearUsuarioRequest
import com.universidad.sigelab.data.remote.dto.CrearUsuarioResponse
import com.universidad.sigelab.data.remote.dto.DashboardMetricasDto
import com.universidad.sigelab.data.remote.dto.DetallePrestamoDto
import com.universidad.sigelab.data.remote.dto.DevolucionRequest
import com.universidad.sigelab.data.remote.dto.DevolucionResponse
import com.universidad.sigelab.data.remote.dto.EquipoDto
import com.universidad.sigelab.data.remote.dto.EquipoRequest
import com.universidad.sigelab.data.remote.dto.EventoAuditoriaRequest
import com.universidad.sigelab.data.remote.dto.FaceRegisterResponse
import com.universidad.sigelab.data.remote.dto.FaceVerifyResponse
import com.universidad.sigelab.data.remote.dto.FinalizarMantenimientoRequest
import com.universidad.sigelab.data.remote.dto.IngresoMantenimientoRequest
import com.universidad.sigelab.data.remote.dto.LaboratorioDto
import com.universidad.sigelab.data.remote.dto.LoginRequest
import com.universidad.sigelab.data.remote.dto.LoginResponse
import com.universidad.sigelab.data.remote.dto.MantenimientoDto
import com.universidad.sigelab.data.remote.dto.PrestamoDto
import com.universidad.sigelab.data.remote.dto.RegistroAuditoriaDto
import com.universidad.sigelab.data.remote.dto.TipoEquipoDto
import com.universidad.sigelab.data.remote.dto.UsuarioDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servidor simulado del modo demo. Responde las mismas rutas y el mismo JSON que la API móvil
 * (backend-referencia/mobile_gateway.py), con datos en memoria y las reglas de escritorio:
 * códigos LAB-00001/EQP00001, cambios de estado, préstamos, devoluciones, mantenimiento y auditoría.
 * Los datos vuelven al estado inicial cada vez que se cierra la app.
 */
@Singleton
class ServidorDemo @Inject constructor() {

    // --- filas de las tablas, igual que tablas.sql ---

    private class Lab(val id: Int, val codigo: String, var nombre: String, var ubicacion: String, var capacidad: Int, var estado: String)
    private class Tipo(val id: Int, val nombre: String, val descripcion: String?)
    private class Eq(
        val id: Int, val codigo: String, var nombre: String, var idTipo: Int, var idLab: Int, var estado: String,
        var marca: String, var modelo: String, var serie: String,
    )
    private class Det(val idEquipo: Int, var estado: String)
    private class Pres(
        val id: Int, val solicitante: String, val fecha: LocalDateTime, val esperada: LocalDateTime,
        var estado: String, val observaciones: String, val detalles: MutableList<Det>,
    )
    private class Mant(
        val id: Int, val idEquipo: Int, val tipo: String, val ingreso: LocalDateTime, val descripcion: String,
        val responsable: String, var salida: LocalDateTime? = null, var resultado: String? = null,
        var estado: String = "EN_PROCESO", var observaciones: String? = null,
    )
    private class Usr(
        val id: Int, val nombre: String, val apellido: String, val usuario: String, val correo: String,
        val contrasena: String, val rol: String, val estado: String,
    )
    private class Aud(val id: Long, val fecha: LocalDateTime, val idUsuario: Int?, val accion: String, val entidad: String, val descripcion: String)

    private class ErrorDemo(val codigo: Int, val detalle: String) : Exception(detalle)

    private val gson = Gson()
    private val ahora: LocalDateTime get() = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

    private val laboratorios = mutableListOf<Lab>()
    private val tipos = mutableListOf<Tipo>()
    private val equipos = mutableListOf<Eq>()
    private val prestamos = mutableListOf<Pres>()
    private val mantenimientos = mutableListOf<Mant>()
    private val usuarios = mutableListOf<Usr>()
    private val auditoria = mutableListOf<Aud>()

    init {
        sembrar()
    }

    @Synchronized
    fun responder(request: Request): Response = try {
        val (codigo, cuerpo) = enrutar(request)
        respuesta(request, codigo, cuerpo)
    } catch (e: ErrorDemo) {
        respuesta(request, e.codigo, gson.toJson(mapOf("detail" to e.detalle)))
    }

    // ------------------------------------------------------------------------ rutas

    private fun enrutar(r: Request): Pair<Int, String> {
        val s = r.url.pathSegments.filter { it.isNotEmpty() }
        val ruta = s.joinToString("/")
        val metodo = r.method
        return when {
            metodo == "POST" && ruta == "api/auth/login" -> login(r)
            metodo == "POST" && ruta == "face/verify" -> verificarRostro(r)
            metodo == "POST" && ruta == "face/register" -> { exigirRol(r, "ADMIN"); ok(FaceRegisterResponse(true, null)) }
            metodo == "POST" && ruta == "api/auditoria/eventos" -> eventoDispositivo(r)
            metodo == "GET" && ruta == "api/dashboard/metricas" -> { exigirRol(r, *TODOS); ok(metricas()) }

            metodo == "GET" && ruta == "api/laboratorios" -> { exigirRol(r, *OPERATIVOS); ok(laboratorios.map(::labDto)) }
            metodo == "POST" && ruta == "api/laboratorios" -> crearLaboratorio(r)
            metodo == "PUT" && s.size == 3 && s[1] == "laboratorios" -> actualizarLaboratorio(r, s[2].toInt())

            metodo == "GET" && ruta == "api/tipos-equipo" -> { exigirRol(r, *OPERATIVOS); ok(tipos.map { TipoEquipoDto(it.id, it.nombre, it.descripcion) }) }
            metodo == "POST" && ruta == "api/tipos-equipo" -> crearTipo(r)

            metodo == "GET" && ruta == "api/equipos" -> { exigirRol(r, *TODOS); ok(equipos.map(::equipoDto)) }
            metodo == "GET" && s.size == 4 && s[1] == "equipos" && s[2] == "codigo" -> { exigirRol(r, *TODOS); ok(equipoDto(porCodigo(s[3]))) }
            metodo == "POST" && ruta == "api/equipos" -> crearEquipo(r)
            metodo == "PUT" && s.size == 3 && s[1] == "equipos" -> actualizarEquipo(r, s[2].toInt())

            metodo == "GET" && ruta == "api/prestamos" -> { exigirRol(r, *OPERATIVOS); ok(prestamos.filter { it.estado == "ACTIVO" }.map(::prestamoDto)) }
            metodo == "POST" && ruta == "api/prestamos" -> crearPrestamo(r)
            metodo == "POST" && ruta == "api/devoluciones" -> devolver(r)

            metodo == "GET" && ruta == "api/mantenimientos" ->
                { exigirRol(r, *OPERATIVOS); ok(mantenimientos.filter { it.estado == "EN_PROCESO" }.sortedBy { it.ingreso }.map(::mantDto)) }
            metodo == "POST" && ruta == "api/mantenimientos" -> ingresarMantenimiento(r)
            metodo == "PUT" && s.size == 4 && s[1] == "mantenimientos" && s[3] == "finalizar" -> finalizarMantenimiento(r, s[2].toInt())

            metodo == "GET" && ruta == "api/reportes/mantenimientos" -> historialMantenimiento(r)
            metodo == "GET" && ruta == "api/auditoria" -> consultarAuditoria(r)

            metodo == "GET" && ruta == "api/usuarios" -> { exigirRol(r, "ADMIN"); ok(usuarios.map(::usuarioDto)) }
            metodo == "POST" && ruta == "api/usuarios" -> crearUsuario(r)

            else -> throw ErrorDemo(404, "Ruta no disponible en el modo demo: $metodo /$ruta")
        }
    }

    // ------------------------------------------------------------- autenticación

    private fun login(r: Request): Pair<Int, String> {
        val body = leer<LoginRequest>(r)
        val u = usuarios.firstOrNull { it.usuario.equals(body.usuario.trim(), ignoreCase = true) }
        if (u == null || u.estado.uppercase() != "ACTIVO" || u.contrasena != body.contrasena) {
            auditar(null, "LOGIN_FALLIDO", "Usuario",
                "Intento de login fallido para '${body.usuario.take(50)}'. Motivo: Usuario/contraseña incorrectos o usuario inactivo")
            throw ErrorDemo(401, "Usuario o contraseña incorrectos, o usuario inactivo.")
        }
        return ok(LoginResponse(u.id, u.usuario, "${u.nombre} ${u.apellido}", u.rol, "demo-pre-${u.id}"))
    }

    /** En el demo cualquier rostro coincide: sirve para mostrar el flujo completo. */
    private fun verificarRostro(r: Request): Pair<Int, String> {
        val u = usuarioDelToken(r, "pre")
        auditar(u.id, "LOGIN_EXITOSO", "Usuario", "Inicio de sesión exitoso con validación facial aprobada para usuario: ${u.usuario}")
        return ok(FaceVerifyResponse(match = true, score = 0.93, accessToken = "demo-acceso-${u.id}", detalle = null))
    }

    /**
     * Solo acepta eventos de los usuarios de ejemplo. Los LOGIN_FALLIDO reales que esperan en la
     * bandeja reciben 503: se quedan en el teléfono para el servidor real.
     */
    private fun eventoDispositivo(r: Request): Pair<Int, String> {
        val e = leer<EventoAuditoriaRequest>(r)
        if (!e.nombreUsuario.endsWith(".demo")) throw ErrorDemo(503, "El modo demo no recibe eventos reales")
        auditar(e.idUsuario, "LOGIN_FALLIDO", "Usuario", "Intento de login fallido para '${e.nombreUsuario}'. Motivo: ${e.detalle}")
        return 204 to ""
    }

    // ------------------------------------------------------------------ catálogos

    private fun crearLaboratorio(r: Request): Pair<Int, String> {
        exigirRol(r, *OPERATIVOS)
        val b = leer<LaboratorioDto>(r)
        if (b.nombre.isBlank()) throw ErrorDemo(422, "El nombre del laboratorio es obligatorio.")
        if (b.capacidad <= 0) throw ErrorDemo(422, "La capacidad debe ser un número mayor que cero.")
        val siguiente = (laboratorios.mapNotNull { it.codigo.removePrefix("LAB-").toIntOrNull() }.maxOrNull() ?: 0) + 1
        val lab = Lab(siguienteId(laboratorios.map { it.id }), "LAB-%05d".format(siguiente), b.nombre.trim(), b.ubicacion.orEmpty().trim(), b.capacidad, "ACTIVO")
        laboratorios += lab
        return 201 to gson.toJson(labDto(lab))
    }

    private fun actualizarLaboratorio(r: Request, id: Int): Pair<Int, String> {
        exigirRol(r, *OPERATIVOS)
        val b = leer<LaboratorioDto>(r)
        val lab = laboratorios.firstOrNull { it.id == id } ?: throw ErrorDemo(404, "No se pudo actualizar el laboratorio.")
        if (b.nombre.isBlank()) throw ErrorDemo(422, "El nombre del laboratorio es obligatorio.")
        if (b.capacidad <= 0) throw ErrorDemo(422, "La capacidad debe ser un número mayor que cero.")
        lab.nombre = b.nombre.trim()
        lab.ubicacion = b.ubicacion.orEmpty().trim()
        lab.capacidad = b.capacidad
        lab.estado = b.estado?.uppercase() ?: lab.estado
        return ok(labDto(lab))
    }

    private fun crearTipo(r: Request): Pair<Int, String> {
        exigirRol(r, *OPERATIVOS)
        val b = leer<TipoEquipoDto>(r)
        val nombre = b.nombre.trim()
        if (nombre.isEmpty()) throw ErrorDemo(422, "El nombre del tipo de equipo es obligatorio.")
        if (tipos.any { it.nombre.equals(nombre, ignoreCase = true) }) throw ErrorDemo(409, "Ya existe el tipo «$nombre»")
        val tipo = Tipo(siguienteId(tipos.map { it.id }), nombre, b.descripcion?.trim().orEmpty())
        tipos += tipo
        return 201 to gson.toJson(TipoEquipoDto(tipo.id, tipo.nombre, tipo.descripcion))
    }

    // -------------------------------------------------------------------- equipos

    private fun crearEquipo(r: Request): Pair<Int, String> {
        exigirRol(r, *OPERATIVOS)
        val b = leer<EquipoRequest>(r)
        validarEquipo(b)
        val siguiente = (equipos.mapNotNull { it.codigo.removePrefix("EQP").toIntOrNull() }.maxOrNull() ?: 0) + 1
        val eq = Eq(
            siguienteId(equipos.map { it.id }), "EQP%05d".format(siguiente), b.nombre.trim(), b.idTipoEquipo, b.idLaboratorio,
            "DISPONIBLE", b.marca.trim(), b.modelo.trim(), b.numeroSerie.trim(),
        )
        equipos += eq
        return 201 to gson.toJson(equipoDto(eq))
    }

    private fun actualizarEquipo(r: Request, id: Int): Pair<Int, String> {
        exigirRol(r, *OPERATIVOS)
        val b = leer<EquipoRequest>(r)
        validarEquipo(b)
        val eq = equipos.firstOrNull { it.id == id } ?: throw ErrorDemo(404, "No se pudo actualizar el equipo.")
        eq.nombre = b.nombre.trim()
        eq.idTipo = b.idTipoEquipo
        eq.idLab = b.idLaboratorio
        eq.marca = b.marca.trim()
        eq.modelo = b.modelo.trim()
        eq.serie = b.numeroSerie.trim()
        return ok(equipoDto(eq))
    }

    private fun validarEquipo(b: EquipoRequest) {
        if (b.nombre.isBlank()) throw ErrorDemo(422, "El nombre del equipo es obligatorio.")
        if (tipos.none { it.id == b.idTipoEquipo }) throw ErrorDemo(422, "Debe seleccionar un tipo de equipo.")
        if (laboratorios.none { it.id == b.idLaboratorio }) throw ErrorDemo(422, "Debe seleccionar un laboratorio.")
    }

    private fun porCodigo(codigo: String): Eq =
        equipos.firstOrNull { it.codigo.equals(codigo.trim(), ignoreCase = true) }
            ?: throw ErrorDemo(404, "El QR corresponde al código \"${codigo.trim()}\", que no existe en el sistema.")

    // ------------------------------------------------- préstamos y devoluciones

    private fun crearPrestamo(r: Request): Pair<Int, String> {
        val u = exigirRol(r, *OPERATIVOS)
        val b = leer<CrearPrestamoRequest>(r)
        if (b.solicitante.isBlank()) throw ErrorDemo(422, "El nombre del solicitante es obligatorio.")
        if (b.idsEquipos.isEmpty()) throw ErrorDemo(422, "Debe agregar al menos un equipo al préstamo.")
        if (b.observaciones.isBlank()) throw ErrorDemo(422, "Las observaciones del préstamo son obligatorias.")
        val esperada = LocalDateTime.parse(b.fechaDevolucionPrevista)
        if (esperada.toLocalDate().isBefore(LocalDate.now())) throw ErrorDemo(422, "La fecha de devolucion no puede ser menor a la de hoy")
        if (b.idsEquipos.toSet().size != b.idsEquipos.size) throw ErrorDemo(422, "Hay equipos duplicados en la selección.")
        val elegidos = b.idsEquipos.map { id -> equipos.firstOrNull { it.id == id } ?: throw ErrorDemo(409, "El equipo $id ya no existe.") }
        elegidos.firstOrNull { it.estado != "DISPONIBLE" }?.let {
            throw ErrorDemo(409, "El equipo ${it.codigo} (${it.nombre}) no está disponible. Estado actual: ${it.estado}")
        }
        elegidos.forEach { it.estado = "PRESTADO" }
        val prestamo = Pres(
            siguienteId(prestamos.map { it.id }), b.solicitante.trim(), ahora, esperada, "ACTIVO", b.observaciones.trim(),
            elegidos.map { Det(it.id, "PENDIENTE") }.toMutableList(),
        )
        prestamos += prestamo
        auditar(u.id, "PRESTAMO_CREADO", "Prestamo", "Préstamo a ${prestamo.solicitante} con ${elegidos.size} equipo(s).")
        return 201 to gson.toJson(CrearPrestamoResponse(prestamo.id))
    }

    private fun devolver(r: Request): Pair<Int, String> {
        val u = exigirRol(r, *OPERATIVOS)
        val b = leer<DevolucionRequest>(r)
        if (b.items.isEmpty()) throw ErrorDemo(422, "Seleccione al menos un equipo a devolver.")
        val prestamo = prestamos.firstOrNull { it.id == b.idPrestamo } ?: throw ErrorDemo(404, "El préstamo no existe.")
        if (prestamo.estado != "ACTIVO") throw ErrorDemo(409, "El préstamo #${prestamo.id} no está ACTIVO.")
        for (item in b.items) {
            val detalle = prestamo.detalles.firstOrNull { it.idEquipo == item.idEquipo && it.estado == "PENDIENTE" }
                ?: throw ErrorDemo(409, "No se pudo procesar la devolución. Verifique si el equipo ya fue devuelto.")
            val condicion = item.condicion.uppercase()
            detalle.estado = "DEVUELTO"
            equipos.first { it.id == item.idEquipo }.estado = if (condicion == "DANADO") "DANADO" else "DISPONIBLE"
            auditar(u.id, "EQUIPO_DEVUELTO", "Equipo", "Equipo devuelto en condición $condicion (préstamo ${prestamo.id}).")
        }
        val pendientes = prestamo.detalles.count { it.estado == "PENDIENTE" }
        if (pendientes == 0) prestamo.estado = "FINALIZADO"
        return ok(DevolucionResponse(pendientes == 0, pendientes))
    }

    // ------------------------------------------------------------------ mantenimiento

    private fun ingresarMantenimiento(r: Request): Pair<Int, String> {
        val u = exigirRol(r, *OPERATIVOS)
        val b = leer<IngresoMantenimientoRequest>(r)
        val tipo = b.tipo.uppercase()
        if (b.descripcion.isBlank()) throw ErrorDemo(422, "Debe ingresar una descripción del problema o mantenimiento.")
        if (b.responsable.isBlank()) throw ErrorDemo(422, "Debe indicar el nombre del técnico o responsable.")
        val eq = equipos.firstOrNull { it.id == b.idEquipo } ?: throw ErrorDemo(404, "El equipo seleccionado no existe.")
        when (eq.estado) {
            "PRESTADO" -> throw ErrorDemo(409, "Un equipo PRESTADO no puede enviarse a mantenimiento. Debe devolverse primero.")
            "BAJA" -> throw ErrorDemo(409, "Un equipo en BAJA ya no puede ingresar a mantenimiento.")
            "MANTENIMIENTO" -> throw ErrorDemo(409, "El equipo ya se encuentra en mantenimiento actualmente.")
        }
        if (eq.estado != (if (tipo == "PREVENTIVO") "DISPONIBLE" else "DANADO")) {
            throw ErrorDemo(409, "Un mantenimiento PREVENTIVO es para equipos DISPONIBLES; uno CORRECTIVO, para equipos DAÑADOS.")
        }
        val m = Mant(siguienteId(mantenimientos.map { it.id }), eq.id, tipo, ahora, b.descripcion.trim(), b.responsable.trim())
        mantenimientos += m
        eq.estado = "MANTENIMIENTO"
        auditar(u.id, "EQUIPO_ENVIADO_MANTENIMIENTO", "Mantenimiento",
            "Equipo ID ${eq.id} enviado a mantenimiento $tipo. Responsable: ${m.responsable}")
        return 204 to ""
    }

    private fun finalizarMantenimiento(r: Request, id: Int): Pair<Int, String> {
        val u = exigirRol(r, *OPERATIVOS)
        val b = leer<FinalizarMantenimientoRequest>(r)
        val m = mantenimientos.firstOrNull { it.id == id } ?: throw ErrorDemo(404, "El mantenimiento seleccionado no existe.")
        if (m.estado != "EN_PROCESO") throw ErrorDemo(409, "Este mantenimiento ya fue finalizado anteriormente.")
        val resultado = b.resultado.uppercase()
        val nuevoEstado = if (resultado == "REPARADO") "DISPONIBLE" else "BAJA"
        m.salida = ahora
        m.resultado = resultado
        m.estado = "FINALIZADO"
        m.observaciones = b.observaciones?.trim()?.ifEmpty { null }
        equipos.first { it.id == m.idEquipo }.estado = nuevoEstado
        auditar(u.id, "MANTENIMIENTO_FINALIZADO", "Mantenimiento",
            "Mantenimiento finalizado como $resultado. Equipo ID ${m.idEquipo} pasa a $nuevoEstado")
        return 204 to ""
    }

    // ----------------------------------------------------------------------- reportes

    private fun historialMantenimiento(r: Request): Pair<Int, String> {
        exigirRol(r, *OPERATIVOS)
        val codigo = r.url.queryParameter("codigo")?.trim().orEmpty()
        val lista = mantenimientos
            .filter { m -> codigo.isEmpty() || equipos.first { it.id == m.idEquipo }.codigo.contains(codigo, ignoreCase = true) }
            .sortedByDescending { it.ingreso }
        return ok(lista.map(::mantDto))
    }

    private fun consultarAuditoria(r: Request): Pair<Int, String> {
        exigirRol(r, "ADMIN")
        val desde = LocalDate.parse(r.url.queryParameter("desde") ?: throw ErrorDemo(422, "Falta la fecha desde"))
        val hasta = LocalDate.parse(r.url.queryParameter("hasta") ?: throw ErrorDemo(422, "Falta la fecha hasta"))
        val accion = r.url.queryParameter("accion")
        val limite = r.url.queryParameter("limite")?.toIntOrNull() ?: 200
        val lista = auditoria
            .filter { !it.fecha.toLocalDate().isBefore(desde) && !it.fecha.toLocalDate().isAfter(hasta) }
            .filter { accion == null || it.accion == accion }
            .sortedByDescending { it.fecha }
            .take(limite)
            .map { a -> RegistroAuditoriaDto(a.id, a.fecha.toString(), a.accion, a.entidad, usuarios.firstOrNull { it.id == a.idUsuario }?.usuario, a.descripcion) }
        return ok(lista)
    }

    // ----------------------------------------------------------------------- usuarios

    private fun crearUsuario(r: Request): Pair<Int, String> {
        exigirRol(r, "ADMIN")
        val b = leer<CrearUsuarioRequest>(r)
        // Mismos RAISERROR del procedimiento CrearUsuario.
        if (usuarios.any { it.usuario.equals(b.nombreUsuario, ignoreCase = true) }) throw ErrorDemo(409, "el nombre de usuario ya existe")
        if (usuarios.any { it.correo.equals(b.correo, ignoreCase = true) }) throw ErrorDemo(409, "el correo ya tiene un usuario registrado")
        val nuevo = Usr(siguienteId(usuarios.map { it.id }), b.nombre, b.apellido, b.nombreUsuario, b.correo, b.contrasena, b.rol.uppercase(), "Activo")
        usuarios += nuevo
        return 201 to gson.toJson(CrearUsuarioResponse(nuevo.id))
    }

    // ---------------------------------------------------------------- utilidades

    private fun usuarioDelToken(r: Request, tipo: String): Usr {
        val token = r.header("Authorization")?.removePrefix("Bearer ")?.trim() ?: throw ErrorDemo(401, "Falta el token")
        val id = token.takeIf { it.startsWith("demo-$tipo-") }?.removePrefix("demo-$tipo-")?.toIntOrNull()
        return usuarios.firstOrNull { it.id == id } ?: throw ErrorDemo(401, "Token inválido o expirado")
    }

    private fun exigirRol(r: Request, vararg roles: String): Usr {
        val u = usuarioDelToken(r, "acceso")
        if (u.rol !in roles) throw ErrorDemo(403, "Acceso Denegado para tu rol.")
        return u
    }

    private fun auditar(idUsuario: Int?, accion: String, entidad: String, descripcion: String) {
        auditoria += Aud((auditoria.maxOfOrNull { it.id } ?: 0L) + 1, ahora, idUsuario, accion, entidad, descripcion)
    }

    private fun metricas(): DashboardMetricasDto {
        val conteo = equipos.groupingBy { it.estado }.eachCount()
        val activos = prestamos.filter { it.estado == "ACTIVO" }
        return DashboardMetricasDto(
            totalEquipos = equipos.size,
            disponibles = conteo["DISPONIBLE"] ?: 0,
            prestados = conteo["PRESTADO"] ?: 0,
            danados = conteo["DANADO"] ?: 0,
            enMantenimiento = conteo["MANTENIMIENTO"] ?: 0,
            deBaja = conteo["BAJA"] ?: 0,
            prestamosActivos = activos.size,
            prestamosVencidos = activos.count { p -> p.esperada.isBefore(ahora) && p.detalles.any { it.estado == "PENDIENTE" } },
        )
    }

    private fun labDto(l: Lab) = LaboratorioDto(l.id, l.codigo, l.nombre, l.ubicacion, l.capacidad, l.estado)

    private fun equipoDto(e: Eq) = EquipoDto(
        e.id, e.codigo, e.nombre, e.idTipo, tipos.firstOrNull { it.id == e.idTipo }?.nombre, e.idLab,
        laboratorios.firstOrNull { it.id == e.idLab }?.nombre, e.estado, e.marca, e.modelo, e.serie,
    )

    private fun prestamoDto(p: Pres) = PrestamoDto(
        p.id, p.solicitante, p.fecha.toString(), p.esperada.toString(), p.estado, p.observaciones,
        p.detalles.map { d ->
            val eq = equipos.first { it.id == d.idEquipo }
            DetallePrestamoDto(eq.id, eq.codigo, eq.nombre, d.estado == "DEVUELTO")
        },
    )

    private fun mantDto(m: Mant): MantenimientoDto {
        val eq = equipos.first { it.id == m.idEquipo }
        return MantenimientoDto(
            m.id, eq.id, eq.codigo, eq.nombre, m.tipo, m.ingreso.toString(), m.descripcion, m.responsable,
            m.salida?.toString(), m.resultado, m.observaciones,
        )
    }

    private fun usuarioDto(u: Usr) = UsuarioDto(u.id, u.nombre, u.apellido, u.usuario, u.correo, u.rol, u.estado)

    private fun siguienteId(ids: List<Int>) = (ids.maxOrNull() ?: 0) + 1

    private inline fun <reified T> leer(r: Request): T {
        val buffer = Buffer()
        r.body?.writeTo(buffer)
        return gson.fromJson(buffer.readUtf8(), T::class.java)
    }

    private fun ok(cuerpo: Any) = 200 to gson.toJson(cuerpo)

    private fun respuesta(r: Request, codigo: Int, cuerpo: String): Response =
        Response.Builder()
            .request(r)
            .protocol(Protocol.HTTP_1_1)
            .code(codigo)
            .message(if (codigo < 300) "OK" else "Error")
            .body(cuerpo.toResponseBody("application/json".toMediaType()))
            .build()

    // --------------------------------------------------------------- datos de ejemplo

    private fun sembrar() {
        val hoy = ahora
        laboratorios += listOf(
            Lab(1, "LAB-00001", "Laboratorio de Redes", "Edificio A, segundo nivel", 25, "ACTIVO"),
            Lab(2, "LAB-00002", "Laboratorio de Electrónica", "Edificio B, primer nivel", 20, "ACTIVO"),
            Lab(3, "LAB-00003", "Laboratorio de Cómputo", "Edificio C, tercer nivel", 30, "ACTIVO"),
        )
        // Los mismos tipos que inserta tablas.sql.
        tipos += listOf(
            Tipo(1, "Laptop", "Computadoras portatiles"),
            Tipo(2, "Camara", "Camaras utilizadas en laboratorios"),
            Tipo(3, "Proyector", "Proyectores multimedia"),
            Tipo(4, "Router", "Equipos de comunicacion de red"),
            Tipo(5, "Switch", "Equipos de conmutacion de red"),
            Tipo(6, "Tablet", "Dispositivos tablet"),
            Tipo(7, "Monitor", "Monitores"),
            Tipo(8, "Kit Electronico", "Kits utilizados en practicas de electronica"),
        )
        equipos += listOf(
            Eq(1, "EQP00001", "Laptop Dell Latitude", 1, 3, "DISPONIBLE", "Dell", "Latitude 5440", "DL5440-0001"),
            Eq(2, "EQP00002", "Laptop HP ProBook", 1, 3, "PRESTADO", "HP", "ProBook 450 G9", "HP450-0002"),
            Eq(3, "EQP00003", "Proyector Epson", 3, 1, "DISPONIBLE", "Epson", "PowerLite X49", "EPX49-0003"),
            Eq(4, "EQP00004", "Router Cisco", 4, 1, "DISPONIBLE", "Cisco", "ISR 4331", "CIS4331-0004"),
            Eq(5, "EQP00005", "Switch Cisco Catalyst", 5, 1, "DANADO", "Cisco", "Catalyst 2960", "CAT2960-0005"),
            Eq(6, "EQP00006", "Cámara Logitech", 2, 2, "PRESTADO", "Logitech", "C920", "LOGC920-0006"),
            Eq(7, "EQP00007", "Tablet Samsung", 6, 3, "MANTENIMIENTO", "Samsung", "Galaxy Tab A8", "SMTA8-0007"),
            Eq(8, "EQP00008", "Monitor LG 24\"", 7, 3, "DISPONIBLE", "LG", "24MK430H", "LG24-0008"),
            Eq(9, "EQP00009", "Kit Arduino Uno", 8, 2, "DISPONIBLE", "Arduino", "Uno R3", "ARD-0009"),
            Eq(10, "EQP00010", "Kit Raspberry Pi", 8, 2, "DANADO", "Raspberry", "Pi 4 Model B", "RPI4-0010"),
            Eq(11, "EQP00011", "Laptop Lenovo ThinkPad", 1, 3, "BAJA", "Lenovo", "ThinkPad E14", "LNE14-0011"),
            Eq(12, "EQP00012", "Proyector BenQ", 3, 2, "PRESTADO", "BenQ", "MW560", "BQ560-0012"),
        )
        prestamos += listOf(
            // Vencido hace 2 días: aparece en el dashboard y en Reportes.
            Pres(1, "Ana Pérez (Carné 1190-21-123)", hoy.minusDays(5), hoy.minusDays(2), "ACTIVO", "Práctica de redes, grupo 2",
                mutableListOf(Det(2, "PENDIENTE"), Det(6, "PENDIENTE"))),
            // Con un equipo ya devuelto: la devolución restante lo finaliza.
            Pres(2, "Ing. Carlos Méndez", hoy.minusDays(1), hoy.plusDays(3), "ACTIVO", "Clase de proyectos finales",
                mutableListOf(Det(12, "PENDIENTE"), Det(9, "DEVUELTO"))),
            Pres(3, "María López", hoy.minusDays(12), hoy.minusDays(10), "FINALIZADO", "Exposición de seminario",
                mutableListOf(Det(3, "DEVUELTO"))),
        )
        mantenimientos += listOf(
            Mant(1, 7, "CORRECTIVO", hoy.minusDays(3), "La pantalla no responde al tacto", "Luis Herrera"),
            Mant(2, 11, "CORRECTIVO", hoy.minusDays(20), "Placa madre dañada", "Luis Herrera",
                salida = hoy.minusDays(15), resultado = "NO_REPARABLE", estado = "FINALIZADO",
                observaciones = "El costo de reparación supera el valor del equipo"),
            Mant(3, 1, "PREVENTIVO", hoy.minusDays(30), "Limpieza interna y actualización", "Sofía Ramírez",
                salida = hoy.minusDays(29), resultado = "REPARADO", estado = "FINALIZADO"),
        )
        usuarios += listOf(
            Usr(1, "Administrador", "Demo", "admin.demo", "admin@demo.edu", CONTRASENA, "ADMIN", "ACTIVO"),
            Usr(2, "Elena", "Encargada", "encargado.demo", "encargado@demo.edu", CONTRASENA, "ENCARGADO", "ACTIVO"),
            Usr(3, "Diego", "Docente", "docente.demo", "docente@demo.edu", CONTRASENA, "DOCENTE", "ACTIVO"),
            Usr(4, "Mario", "Inactivo", "inactivo.demo", "inactivo@demo.edu", CONTRASENA, "ENCARGADO", "INACTIVO"),
        )
        auditar(2, "PRESTAMO_CREADO", "Prestamo", "Préstamo a Ana Pérez (Carné 1190-21-123) con 2 equipo(s).")
        auditar(null, "LOGIN_FALLIDO", "Usuario", "Intento de login fallido para 'inactivo.demo'. Motivo: Usuario/contraseña incorrectos o usuario inactivo")
        auditar(2, "EQUIPO_ENVIADO_MANTENIMIENTO", "Mantenimiento", "Equipo ID 7 enviado a mantenimiento CORRECTIVO. Responsable: Luis Herrera")
    }

    companion object {
        const val CONTRASENA = "Demo1234"
        val USUARIOS_DE_EJEMPLO = listOf("admin.demo · ADMIN", "encargado.demo · ENCARGADO", "docente.demo · DOCENTE")
        private val OPERATIVOS = arrayOf("ADMIN", "ENCARGADO")
        private val TODOS = arrayOf("ADMIN", "ENCARGADO", "DOCENTE")
    }
}
