package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.NuevoMantenimiento
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.TipoMantenimiento
import com.universidad.sigelab.domain.repository.MantenimientoRepository
import com.universidad.sigelab.domain.rules.ReglasEquipo
import javax.inject.Inject

// Largos de las columnas en SQL Server.
private const val LARGO_DESCRIPCION = 500
private const val LARGO_RESPONSABLE = 150
private const val LARGO_OBSERVACIONES = 500

/** Reglas y mensajes de MantenimientoService.ingresarAMantenimiento (escritorio). */
class IngresarMantenimientoUseCase @Inject constructor(
    private val repo: MantenimientoRepository,
) {
    fun validar(equipo: Equipo?, tipo: TipoMantenimiento, descripcion: String, responsable: String): List<String> = buildList {
        when (equipo?.estado) {
            null -> add("Debe seleccionar un equipo.")
            EstadoEquipo.PRESTADO -> add("Un equipo PRESTADO no puede enviarse a mantenimiento. Debe devolverse primero.")
            EstadoEquipo.BAJA -> add("Un equipo en BAJA ya no puede ingresar a mantenimiento.")
            EstadoEquipo.MANTENIMIENTO -> add("El equipo ya se encuentra en mantenimiento actualmente.")
            // MantenimientoView solo ofrece DISPONIBLES para PREVENTIVO y DAÑADOS para CORRECTIVO.
            EstadoEquipo.DISPONIBLE, EstadoEquipo.DANADO -> if (equipo.estado != tipo.estadoRequerido) {
                add("Un mantenimiento PREVENTIVO es para equipos DISPONIBLES; uno CORRECTIVO, para equipos DAÑADOS.")
            }
        }
        when {
            descripcion.isBlank() -> add("Debe ingresar una descripción del problema o mantenimiento.")
            descripcion.trim().length > LARGO_DESCRIPCION -> add("La descripción admite máximo $LARGO_DESCRIPCION caracteres.")
        }
        when {
            responsable.isBlank() -> add("Debe indicar el nombre del técnico o responsable.")
            responsable.trim().length > LARGO_RESPONSABLE -> add("El responsable admite máximo $LARGO_RESPONSABLE caracteres.")
        }
    }

    suspend operator fun invoke(
        equipo: Equipo?,
        tipo: TipoMantenimiento,
        descripcion: String,
        responsable: String,
    ): Result<Unit> {
        val errores = validar(equipo, tipo, descripcion, responsable)
        if (errores.isNotEmpty() || equipo == null) return Result.failure(AppError.Validacion(errores))
        return repo.ingresar(NuevoMantenimiento(equipo.id, tipo, descripcion.trim(), responsable.trim()))
    }
}

/** Lista "Equipo Afectado" de escritorio: PREVENTIVO muestra los DISPONIBLES y CORRECTIVO los DAÑADOS. */
fun candidatosParaMantenimiento(equipos: List<Equipo>, tipo: TipoMantenimiento, texto: String = ""): List<Equipo> =
    filtrarEquipos(equipos, tipo.estadoRequerido, texto).sortedBy { it.codigoInventario }

/** Qué hacer al escanear un equipo desde la pantalla de Mantenimiento. */
sealed interface AccionQrMantenimiento {
    data class Ingresar(val equipo: Equipo, val tipo: TipoMantenimiento) : AccionQrMantenimiento
    data class Finalizar(val mantenimiento: Mantenimiento) : AccionQrMantenimiento
    data class NoAplica(val mensaje: String) : AccionQrMantenimiento
}

fun accionParaEquipoEscaneado(equipo: Equipo, enProceso: List<Mantenimiento>): AccionQrMantenimiento =
    when (equipo.estado) {
        EstadoEquipo.MANTENIMIENTO ->
            enProceso.firstOrNull { it.idEquipo == equipo.id }
                ?.let { AccionQrMantenimiento.Finalizar(it) }
                ?: AccionQrMantenimiento.NoAplica(
                    "${equipo.codigoInventario} figura en mantenimiento pero no aparece en Mantenimientos en Proceso. Actualice la lista.",
                )
        EstadoEquipo.DISPONIBLE -> AccionQrMantenimiento.Ingresar(equipo, TipoMantenimiento.PREVENTIVO)
        EstadoEquipo.DANADO -> AccionQrMantenimiento.Ingresar(equipo, TipoMantenimiento.CORRECTIVO)
        EstadoEquipo.PRESTADO ->
            AccionQrMantenimiento.NoAplica("Un equipo PRESTADO no puede enviarse a mantenimiento. Debe devolverse primero.")
        EstadoEquipo.BAJA -> AccionQrMantenimiento.NoAplica("Un equipo en BAJA ya no puede ingresar a mantenimiento.")
    }

/**
 * Reglas de MantenimientoService.cerrarMantenimiento: REPARADO → DISPONIBLE, NO_REPARABLE → BAJA.
 * Las observaciones finales son opcionales. Devuelve el estado final del equipo.
 */
class CerrarMantenimientoUseCase @Inject constructor(
    private val repo: MantenimientoRepository,
) {
    suspend operator fun invoke(
        mantenimiento: Mantenimiento,
        resultado: ResultadoMantenimiento,
        observaciones: String,
    ): Result<EstadoEquipo> {
        val errores = buildList {
            if (!mantenimiento.enProceso) add("Este mantenimiento ya fue finalizado anteriormente.")
            if (observaciones.trim().length > LARGO_OBSERVACIONES) {
                add("Las observaciones admiten máximo $LARGO_OBSERVACIONES caracteres.")
            }
        }
        if (errores.isNotEmpty()) return Result.failure(AppError.Validacion(errores))
        return repo.finalizar(mantenimiento.id, resultado, observaciones.trim().ifEmpty { null })
            .map { ReglasEquipo.estadoTrasMantenimiento(resultado) }
    }
}
