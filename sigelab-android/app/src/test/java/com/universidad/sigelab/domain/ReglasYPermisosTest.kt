package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.Acceso
import com.universidad.sigelab.domain.model.CondicionDevolucion
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.rules.ReglasEquipo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasYPermisosTest {

    @Test
    fun `ciclo de vida del equipo sigue el diagrama`() {
        assertTrue(ReglasEquipo.puedeCambiar(EstadoEquipo.DISPONIBLE, EstadoEquipo.PRESTADO))
        assertTrue(ReglasEquipo.puedeCambiar(EstadoEquipo.PRESTADO, EstadoEquipo.DANADO))
        assertTrue(ReglasEquipo.puedeCambiar(EstadoEquipo.MANTENIMIENTO, EstadoEquipo.BAJA))
        assertFalse(ReglasEquipo.puedeCambiar(EstadoEquipo.PRESTADO, EstadoEquipo.PRESTADO))
        assertFalse(ReglasEquipo.puedeCambiar(EstadoEquipo.DANADO, EstadoEquipo.PRESTADO))
        EstadoEquipo.entries.forEach { assertFalse("BAJA es terminal", ReglasEquipo.puedeCambiar(EstadoEquipo.BAJA, it)) }
    }

    @Test
    fun `devolucion y mantenimiento producen el estado esperado`() {
        assertEquals(EstadoEquipo.DISPONIBLE, ReglasEquipo.estadoTrasDevolucion(CondicionDevolucion.BUENO))
        assertEquals(EstadoEquipo.DANADO, ReglasEquipo.estadoTrasDevolucion(CondicionDevolucion.DANADO))
        assertEquals(EstadoEquipo.DISPONIBLE, ReglasEquipo.estadoTrasMantenimiento(ResultadoMantenimiento.REPARADO))
        assertEquals(EstadoEquipo.BAJA, ReglasEquipo.estadoTrasMantenimiento(ResultadoMantenimiento.NO_REPARABLE))
    }

    @Test
    fun `estado se interpreta con las variantes de SQL Server`() {
        assertEquals(EstadoEquipo.DANADO, EstadoEquipo.desde("DAÑADO"))
        assertEquals(EstadoEquipo.DANADO, EstadoEquipo.desde(" danado "))
        assertEquals(EstadoEquipo.BAJA, EstadoEquipo.desde("DE BAJA"))
        assertEquals(EstadoEquipo.MANTENIMIENTO, EstadoEquipo.desde("En mantenimiento"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `estado desconocido falla`() {
        EstadoEquipo.desde("PERDIDO")
    }

    @Test
    fun `solo ADMIN crea usuarios`() {
        assertTrue(Rol.ADMIN.puedeEditar(Modulo.USUARIOS))
        assertFalse(Rol.ENCARGADO.puedeVer(Modulo.USUARIOS))
        assertFalse(Rol.DOCENTE.puedeVer(Modulo.USUARIOS))
    }

    @Test
    fun `ENCARGADO opera todo menos usuarios y DOCENTE solo consulta equipos`() {
        Modulo.entries.filter { it != Modulo.USUARIOS }.forEach {
            assertEquals(Acceso.TOTAL, Rol.ENCARGADO.acceso(it))
        }
        assertEquals(Acceso.LECTURA, Rol.DOCENTE.acceso(Modulo.EQUIPOS))
        assertEquals(listOf(Modulo.EQUIPOS), Modulo.entries.filter(Rol.DOCENTE::puedeVer))
    }

    @Test
    fun `rol se lee sin importar mayusculas`() {
        assertEquals(Rol.ADMIN, Rol.desde("Admin"))
        assertEquals(Rol.ENCARGADO, Rol.desde("encargado "))
        assertNull(Rol.desde("INVITADO"))
    }
}
