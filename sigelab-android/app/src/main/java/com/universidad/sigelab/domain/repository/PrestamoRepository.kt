package com.universidad.sigelab.domain.repository

import com.universidad.sigelab.domain.model.ItemDevolucion
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.NuevoMantenimiento
import com.universidad.sigelab.domain.model.NuevoPrestamo
import com.universidad.sigelab.domain.model.NuevoUsuario
import com.universidad.sigelab.domain.model.Prestamo
import com.universidad.sigelab.domain.model.ResultadoDevolucion
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.UsuarioRegistrado

/**
 * Las operaciones que cambian estados se ejecutan en el servidor dentro de una transacción
 * (equipos + préstamo + auditoría), con las mismas consultas que los repositorios de escritorio.
 * La app valida antes para dar respuesta inmediata, pero el servidor vuelve a validar y responde
 * 409 si algo cambió entretanto.
 */
interface PrestamoRepository {
    /** Préstamos ACTIVOS con sus equipos PENDIENTES. */
    suspend fun prestamosActivos(): Result<List<Prestamo>>

    /** Devuelve el id del préstamo. Servidor: equipos → PRESTADO y auditoría PRESTAMO_CREADO. */
    suspend fun crear(nuevo: NuevoPrestamo): Result<Int>

    /** Servidor: estados según condición, EQUIPO_DEVUELTO y FINALIZADO si no quedan pendientes. */
    suspend fun registrarDevolucion(idPrestamo: Int, items: List<ItemDevolucion>): Result<ResultadoDevolucion>
}

interface MantenimientoRepository {
    /** Mantenimientos EN_PROCESO, del más antiguo al más reciente. */
    suspend fun enProceso(): Result<List<Mantenimiento>>

    /** Servidor: equipo → MANTENIMIENTO y auditoría EQUIPO_ENVIADO_MANTENIMIENTO. */
    suspend fun ingresar(nuevo: NuevoMantenimiento): Result<Unit>

    /** Servidor: REPARADO → DISPONIBLE, NO_REPARABLE → BAJA y auditoría MANTENIMIENTO_FINALIZADO. */
    suspend fun finalizar(
        idMantenimiento: Int,
        resultado: ResultadoMantenimiento,
        observaciones: String?,
    ): Result<Unit>
}

interface UsuarioRepository {
    /** Tabla "USUARIOS REGISTRADOS". */
    suspend fun listar(): Result<List<UsuarioRegistrado>>

    /** Ejecuta el procedimiento CrearUsuario y devuelve el IdUsuario generado. */
    suspend fun crear(nuevo: NuevoUsuario): Result<Int>

    /** POST /face/register: el IdUsuario es el vínculo con el perfil facial. */
    suspend fun registrarRostro(idUsuario: Int, fotoJpeg: ByteArray): Result<Unit>
}
