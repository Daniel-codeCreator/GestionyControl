package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.rules.QrEquipo
import com.universidad.sigelab.domain.usecase.CampoEquipo
import com.universidad.sigelab.domain.usecase.ConsultarEquipoPorQrUseCase
import com.universidad.sigelab.domain.usecase.GuardarEquipoUseCase
import com.universidad.sigelab.domain.usecase.filtrarEquipos
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EquiposUseCasesTest {

    // --- QR ---

    @Test
    fun `el QR contiene el codigo de inventario y se guarda como en escritorio`() {
        assertEquals("EQP00001", QrEquipo.contenido(equipo(1)))
        assertEquals("QR_EQP00001.png", QrEquipo.nombreArchivo(equipo(1)))
        assertEquals("EQP00001", QrEquipo.extraerCodigo("  EQP00001\n"))
        assertNull(QrEquipo.extraerCodigo("   "))
        assertNull(QrEquipo.extraerCodigo("EQP\u000000001"))
        assertNull(QrEquipo.extraerCodigo("X".repeat(65)))
    }

    @Test
    fun `leer el QR solo consulta y nunca guarda`() = runBlocking {
        val existente = equipo(1, EstadoEquipo.PRESTADO)
        val repo = FakeEquipoRepository(porCodigo = mapOf("EQP00001" to existente))

        val resultado = ConsultarEquipoPorQrUseCase(repo)(" EQP00001 ")

        assertEquals(existente, resultado.getOrThrow())
        assertEquals(listOf("EQP00001"), repo.codigosBuscados)
        assertTrue(repo.guardados.isEmpty())
    }

    @Test
    fun `QR ilegible no llega al servidor y codigo inexistente se informa`() = runBlocking {
        val repo = FakeEquipoRepository()
        val caso = ConsultarEquipoPorQrUseCase(repo)

        assertTrue(caso("\n\t").exceptionOrNull() is AppError.Validacion)
        assertTrue(repo.codigosBuscados.isEmpty())
        assertTrue(caso("EQP00404").exceptionOrNull() is AppError.NoEncontrado)
    }

    // --- Alta y actualización ---

    private val nuevo = equipo(0).copy(codigoInventario = "", nombre = "Laptop Dell", marca = "Dell", modelo = "Latitude 5440")

    @Test
    fun `alta valida nombre, tipo y laboratorio con los mensajes de escritorio`() {
        val errores = GuardarEquipoUseCase(FakeEquipoRepository())
            .validar(nuevo.copy(nombre = " ", idTipoEquipo = 0, idLaboratorio = 0))
        assertEquals(listOf(CampoEquipo.NOMBRE, CampoEquipo.TIPO, CampoEquipo.LABORATORIO), errores.map { it.campo })
        assertEquals(
            listOf("El nombre del equipo es obligatorio.", "Debe seleccionar un tipo de equipo.", "Debe seleccionar un laboratorio."),
            errores.map { it.mensaje },
        )
        // Marca, modelo y serie son opcionales; el código lo genera el servidor.
        assertTrue(GuardarEquipoUseCase(FakeEquipoRepository()).validar(nuevo.copy(marca = "", modelo = "")).isEmpty())
    }

    @Test
    fun `solo ADMIN y ENCARGADO guardan equipos y el servidor asigna el codigo`() = runBlocking {
        val repo = FakeEquipoRepository()
        val caso = GuardarEquipoUseCase(repo)

        assertTrue(caso(Rol.DOCENTE, nuevo).exceptionOrNull() is AppError.SinPermiso)
        assertTrue(repo.guardados.isEmpty())

        val guardado = caso(Rol.ENCARGADO, nuevo.copy(nombre = "  Laptop  ", numeroSerie = " SN-77 ")).getOrThrow()
        assertEquals("EQP00500", guardado.codigoInventario)
        assertEquals("Laptop", repo.guardados.single().nombre)
        assertEquals("SN-77", repo.guardados.single().numeroSerie)
    }

    // --- Lista ---

    @Test
    fun `filtro por estado y por texto en codigo, nombre o serie`() {
        val equipos = listOf(
            equipo(1),
            equipo(2, EstadoEquipo.PRESTADO),
            equipo(3, EstadoEquipo.PRESTADO).copy(nombre = "Proyector Epson", numeroSerie = "X9-123"),
        )
        assertEquals(listOf(2, 3), filtrarEquipos(equipos, EstadoEquipo.PRESTADO, "").map { it.id })
        assertEquals(listOf(3), filtrarEquipos(equipos, null, "epson").map { it.id })
        assertEquals(listOf(3), filtrarEquipos(equipos, null, "x9-1").map { it.id })
        assertEquals(listOf(2), filtrarEquipos(equipos, EstadoEquipo.PRESTADO, " eqp00002 ").map { it.id })
    }
}
