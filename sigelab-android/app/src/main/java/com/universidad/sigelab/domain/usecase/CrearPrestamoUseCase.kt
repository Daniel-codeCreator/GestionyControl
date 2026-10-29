package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.NuevoPrestamo
import com.universidad.sigelab.domain.repository.PrestamoRepository
import com.universidad.sigelab.domain.rules.ReglasEquipo
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Mensajes de PrestamoService.realizarPrestamoMultiple (escritorio). */
sealed interface ErrorPrestamo {
    val mensaje: String

    data object SinSolicitante : ErrorPrestamo {
        override val mensaje = "El nombre del solicitante es obligatorio."
    }

    data object SolicitanteMuyLargo : ErrorPrestamo {
        override val mensaje = "El solicitante admite máximo $LARGO_SOLICITANTE caracteres."
    }

    data object SinEquipos : ErrorPrestamo {
        override val mensaje = "Debe agregar al menos un equipo al préstamo."
    }

    data class EquiposDuplicados(val codigos: List<String>) : ErrorPrestamo {
        override val mensaje = codigos.joinToString("\n") { "El equipo $it está duplicado en la selección." }
    }

    data class EquiposNoDisponibles(val detalle: List<String>) : ErrorPrestamo {
        override val mensaje = detalle.joinToString("\n")
    }

    data object FechaDevolucionInvalida : ErrorPrestamo {
        override val mensaje = "La fecha de devolución no puede ser menor a la de hoy."
    }

    data object SinObservaciones : ErrorPrestamo {
        override val mensaje = "Las observaciones del préstamo son obligatorias."
    }

    data object ObservacionesMuyLargas : ErrorPrestamo {
        override val mensaje = "Las observaciones admiten máximo $LARGO_OBSERVACIONES caracteres."
    }
}

private const val LARGO_SOLICITANTE = 150
private const val LARGO_OBSERVACIONES = 500

class CrearPrestamoUseCase @Inject constructor(
    private val repo: PrestamoRepository,
    private val reloj: Clock,
) {
    /** RN-05 (al menos un equipo), RN-06 (sin duplicados), RN-07/08 (todos DISPONIBLES), fecha y observaciones. */
    fun validar(
        solicitante: String,
        equipos: List<Equipo>,
        fechaDevolucion: LocalDate,
        observaciones: String,
    ): List<ErrorPrestamo> = buildList {
        when {
            solicitante.isBlank() -> add(ErrorPrestamo.SinSolicitante)
            solicitante.trim().length > LARGO_SOLICITANTE -> add(ErrorPrestamo.SolicitanteMuyLargo)
        }
        if (equipos.isEmpty()) add(ErrorPrestamo.SinEquipos)

        val duplicados = equipos.groupBy { it.id }.filterValues { it.size > 1 }.values.map { it.first().codigoInventario }
        if (duplicados.isNotEmpty()) add(ErrorPrestamo.EquiposDuplicados(duplicados))

        val noDisponibles = equipos.distinctBy { it.id }.filterNot { ReglasEquipo.puedePrestarse(it.estado) }
        if (noDisponibles.isNotEmpty()) {
            add(
                ErrorPrestamo.EquiposNoDisponibles(
                    noDisponibles.map {
                        "El equipo ${it.codigoInventario} (${it.nombre}) no está disponible. Estado actual: ${it.estado.etiqueta}"
                    },
                ),
            )
        }

        if (fechaDevolucion.isBefore(LocalDate.now(reloj))) add(ErrorPrestamo.FechaDevolucionInvalida)
        when {
            observaciones.isBlank() -> add(ErrorPrestamo.SinObservaciones)
            observaciones.trim().length > LARGO_OBSERVACIONES -> add(ErrorPrestamo.ObservacionesMuyLargas)
        }
    }

    suspend operator fun invoke(
        solicitante: String,
        equipos: List<Equipo>,
        fechaDevolucion: LocalDate,
        observaciones: String,
    ): Result<Int> {
        val errores = validar(solicitante, equipos, fechaDevolucion, observaciones)
        if (errores.isNotEmpty()) return Result.failure(AppError.Validacion(errores.map { it.mensaje }))
        return repo.crear(
            NuevoPrestamo(
                solicitante = solicitante.trim(),
                idsEquipos = equipos.map { it.id },
                fechaDevolucionPrevista = fechaDevolucion.atTime(HORA_LIMITE),
                observaciones = observaciones.trim(),
            ),
        )
    }

    companion object {
        /**
         * "Fecha Fin Estimada" se elige por día; vence al final de ese día. Así un préstamo para hoy
         * cumple CK_Prestamo_Fechas (FechaEsperadaDevolucion >= FechaPrestamo).
         */
        val HORA_LIMITE: LocalTime = LocalTime.of(23, 59)
    }
}
