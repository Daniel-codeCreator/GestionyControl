package com.universidad.sigelab.domain.model

import java.time.LocalDateTime

enum class EstadoPrestamo { ACTIVO, FINALIZADO }

enum class CondicionDevolucion(val etiqueta: String) {
    BUENO("Bueno"),
    DANADO("Dañado"),
}

/** Fila de DetallePrestamo: PENDIENTE o DEVUELTO. */
data class DetallePrestamo(
    val idEquipo: Int,
    val codigoInventario: String,
    val nombreEquipo: String,
    val devuelto: Boolean,
)

/** Tabla Prestamo. El solicitante es texto libre ("Estudiante / Usuario" en escritorio). */
data class Prestamo(
    val id: Int,
    val solicitante: String,
    val fechaPrestamo: LocalDateTime,
    val fechaDevolucionPrevista: LocalDateTime,
    val estado: EstadoPrestamo,
    val observaciones: String?,
    val detalles: List<DetallePrestamo>,
) {
    val pendientes: List<DetallePrestamo> get() = detalles.filterNot { it.devuelto }

    /** Igual que el dashboard de escritorio: ACTIVO y con la fecha esperada ya pasada. */
    fun estaVencido(ahora: LocalDateTime) =
        estado == EstadoPrestamo.ACTIVO && ahora.isAfter(fechaDevolucionPrevista)
}

data class NuevoPrestamo(
    val solicitante: String,
    val idsEquipos: List<Int>,
    val fechaDevolucionPrevista: LocalDateTime,
    val observaciones: String,
)

data class ItemDevolucion(
    val idEquipo: Int,
    val condicion: CondicionDevolucion,
    val observacion: String? = null,
)

data class ResultadoDevolucion(
    val prestamoFinalizado: Boolean,
    val equiposPendientes: Int,
)
