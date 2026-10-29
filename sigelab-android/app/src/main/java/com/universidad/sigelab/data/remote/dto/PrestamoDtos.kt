package com.universidad.sigelab.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DetallePrestamoDto(
    @SerializedName("id_equipo") val idEquipo: Int,
    @SerializedName("codigo_inventario") val codigoInventario: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("devuelto") val devuelto: Boolean,
)

data class PrestamoDto(
    @SerializedName("id_prestamo") val idPrestamo: Int,
    @SerializedName("solicitante") val solicitante: String,
    @SerializedName("fecha_prestamo") val fechaPrestamo: String,
    @SerializedName("fecha_devolucion_prevista") val fechaDevolucionPrevista: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("observaciones") val observaciones: String?,
    @SerializedName("detalles") val detalles: List<DetallePrestamoDto>,
)

data class CrearPrestamoRequest(
    @SerializedName("solicitante") val solicitante: String,
    @SerializedName("ids_equipos") val idsEquipos: List<Int>,
    @SerializedName("fecha_devolucion_prevista") val fechaDevolucionPrevista: String,
    @SerializedName("observaciones") val observaciones: String,
)

data class CrearPrestamoResponse(
    @SerializedName("id_prestamo") val idPrestamo: Int,
)

data class ItemDevolucionDto(
    @SerializedName("id_equipo") val idEquipo: Int,
    @SerializedName("condicion") val condicion: String,
    @SerializedName("observacion") val observacion: String?,
)

data class DevolucionRequest(
    @SerializedName("id_prestamo") val idPrestamo: Int,
    @SerializedName("items") val items: List<ItemDevolucionDto>,
)

data class DevolucionResponse(
    @SerializedName("prestamo_finalizado") val prestamoFinalizado: Boolean,
    @SerializedName("equipos_pendientes") val equiposPendientes: Int,
)

/** Columnas de MantenimientoRepository.buscarPorId (escritorio). */
data class MantenimientoDto(
    @SerializedName("id_mantenimiento") val idMantenimiento: Int,
    @SerializedName("id_equipo") val idEquipo: Int,
    @SerializedName("codigo_inventario") val codigoInventario: String?,
    @SerializedName("nombre_equipo") val nombreEquipo: String?,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("fecha_ingreso") val fechaIngreso: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("responsable") val responsable: String,
    @SerializedName("fecha_salida") val fechaSalida: String?,
    @SerializedName("resultado") val resultado: String?,
    @SerializedName("observaciones") val observaciones: String?,
)

data class IngresoMantenimientoRequest(
    @SerializedName("id_equipo") val idEquipo: Int,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("responsable") val responsable: String,
)

data class FinalizarMantenimientoRequest(
    @SerializedName("resultado") val resultado: String,
    @SerializedName("observaciones") val observaciones: String?,
)
