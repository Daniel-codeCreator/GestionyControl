package com.universidad.sigelab.domain.repository

import com.universidad.sigelab.domain.model.DashboardMetricas
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    /** Última instantánea guardada en Room (null si nunca se descargó). */
    fun observarMetricas(): Flow<DashboardMetricas?>

    /** Descarga las métricas del servidor y actualiza la caché. */
    suspend fun refrescar(): Result<Unit>
}
