package com.universidad.sigelab.data.repository

import com.universidad.sigelab.core.network.apiCall
import com.universidad.sigelab.data.local.DashboardDao
import com.universidad.sigelab.data.remote.api.SigelabApi
import com.universidad.sigelab.data.toDomain
import com.universidad.sigelab.data.toEntity
import com.universidad.sigelab.domain.model.DashboardMetricas
import com.universidad.sigelab.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Room es la única fuente para la UI; la red solo actualiza Room (single source of truth). */
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
    private val dao: DashboardDao,
) : DashboardRepository {

    override fun observarMetricas(): Flow<DashboardMetricas?> = dao.observar().map { it?.toDomain() }

    override suspend fun refrescar(): Result<Unit> =
        apiCall { api.metricas().toEntity(Instant.now()) }.map { dao.guardar(it) }
}
