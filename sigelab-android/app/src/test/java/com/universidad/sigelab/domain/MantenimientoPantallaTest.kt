package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.TipoMantenimiento
import com.universidad.sigelab.domain.usecase.AccionQrMantenimiento
import com.universidad.sigelab.domain.usecase.accionParaEquipoEscaneado
import com.universidad.sigelab.domain.usecase.candidatosParaMantenimiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MantenimientoPantallaTest {

    private val inventario = listOf(
        equipo(1, EstadoEquipo.DANADO),
        equipo(2, EstadoEquipo.DISPONIBLE),
        equipo(3, EstadoEquipo.PRESTADO),
        equipo(4, EstadoEquipo.MANTENIMIENTO),
        equipo(5, EstadoEquipo.BAJA),
        equipo(6, EstadoEquipo.DANADO),
    )

    private val enTaller = mantenimiento(30, idEquipo = 4)

    @Test
    fun `el tipo decide el equipo afectado como en escritorio`() {
        assertEquals(listOf(1, 6), candidatosParaMantenimiento(inventario, TipoMantenimiento.CORRECTIVO).map { it.id })
        assertEquals(listOf(2), candidatosParaMantenimiento(inventario, TipoMantenimiento.PREVENTIVO).map { it.id })
        assertEquals(listOf(6), candidatosParaMantenimiento(inventario, TipoMantenimiento.CORRECTIVO, "00006").map { it.id })
    }

    @Test
    fun `escanear elige el tipo o el cierre segun el estado del equipo`() {
        val enProceso = listOf(enTaller)

        assertEquals(
            AccionQrMantenimiento.Ingresar(inventario[0], TipoMantenimiento.CORRECTIVO),
            accionParaEquipoEscaneado(inventario[0], enProceso),
        )
        assertEquals(
            AccionQrMantenimiento.Ingresar(inventario[1], TipoMantenimiento.PREVENTIVO),
            accionParaEquipoEscaneado(inventario[1], enProceso),
        )
        assertEquals(AccionQrMantenimiento.Finalizar(enTaller), accionParaEquipoEscaneado(inventario[3], enProceso))

        val prestado = accionParaEquipoEscaneado(inventario[2], enProceso)
        assertTrue(prestado is AccionQrMantenimiento.NoAplica && prestado.mensaje.contains("Debe devolverse primero"))
        assertTrue(accionParaEquipoEscaneado(inventario[4], enProceso) is AccionQrMantenimiento.NoAplica)
        // En mantenimiento según la caché, pero la lista de escritorio está desactualizada.
        assertTrue(accionParaEquipoEscaneado(inventario[3], emptyList()) is AccionQrMantenimiento.NoAplica)
    }

    @Test
    fun `dias en taller se cuentan por fecha calendario`() {
        assertEquals(0L, enTaller.diasEnTaller(LocalDate.of(2026, 10, 20)))
        assertEquals(7L, enTaller.diasEnTaller(LocalDate.of(2026, 10, 27)))
    }
}
