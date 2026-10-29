package com.universidad.sigelab.data.repository

import com.universidad.sigelab.core.network.apiCall
import com.universidad.sigelab.data.local.EquipoDao
import com.universidad.sigelab.data.remote.api.SigelabApi
import com.universidad.sigelab.data.remote.dto.EquipoRequest
import com.universidad.sigelab.data.toDomain
import com.universidad.sigelab.data.toDto
import com.universidad.sigelab.data.toEntity
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.TipoEquipo
import com.universidad.sigelab.domain.repository.CatalogoRepository
import com.universidad.sigelab.domain.repository.EquipoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogoRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
) : CatalogoRepository {

    override suspend fun laboratorios() = apiCall { api.laboratorios().map { it.toDomain() } }

    override suspend fun guardarLaboratorio(laboratorio: Laboratorio) = apiCall {
        val body = laboratorio.toDto()
        val guardado = if (laboratorio.id == 0) api.crearLaboratorio(body) else api.actualizarLaboratorio(laboratorio.id, body)
        guardado.toDomain()
    }

    override suspend fun tiposEquipo() = apiCall { api.tiposEquipo().map { it.toDomain() } }

    /** El nombre tiene UNIQUE en SQL Server: un duplicado vuelve como 409 (AppError.Conflicto). */
    override suspend fun crearTipoEquipo(tipo: TipoEquipo) = apiCall { api.crearTipoEquipo(tipo.toDto()).toDomain() }
}

@Singleton
class EquipoRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
    private val dao: EquipoDao,
) : EquipoRepository {

    override fun observarEquipos(estado: EstadoEquipo?): Flow<List<Equipo>> =
        (if (estado == null) dao.observarTodos() else dao.observarPorEstado(estado.name))
            .map { lista -> lista.map { it.toDomain() } }

    override fun observarEquipo(id: Int): Flow<Equipo?> = dao.observarPorId(id).map { it?.toDomain() }

    override suspend fun sincronizar(): Result<Unit> =
        apiCall { api.equipos().map { it.toEntity() } }.map { dao.reemplazarTodo(it) }

    override suspend fun buscarPorCodigo(codigoInventario: String): Result<Equipo> {
        val codigo = codigoInventario.trim()
        return apiCall(
            alErrorHttp = { http, _ ->
                // Mismo texto que QrService de escritorio.
                if (http == 404) AppError.NoEncontrado("El QR corresponde al código \"$codigo\", que no existe en el sistema.") else null
            },
        ) { api.equipoPorCodigo(codigo).toEntity() }
            .onSuccess { dao.guardar(listOf(it)) }
            .map { it.toDomain() }
            .recoverCatching { error ->
                // Sin red se responde con la copia local (puede estar desactualizada).
                if (error !is AppError.SinConexion) throw error
                dao.buscarPorCodigo(codigo)?.toDomain() ?: throw error
            }
    }

    override suspend fun guardar(equipo: Equipo): Result<Equipo> =
        apiCall {
            val body = EquipoRequest(
                nombre = equipo.nombre.trim(),
                idTipoEquipo = equipo.idTipoEquipo,
                idLaboratorio = equipo.idLaboratorio,
                marca = equipo.marca.trim(),
                modelo = equipo.modelo.trim(),
                numeroSerie = equipo.numeroSerie.trim(),
            )
            val guardado = if (equipo.id == 0) api.crearEquipo(body) else api.actualizarEquipo(equipo.id, body)
            guardado.toEntity()
        }
            .onSuccess { dao.guardar(listOf(it)) }
            .map { it.toDomain() }
}
