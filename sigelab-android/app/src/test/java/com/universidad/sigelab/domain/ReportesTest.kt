package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.EstadoPrestamo
import com.universidad.sigelab.domain.model.FiltroAuditoria
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.PeriodoAuditoria
import com.universidad.sigelab.domain.model.RegistroAuditoria
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.repository.ReporteRepository
import com.universidad.sigelab.domain.rules.ReportesCsv
import com.universidad.sigelab.domain.usecase.ConsultarAuditoriaUseCase
import com.universidad.sigelab.domain.usecase.ConsultarHistorialMantenimientoUseCase
import com.universidad.sigelab.domain.usecase.equiposPorEstado
import com.universidad.sigelab.domain.usecase.prestamosVencidos
import com.universidad.sigelab.domain.usecase.resumirInventario
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

class ReportesTest {

    private val ahora = LocalDateTime.of(2026, 10, 27, 10, 0)

    @Test
    fun `vencidos solo incluye activos atrasados, del mas atrasado al menos`() {
        val aTiempo = prestamo(1 to false).copy(id = 1, fechaDevolucionPrevista = ahora.plusDays(1))
        val hoy = prestamo(2 to false).copy(id = 2, fechaDevolucionPrevista = ahora.minusHours(2))
        val tresDias = prestamo(3 to false).copy(id = 3, fechaDevolucionPrevista = ahora.minusDays(3))
        val finalizado = prestamo(4 to true, estado = EstadoPrestamo.FINALIZADO).copy(id = 4, fechaDevolucionPrevista = ahora.minusDays(9))

        val vencidos = prestamosVencidos(listOf(aTiempo, hoy, tresDias, finalizado), ahora)

        assertEquals(listOf(3, 2), vencidos.map { it.prestamo.id })
        assertEquals(listOf(3L, 0L), vencidos.map { it.diasDeAtraso })
    }

    @Test
    fun `equipos por estado con TODOS o un estado, ordenados por codigo`() {
        val equipos = listOf(equipo(3, EstadoEquipo.PRESTADO), equipo(1), equipo(2, EstadoEquipo.PRESTADO), equipo(4, EstadoEquipo.BAJA))

        assertEquals(listOf(1, 2, 3, 4), equiposPorEstado(equipos, null).map { it.id })
        assertEquals(listOf(2, 3), equiposPorEstado(equipos, EstadoEquipo.PRESTADO).map { it.id })

        val resumen = resumirInventario(equipos)
        assertEquals(4, resumen.total)
        assertEquals(2, resumen.conteos[EstadoEquipo.PRESTADO])
        assertNull(resumen.conteos[EstadoEquipo.DANADO])
    }

    private class FakeReporteRepository : ReporteRepository {
        var filtro: FiltroAuditoria? = null
        var codigoConsultado: String? = "sin consultar"

        override suspend fun historialMantenimiento(codigoInventario: String?): Result<List<Mantenimiento>> {
            codigoConsultado = codigoInventario
            return Result.success(
                listOf(mantenimiento(1, 1), mantenimiento(2, 2).copy(fechaIngreso = LocalDateTime.of(2026, 10, 25, 8, 0))),
            )
        }

        override suspend fun auditoria(filtro: FiltroAuditoria): Result<List<RegistroAuditoria>> {
            this.filtro = filtro
            return Result.success(
                listOf(
                    RegistroAuditoria(1, LocalDateTime.of(2026, 10, 25, 8, 0), "LOGIN_EXITOSO", "admin", "Usuario", ""),
                    RegistroAuditoria(2, LocalDateTime.of(2026, 10, 27, 9, 0), "LOGIN_FALLIDO", null, "Usuario", "x"),
                ),
            )
        }
    }

    @Test
    fun `historial de mantenimiento con codigo opcional y lo mas reciente primero`() = runBlocking {
        val repo = FakeReporteRepository()
        val caso = ConsultarHistorialMantenimientoUseCase(repo)

        assertTrue(caso(Rol.DOCENTE, "").exceptionOrNull() is AppError.SinPermiso)

        val todos = caso(Rol.ENCARGADO, "  ").getOrThrow()
        assertNull("código vacío = todos, como en escritorio", repo.codigoConsultado)
        assertEquals(listOf(2, 1), todos.map { it.id })

        caso(Rol.ENCARGADO, " EQP00002 ")
        assertEquals("EQP00002", repo.codigoConsultado)
    }

    private val reloj = Clock.fixed(ahora.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Test
    fun `auditoria solo para ADMIN, con rango del periodo y lo mas reciente primero`() = runBlocking {
        val repo = FakeReporteRepository()
        val caso = ConsultarAuditoriaUseCase(repo, reloj)

        assertTrue(caso(Rol.ENCARGADO, PeriodoAuditoria.HOY, null).exceptionOrNull() is AppError.SinPermiso)
        assertNull(repo.filtro)

        val registros = caso(Rol.ADMIN, PeriodoAuditoria.SIETE_DIAS, "LOGIN_FALLIDO").getOrThrow()
        assertEquals(FiltroAuditoria(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 27), "LOGIN_FALLIDO"), repo.filtro)
        assertEquals(listOf(2L, 1L), registros.map { it.id })
        assertTrue(Rol.ADMIN.veAuditoria)
        assertFalse(Rol.ENCARGADO.veAuditoria)
    }

    @Test
    fun `csv escapa comillas y comas y neutraliza formulas`() {
        assertEquals("simple", ReportesCsv.celda("simple"))
        assertEquals("\"Pérez, Ana\"", ReportesCsv.celda("Pérez, Ana"))
        assertEquals("\"dijo \"\"hola\"\"\"", ReportesCsv.celda("dijo \"hola\""))
        // Un "usuario" escrito en un login fallido no debe ejecutarse como fórmula al abrir en Excel.
        assertEquals("'=HYPERLINK(\"x\")".let { "\"" + it.replace("\"", "\"\"") + "\"" }, ReportesCsv.celda("=HYPERLINK(\"x\")"))
        assertEquals("'@SUMA", ReportesCsv.celda("@SUMA"))

        val csv = ReportesCsv.tabla(listOf("A", "B"), listOf(listOf("1", "2")))
        assertEquals("A,B\r\n1,2", csv)
    }

    @Test
    fun `csv con las columnas de las tablas de escritorio`() {
        val vencido = prestamosVencidos(listOf(prestamo(1 to true, 2 to false).copy(fechaDevolucionPrevista = ahora.minusDays(1))), ahora)
        val filas = ReportesCsv.vencidos(vencido).split("\r\n")
        assertTrue(filas[0].startsWith("ID Préstamo,Solicitante,Fecha Préstamo,Fecha Esperada,Estado"))
        assertTrue(filas[1].contains("Ana Pérez") && filas[1].contains("EQP00002 Equipo 2"))
        assertFalse(filas[1].contains("EQP00001"))

        val cerrado = mantenimiento(5, 1).copy(fechaSalida = ahora, resultado = ResultadoMantenimiento.NO_REPARABLE)
        val historial = ReportesCsv.historialMantenimiento(listOf(cerrado)).split("\r\n")
        assertEquals("ID Mant.,Código Equipo,Equipo,Tipo,Ingreso,Salida,Resultado,Técnico", historial[0])
        assertEquals("5,EQP00001,Equipo 1,CORRECTIVO,2026-10-20 15:00,2026-10-27 10:00,NO_REPARABLE,Carlos Ruiz", historial[1])

        assertEquals("ID,Código,Nombre,Tipo,Laboratorio,Estado", ReportesCsv.equiposPorEstado(emptyList()))
    }
}
