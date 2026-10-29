package com.universidad.sigelab.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Mantenimiento.Tipo. Como en la pantalla de escritorio, el tipo decide qué equipos se pueden
 * ingresar: PREVENTIVO lista los DISPONIBLES y CORRECTIVO los DAÑADOS.
 */
enum class TipoMantenimiento(val etiqueta: String, val estadoRequerido: EstadoEquipo) {
    CORRECTIVO("Correctivo", EstadoEquipo.DANADO),
    PREVENTIVO("Preventivo", EstadoEquipo.DISPONIBLE);

    companion object {
        fun desde(valor: String): TipoMantenimiento =
            entries.firstOrNull { it.name.equals(valor.trim(), ignoreCase = true) } ?: CORRECTIVO
    }
}

enum class ResultadoMantenimiento(val etiqueta: String) {
    REPARADO("Reparado"),
    NO_REPARABLE("No reparable"),
}

/** Tabla Mantenimiento con el código y nombre del equipo, como la consulta de escritorio. */
data class Mantenimiento(
    val id: Int,
    val idEquipo: Int,
    val codigoInventario: String,
    val nombreEquipo: String,
    val tipo: TipoMantenimiento,
    val fechaIngreso: LocalDateTime,
    val descripcion: String,
    /** Técnico o responsable. */
    val responsable: String,
    /** null mientras el equipo sigue EN_PROCESO. */
    val fechaSalida: LocalDateTime? = null,
    val resultado: ResultadoMantenimiento? = null,
    val observaciones: String? = null,
) {
    val enProceso: Boolean get() = fechaSalida == null && resultado == null

    fun diasEnTaller(hoy: LocalDate): Long = ChronoUnit.DAYS.between(fechaIngreso.toLocalDate(), hoy)
}

data class NuevoMantenimiento(
    val idEquipo: Int,
    val tipo: TipoMantenimiento,
    val descripcion: String,
    val responsable: String,
)
