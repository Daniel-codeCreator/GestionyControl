package com.universidad.sigelab.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

// --- Auditoría de acciones ---

/**
 * Acciones que registra escritorio en la tabla Auditoria (AuthService, PrestamoRepository y
 * MantenimientoRepository). El servidor puede enviar otras y se muestran con su código.
 */
enum class AccionAuditoria(val etiqueta: String) {
    LOGIN_FALLIDO("Login fallido"),
    LOGIN_EXITOSO("Login exitoso"),
    PRESTAMO_CREADO("Préstamo creado"),
    EQUIPO_DEVUELTO("Equipo devuelto"),
    EQUIPO_ENVIADO_MANTENIMIENTO("Enviado a mantenimiento"),
    MANTENIMIENTO_FINALIZADO("Mantenimiento finalizado");

    companion object {
        fun etiquetaDe(codigo: String): String = entries.firstOrNull { it.name == codigo }?.etiqueta ?: codigo
    }
}

data class RegistroAuditoria(
    val id: Long,
    val fecha: LocalDateTime,
    val accion: String,
    val usuario: String?,
    /** Columna Entidad: Usuario, Prestamo, Equipo, Mantenimiento. */
    val entidad: String?,
    val detalle: String,
)

/** Máximo de registros por consulta; la pantalla avisa cuando se alcanza. */
const val LIMITE_AUDITORIA = 200

enum class PeriodoAuditoria(val etiqueta: String, private val diasAtras: Long) {
    HOY("Hoy", 0),
    SIETE_DIAS("7 días", 6),
    TREINTA_DIAS("30 días", 29);

    /** Rango de fechas inclusivo que termina hoy. */
    fun rango(hoy: LocalDate): Pair<LocalDate, LocalDate> = hoy.minusDays(diasAtras) to hoy
}

data class FiltroAuditoria(
    val desde: LocalDate,
    val hasta: LocalDate,
    val accion: String?,
)
