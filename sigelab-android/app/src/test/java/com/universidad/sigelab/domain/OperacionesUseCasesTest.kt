package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.CondicionDevolucion
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.EstadoPrestamo
import com.universidad.sigelab.domain.model.ItemDevolucion
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.TipoMantenimiento
import com.universidad.sigelab.domain.usecase.CerrarMantenimientoUseCase
import com.universidad.sigelab.domain.usecase.CrearPrestamoUseCase
import com.universidad.sigelab.domain.usecase.ErrorPrestamo
import com.universidad.sigelab.domain.usecase.IngresarMantenimientoUseCase
import com.universidad.sigelab.domain.usecase.ProcesarDevolucionUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class OperacionesUseCasesTest {

    private val reloj = Clock.fixed(LocalDateTime.of(2026, 10, 26, 9, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val hoy = LocalDate.of(2026, 10, 26)

    // --- Préstamos ---

    @Test
    fun `prestamo valido se envia con todos los equipos y vence al final del dia elegido`() = runBlocking {
        val repo = FakePrestamoRepository()
        val resultado = CrearPrestamoUseCase(repo, reloj)(" Ana Pérez ", listOf(equipo(1), equipo(2)), hoy, " Práctica ")

        assertEquals(99, resultado.getOrThrow())
        val enviado = repo.creado!!
        assertEquals("Ana Pérez", enviado.solicitante)
        assertEquals(listOf(1, 2), enviado.idsEquipos)
        assertEquals("Práctica", enviado.observaciones)
        // Un préstamo para hoy es válido en escritorio; vence a las 23:59.
        assertEquals(LocalDateTime.of(2026, 10, 26, 23, 59), enviado.fechaDevolucionPrevista)
    }

    @Test
    fun `prestamo rechaza duplicados y equipos no disponibles sin llamar al servidor`() = runBlocking {
        val repo = FakePrestamoRepository()
        val caso = CrearPrestamoUseCase(repo, reloj)
        val equipos = listOf(equipo(1), equipo(1), equipo(2, EstadoEquipo.PRESTADO))

        val errores = caso.validar("Ana", equipos, hoy, "Clase")
        assertTrue(errores.contains(ErrorPrestamo.EquiposDuplicados(listOf("EQP00001"))))
        val noDisponible = errores.filterIsInstance<ErrorPrestamo.EquiposNoDisponibles>().single()
        assertEquals("El equipo EQP00002 (Equipo 2) no está disponible. Estado actual: Prestado", noDisponible.mensaje)

        assertTrue(caso("Ana", equipos, hoy, "Clase").exceptionOrNull() is AppError.Validacion)
        assertNull(repo.creado)
    }

    @Test
    fun `prestamo exige solicitante, equipos, fecha desde hoy y observaciones`() {
        val errores = CrearPrestamoUseCase(FakePrestamoRepository(), reloj)
            .validar("  ", emptyList(), hoy.minusDays(1), " ")
        assertEquals(
            listOf(
                ErrorPrestamo.SinSolicitante,
                ErrorPrestamo.SinEquipos,
                ErrorPrestamo.FechaDevolucionInvalida,
                ErrorPrestamo.SinObservaciones,
            ),
            errores,
        )
        assertEquals("Las observaciones del préstamo son obligatorias.", ErrorPrestamo.SinObservaciones.mensaje)
    }

    // --- Devoluciones ---

    @Test
    fun `devolucion parcial deja el prestamo ACTIVO`() {
        val caso = ProcesarDevolucionUseCase(FakePrestamoRepository())
        val prestamo = prestamo(1 to false, 2 to false)
        val vista = caso.vistaPrevia(prestamo, listOf(ItemDevolucion(1, CondicionDevolucion.BUENO)))

        assertEquals(mapOf(1 to EstadoEquipo.DISPONIBLE), vista.nuevosEstados)
        assertEquals(1, vista.pendientesRestantes)
        assertFalse(vista.prestamoFinalizado)
    }

    @Test
    fun `devolver el ultimo pendiente finaliza el prestamo y el danado queda DANADO`() {
        val caso = ProcesarDevolucionUseCase(FakePrestamoRepository())
        val prestamo = prestamo(1 to true, 2 to false)
        val vista = caso.vistaPrevia(prestamo, listOf(ItemDevolucion(2, CondicionDevolucion.DANADO)))

        assertEquals(EstadoEquipo.DANADO, vista.nuevosEstados[2])
        assertTrue(vista.prestamoFinalizado)
    }

    @Test
    fun `devolucion rechaza equipos ya devueltos, ajenos o repetidos`() = runBlocking {
        val repo = FakePrestamoRepository()
        val caso = ProcesarDevolucionUseCase(repo)
        val prestamo = prestamo(1 to true, 2 to false)
        val items = listOf(
            ItemDevolucion(1, CondicionDevolucion.BUENO),
            ItemDevolucion(2, CondicionDevolucion.BUENO),
            ItemDevolucion(2, CondicionDevolucion.BUENO),
            ItemDevolucion(5, CondicionDevolucion.BUENO),
        )

        val errores = caso.validar(prestamo, items)
        assertEquals(2, errores.size)
        assertTrue(errores[0].contains("EQP00002"))
        assertTrue(errores[1].contains("EQP00001") && errores[1].contains("#5"))
        assertTrue(caso(prestamo, items).isFailure)
        assertNull(repo.devolucion)
    }

    @Test
    fun `no se devuelve sobre un prestamo FINALIZADO`() {
        val caso = ProcesarDevolucionUseCase(FakePrestamoRepository())
        val errores = caso.validar(
            prestamo(1 to false, estado = EstadoPrestamo.FINALIZADO),
            listOf(ItemDevolucion(1, CondicionDevolucion.BUENO)),
        )
        assertTrue(errores.single().contains("no está ACTIVO"))
    }

    // --- Mantenimiento ---

    @Test
    fun `ingreso a mantenimiento con las reglas de MantenimientoService`() = runBlocking {
        val repo = FakeMantenimientoRepository()
        val caso = IngresarMantenimientoUseCase(repo)
        val correctivo = TipoMantenimiento.CORRECTIVO

        assertTrue(caso(equipo(1, EstadoEquipo.DANADO), correctivo, " Pantalla rota ", " Carlos ").isSuccess)
        assertTrue(caso(equipo(2, EstadoEquipo.DISPONIBLE), TipoMantenimiento.PREVENTIVO, "Limpieza", "Carlos").isSuccess)

        val prestado = caso(equipo(3, EstadoEquipo.PRESTADO), correctivo, "x", "y").exceptionOrNull()
        assertTrue(prestado!!.message!!.contains("Un equipo PRESTADO no puede enviarse a mantenimiento"))
        assertTrue(caso(equipo(4, EstadoEquipo.BAJA), correctivo, "x", "y").isFailure)
        assertTrue(caso(equipo(5, EstadoEquipo.MANTENIMIENTO), correctivo, "x", "y").isFailure)
        assertEquals(
            listOf("Debe ingresar una descripción del problema o mantenimiento.", "Debe indicar el nombre del técnico o responsable."),
            caso.validar(equipo(6, EstadoEquipo.DANADO), correctivo, " ", ""),
        )
        assertEquals(listOf("Debe seleccionar un equipo."), caso.validar(null, correctivo, "x", "y"))
        // Como en la pantalla de escritorio: un dañado no entra como PREVENTIVO ni un disponible como CORRECTIVO.
        assertEquals(1, caso.validar(equipo(7, EstadoEquipo.DANADO), TipoMantenimiento.PREVENTIVO, "x", "y").size)
        assertTrue(caso(equipo(8, EstadoEquipo.DISPONIBLE), correctivo, "x", "y").isFailure)

        assertEquals(2, repo.ingresos.size)
        assertEquals("Pantalla rota", repo.ingresos[0].descripcion)
        assertEquals("Carlos", repo.ingresos[0].responsable)
    }

    @Test
    fun `cierre de mantenimiento devuelve el estado final y las observaciones son opcionales`() = runBlocking {
        val repo = FakeMantenimientoRepository()
        val caso = CerrarMantenimientoUseCase(repo)
        val enProceso = mantenimiento(1, 1)

        assertEquals(EstadoEquipo.DISPONIBLE, caso(enProceso, ResultadoMantenimiento.REPARADO, "  ").getOrThrow())
        assertEquals(EstadoEquipo.BAJA, caso(enProceso, ResultadoMantenimiento.NO_REPARABLE, "Placa quemada").getOrThrow())
        assertEquals(listOf(ResultadoMantenimiento.REPARADO to null, ResultadoMantenimiento.NO_REPARABLE to "Placa quemada"), repo.cierres)

        val finalizado = enProceso.copy(fechaSalida = LocalDateTime.of(2026, 10, 22, 8, 0), resultado = ResultadoMantenimiento.REPARADO)
        assertEquals(
            "Este mantenimiento ya fue finalizado anteriormente.",
            caso(finalizado, ResultadoMantenimiento.NO_REPARABLE, "").exceptionOrNull()?.message,
        )
        assertEquals(2, repo.cierres.size)
    }
}
