package com.universidad.sigelab.domain.repository

import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.TipoEquipo
import kotlinx.coroutines.flow.Flow

/**
 * Mismas operaciones que escritorio: laboratorios se crean y se actualizan (incluido su estado
 * ACTIVO/INACTIVO); tipos de equipo solo se crean. Ninguno de los dos se elimina.
 */
interface CatalogoRepository {
    suspend fun laboratorios(): Result<List<Laboratorio>>

    /** Alta (id = 0, el servidor genera el código LAB-00001) o actualización. */
    suspend fun guardarLaboratorio(laboratorio: Laboratorio): Result<Laboratorio>

    suspend fun tiposEquipo(): Result<List<TipoEquipo>>
    suspend fun crearTipoEquipo(tipo: TipoEquipo): Result<TipoEquipo>
}

interface EquipoRepository {
    /** Equipos desde la caché local (Room). */
    fun observarEquipos(estado: EstadoEquipo? = null): Flow<List<Equipo>>

    /** Un equipo de la caché local (null si no está). */
    fun observarEquipo(id: Int): Flow<Equipo?>

    /** Reemplaza la caché local con la lista del servidor. */
    suspend fun sincronizar(): Result<Unit>

    /** Consulta por código de inventario (lectura de QR). Solo lectura: no modifica el estado. */
    suspend fun buscarPorCodigo(codigoInventario: String): Result<Equipo>

    /** Alta (id = 0: el servidor genera el código EQP00001 y el equipo nace DISPONIBLE) o actualización. */
    suspend fun guardar(equipo: Equipo): Result<Equipo>
}
