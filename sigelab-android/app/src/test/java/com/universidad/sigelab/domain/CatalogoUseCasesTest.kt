package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.EstadoRegistro
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.TipoEquipo
import com.universidad.sigelab.domain.usecase.CampoCatalogo
import com.universidad.sigelab.domain.usecase.CrearTipoEquipoUseCase
import com.universidad.sigelab.domain.usecase.GuardarLaboratorioUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoUseCasesTest {

    private val nuevoLab = Laboratorio(0, "", "Química", "Edificio B", 25)
    private val tipos = listOf(TipoEquipo(1, "Laptop", null), TipoEquipo(2, "Proyector", null))

    @Test
    fun `laboratorio exige nombre y capacidad mayor que cero, como escritorio`() {
        val errores = GuardarLaboratorioUseCase(FakeCatalogoRepository())
            .validar(nuevoLab.copy(nombre = " ", capacidad = 0))
        assertEquals(listOf(CampoCatalogo.NOMBRE, CampoCatalogo.CAPACIDAD), errores.map { it.campo })
        assertEquals("El nombre del laboratorio es obligatorio.", errores[0].mensaje)
        assertEquals("La capacidad debe ser un número mayor que cero.", errores[1].mensaje)

        // La ubicación es opcional.
        assertTrue(GuardarLaboratorioUseCase(FakeCatalogoRepository()).validar(nuevoLab.copy(ubicacion = "")).isEmpty())
    }

    @Test
    fun `el servidor asigna el codigo LAB y se exige rol operativo`() = runBlocking {
        val repo = FakeCatalogoRepository()
        val caso = GuardarLaboratorioUseCase(repo)

        assertTrue(caso(Rol.DOCENTE, nuevoLab).exceptionOrNull() is AppError.SinPermiso)

        val guardado = caso(Rol.ENCARGADO, nuevoLab.copy(nombre = " Química ", ubicacion = "  ")).getOrThrow()
        assertEquals("LAB-00077", guardado.codigo)
        val enviado = repo.laboratoriosGuardados.single()
        assertEquals("Química", enviado.nombre)
        assertEquals("", enviado.ubicacion)
        assertEquals(EstadoRegistro.ACTIVO, enviado.estado)
    }

    @Test
    fun `nombre de tipo de equipo obligatorio y sin repetir`() {
        val caso = CrearTipoEquipoUseCase(FakeCatalogoRepository())
        assertEquals(listOf(CampoCatalogo.NOMBRE), caso.validar(TipoEquipo(0, "LAPTOP", null), tipos).map { it.campo })
        assertEquals(
            "El nombre del tipo de equipo es obligatorio.",
            caso.validar(TipoEquipo(0, "  ", null), tipos).single().mensaje,
        )
        assertTrue(caso.validar(TipoEquipo(0, "Router", null), tipos).isEmpty())
    }

    @Test
    fun `crear tipo siempre envia un alta y descripcion vacia como null`() = runBlocking {
        val repo = FakeCatalogoRepository()
        val caso = CrearTipoEquipoUseCase(repo)

        assertTrue(caso(Rol.DOCENTE, TipoEquipo(0, "Router", null), tipos).exceptionOrNull() is AppError.SinPermiso)

        caso(Rol.ADMIN, TipoEquipo(5, " Router ", "  "), tipos).getOrThrow()
        val enviado = repo.tiposCreados.single()
        assertEquals(0, enviado.id)
        assertEquals("Router", enviado.nombre)
        assertNull(enviado.descripcion)
    }

    @Test
    fun `estado de registro tolera el Activo que guarda el procedimiento CrearUsuario`() {
        assertEquals(EstadoRegistro.ACTIVO, EstadoRegistro.desde("Activo"))
        assertEquals(EstadoRegistro.INACTIVO, EstadoRegistro.desde(" INACTIVO "))
        assertEquals(EstadoRegistro.ACTIVO, EstadoRegistro.desde(null))
    }
}
