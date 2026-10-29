package com.universidad.sigelab.domain.rules

import com.universidad.sigelab.domain.model.CondicionDevolucion
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.EstadoEquipo.BAJA
import com.universidad.sigelab.domain.model.EstadoEquipo.DANADO
import com.universidad.sigelab.domain.model.EstadoEquipo.DISPONIBLE
import com.universidad.sigelab.domain.model.EstadoEquipo.MANTENIMIENTO
import com.universidad.sigelab.domain.model.EstadoEquipo.PRESTADO
import com.universidad.sigelab.domain.model.ResultadoMantenimiento

/**
 * Ciclo de vida de un equipo (diagrama "Flujo principal del ciclo de un equipo"):
 *
 *   DISPONIBLE ─préstamo→ PRESTADO ─devolución→ DISPONIBLE | DANADO
 *   DANADO / DISPONIBLE (revisión) ─→ MANTENIMIENTO ─resultado→ DISPONIBLE | BAJA
 *
 * Leer el QR de un equipo no es una transición: no cambia el estado.
 */
object ReglasEquipo {

    private val transiciones: Map<EstadoEquipo, Set<EstadoEquipo>> = mapOf(
        DISPONIBLE to setOf(PRESTADO, MANTENIMIENTO),
        PRESTADO to setOf(DISPONIBLE, DANADO),
        DANADO to setOf(MANTENIMIENTO),
        MANTENIMIENTO to setOf(DISPONIBLE, BAJA),
        BAJA to emptySet(),
    )

    fun puedeCambiar(de: EstadoEquipo, a: EstadoEquipo): Boolean = a in transiciones.getValue(de)

    fun puedePrestarse(estado: EstadoEquipo) = estado == DISPONIBLE

    /** Entra a mantenimiento un equipo DAÑADO o uno DISPONIBLE que se manda a revisión. */
    fun puedeIngresarAMantenimiento(estado: EstadoEquipo) = puedeCambiar(estado, MANTENIMIENTO)

    fun estadoTrasDevolucion(condicion: CondicionDevolucion): EstadoEquipo = when (condicion) {
        CondicionDevolucion.BUENO -> DISPONIBLE
        CondicionDevolucion.DANADO -> DANADO
    }

    fun estadoTrasMantenimiento(resultado: ResultadoMantenimiento): EstadoEquipo = when (resultado) {
        ResultadoMantenimiento.REPARADO -> DISPONIBLE
        ResultadoMantenimiento.NO_REPARABLE -> BAJA
    }
}
