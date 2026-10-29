package com.universidad.sigelab.data.repository

import com.universidad.sigelab.core.network.apiCall
import com.universidad.sigelab.data.parsearFecha
import com.universidad.sigelab.data.remote.api.SigelabApi
import com.universidad.sigelab.data.toDomain
import com.universidad.sigelab.domain.model.FiltroAuditoria
import com.universidad.sigelab.domain.model.LIMITE_AUDITORIA
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.RegistroAuditoria
import com.universidad.sigelab.domain.repository.ReporteRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReporteRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
) : ReporteRepository {

    override suspend fun historialMantenimiento(codigoInventario: String?): Result<List<Mantenimiento>> = apiCall {
        api.historialMantenimiento(codigoInventario).map { it.toDomain() }
    }

    override suspend fun auditoria(filtro: FiltroAuditoria): Result<List<RegistroAuditoria>> = apiCall {
        api.auditoria(desde = filtro.desde.toString(), hasta = filtro.hasta.toString(), accion = filtro.accion, limite = LIMITE_AUDITORIA)
            .map { RegistroAuditoria(it.idAuditoria, parsearFecha(it.fecha), it.accion, it.usuario, it.entidad, it.detalle.orEmpty()) }
    }
}
