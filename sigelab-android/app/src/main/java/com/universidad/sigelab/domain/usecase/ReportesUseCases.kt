package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.DetallePrestamo
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.FiltroAuditoria
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.PeriodoAuditoria
import com.universidad.sigelab.domain.model.Prestamo
import com.universidad.sigelab.domain.model.RegistroAuditoria
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.repository.ReporteRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// Los tres reportes de escritorio (ReportesView): equipos por estado, préstamos vencidos e
// historial de mantenimiento. La auditoría es un agregado de la app, solo para ADMIN.

// --- Equipos por estado ---

/** Conteos para el filtro "Estado:" (TODOS y cada estado). */
data class ResumenInventario(
    val total: Int = 0,
    val conteos: Map<EstadoEquipo, Int> = emptyMap(),
)

fun resumirInventario(equipos: List<Equipo>) = ResumenInventario(
    total = equipos.size,
    conteos = equipos.groupingBy { it.estado }.eachCount(),
)

/** "Buscar" de escritorio: estado null = TODOS. */
fun equiposPorEstado(equipos: List<Equipo>, estado: EstadoEquipo?): List<Equipo> =
    equipos.filter { estado == null || it.estado == estado }.sortedBy { it.codigoInventario }

// --- Préstamos vencidos ---

data class PrestamoVencido(
    val prestamo: Prestamo,
    /** 0 = venció hoy. */
    val diasDeAtraso: Long,
) {
    val pendientes: List<DetallePrestamo> get() = prestamo.pendientes
}

/** Préstamos ACTIVOS cuya fecha de devolución ya pasó, del más atrasado al menos atrasado. */
fun prestamosVencidos(prestamos: List<Prestamo>, ahora: LocalDateTime): List<PrestamoVencido> =
    prestamos
        .filter { it.estaVencido(ahora) }
        .sortedBy { it.fechaDevolucionPrevista }
        .map { PrestamoVencido(it, ChronoUnit.DAYS.between(it.fechaDevolucionPrevista.toLocalDate(), ahora.toLocalDate())) }

// --- Historial de mantenimiento y auditoría ---

class ConsultarHistorialMantenimientoUseCase @Inject constructor(
    private val repo: ReporteRepository,
) {
    /** Código vacío = todos los mantenimientos, como en escritorio. */
    suspend operator fun invoke(rolActual: Rol, codigoInventario: String): Result<List<Mantenimiento>> {
        if (!rolActual.puedeVer(Modulo.REPORTES)) return Result.failure(AppError.SinPermiso())
        return repo.historialMantenimiento(codigoInventario.trim().ifEmpty { null })
            .map { lista -> lista.sortedByDescending { it.fechaIngreso } }
    }
}

class ConsultarAuditoriaUseCase @Inject constructor(
    private val repo: ReporteRepository,
    private val reloj: Clock,
) {
    fun filtro(periodo: PeriodoAuditoria, accion: String?): FiltroAuditoria {
        val (desde, hasta) = periodo.rango(LocalDate.now(reloj))
        return FiltroAuditoria(desde, hasta, accion)
    }

    suspend operator fun invoke(rolActual: Rol, periodo: PeriodoAuditoria, accion: String?): Result<List<RegistroAuditoria>> {
        if (!rolActual.veAuditoria) return Result.failure(AppError.SinPermiso())
        return repo.auditoria(filtro(periodo, accion)).map { registros -> registros.sortedByDescending { it.fecha } }
    }
}
