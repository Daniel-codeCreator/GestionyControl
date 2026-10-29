package com.universidad.sigelab.data

import com.universidad.sigelab.data.local.AuditoriaPendienteEntity
import com.universidad.sigelab.data.local.DashboardSnapshotEntity
import com.universidad.sigelab.data.local.EquipoEntity
import com.universidad.sigelab.data.remote.dto.DashboardMetricasDto
import com.universidad.sigelab.data.remote.dto.EquipoDto
import com.universidad.sigelab.data.remote.dto.EventoAuditoriaRequest
import com.universidad.sigelab.data.remote.dto.LaboratorioDto
import com.universidad.sigelab.data.remote.dto.LoginResponse
import com.universidad.sigelab.data.remote.dto.MantenimientoDto
import com.universidad.sigelab.data.remote.dto.PrestamoDto
import com.universidad.sigelab.data.remote.dto.TipoEquipoDto
import com.universidad.sigelab.data.remote.dto.UsuarioDto
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.DashboardMetricas
import com.universidad.sigelab.domain.model.DetallePrestamo
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.EstadoPrestamo
import com.universidad.sigelab.domain.model.EstadoRegistro
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.PreAutenticacion
import com.universidad.sigelab.domain.model.Prestamo
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.TipoEquipo
import com.universidad.sigelab.domain.model.TipoMantenimiento
import com.universidad.sigelab.domain.model.Usuario
import com.universidad.sigelab.domain.model.UsuarioRegistrado
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// --- Fechas: FastAPI serializa datetime en ISO-8601, con o sin zona horaria ---

internal fun parsearFecha(valor: String): LocalDateTime =
    runCatching { LocalDateTime.parse(valor) }.getOrElse {
        OffsetDateTime.parse(valor).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

internal fun LocalDateTime.aIso(): String = truncatedTo(ChronoUnit.SECONDS).toString()

// --- Auth y usuarios ---

internal fun LoginResponse.toDomain(): PreAutenticacion {
    val rolDominio = Rol.desde(rol) ?: throw AppError.Servidor(200, "Rol no reconocido: $rol")
    return PreAutenticacion(
        usuario = Usuario(idUsuario, nombreUsuario, nombreCompleto ?: nombreUsuario, rolDominio),
        token = preAuthToken,
    )
}

internal fun UsuarioDto.toDomain() = UsuarioRegistrado(
    id = idUsuario,
    nombreCompleto = "$nombre $apellido".trim(),
    nombreUsuario = nombreUsuario,
    correo = correo,
    rol = rol?.let(Rol::desde),
    estado = EstadoRegistro.desde(estado),
)

internal fun AuditoriaPendienteEntity.toRequest() = EventoAuditoriaRequest(
    accion = accion,
    nombreUsuario = nombreUsuario,
    idUsuario = idUsuario,
    detalle = detalle,
    fechaCliente = fechaCliente,
)

// --- Dashboard ---

internal fun DashboardMetricasDto.toEntity(ahora: Instant) = DashboardSnapshotEntity(
    totalEquipos = totalEquipos,
    disponibles = disponibles,
    prestados = prestados,
    danados = danados,
    enMantenimiento = enMantenimiento,
    deBaja = deBaja,
    prestamosActivos = prestamosActivos,
    prestamosVencidos = prestamosVencidos,
    actualizadoEnEpochMs = ahora.toEpochMilli(),
)

internal fun DashboardSnapshotEntity.toDomain() = DashboardMetricas(
    totalEquipos = totalEquipos,
    disponibles = disponibles,
    prestados = prestados,
    danados = danados,
    enMantenimiento = enMantenimiento,
    deBaja = deBaja,
    prestamosActivos = prestamosActivos,
    prestamosVencidos = prestamosVencidos,
    actualizadoEn = Instant.ofEpochMilli(actualizadoEnEpochMs),
)

// --- Catálogos ---

internal fun LaboratorioDto.toDomain() = Laboratorio(
    id = idLaboratorio ?: 0,
    codigo = codigo.orEmpty(),
    nombre = nombre,
    ubicacion = ubicacion.orEmpty(),
    capacidad = capacidad,
    estado = EstadoRegistro.desde(estado),
)

/** En el alta el código va null: lo genera el servidor (LAB-00001), igual que escritorio. */
internal fun Laboratorio.toDto() = LaboratorioDto(
    idLaboratorio = id.takeIf { it != 0 },
    codigo = codigo.trim().takeIf { id != 0 && it.isNotEmpty() },
    nombre = nombre.trim(),
    ubicacion = ubicacion.trim(),
    capacidad = capacidad,
    estado = estado.name,
)

internal fun TipoEquipoDto.toDomain() = TipoEquipo(idTipoEquipo ?: 0, nombre, descripcion)
internal fun TipoEquipo.toDto() = TipoEquipoDto(id.takeIf { it != 0 }, nombre.trim(), descripcion)

internal fun EquipoDto.toEntity() = EquipoEntity(
    id = idEquipo,
    codigoInventario = codigoInventario,
    nombre = nombre,
    idTipoEquipo = idTipoEquipo,
    tipo = tipo.orEmpty(),
    idLaboratorio = idLaboratorio,
    laboratorio = laboratorio.orEmpty(),
    estado = EstadoEquipo.desde(estado),
    marca = marca.orEmpty(),
    modelo = modelo.orEmpty(),
    numeroSerie = numeroSerie.orEmpty(),
)

internal fun EquipoEntity.toDomain() = Equipo(
    id = id,
    codigoInventario = codigoInventario,
    nombre = nombre,
    idTipoEquipo = idTipoEquipo,
    tipo = tipo,
    idLaboratorio = idLaboratorio,
    laboratorio = laboratorio,
    estado = estado,
    marca = marca,
    modelo = modelo,
    numeroSerie = numeroSerie,
)

// --- Préstamos y mantenimiento ---

internal fun PrestamoDto.toDomain() = Prestamo(
    id = idPrestamo,
    solicitante = solicitante,
    fechaPrestamo = parsearFecha(fechaPrestamo),
    fechaDevolucionPrevista = parsearFecha(fechaDevolucionPrevista),
    estado = EstadoPrestamo.valueOf(estado.trim().uppercase()),
    observaciones = observaciones,
    detalles = detalles.map { DetallePrestamo(it.idEquipo, it.codigoInventario, it.nombre, it.devuelto) },
)

internal fun MantenimientoDto.toDomain() = Mantenimiento(
    id = idMantenimiento,
    idEquipo = idEquipo,
    codigoInventario = codigoInventario.orEmpty(),
    nombreEquipo = nombreEquipo.orEmpty(),
    tipo = TipoMantenimiento.desde(tipo),
    fechaIngreso = parsearFecha(fechaIngreso),
    descripcion = descripcion,
    responsable = responsable,
    fechaSalida = fechaSalida?.let(::parsearFecha),
    resultado = resultado?.let { ResultadoMantenimiento.valueOf(it.trim().uppercase().replace(' ', '_')) },
    observaciones = observaciones,
)
