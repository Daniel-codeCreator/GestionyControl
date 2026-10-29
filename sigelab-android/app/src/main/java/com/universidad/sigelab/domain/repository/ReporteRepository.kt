package com.universidad.sigelab.domain.repository

import com.universidad.sigelab.domain.model.FiltroAuditoria
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.RegistroAuditoria

interface ReporteRepository {
    /**
     * "Consultar Historial" de escritorio: todos los mantenimientos, del más reciente al más antiguo,
     * filtrados por código de inventario si se indica.
     */
    suspend fun historialMantenimiento(codigoInventario: String?): Result<List<Mantenimiento>>

    /** Registros de la tabla Auditoría (solo ADMIN). */
    suspend fun auditoria(filtro: FiltroAuditoria): Result<List<RegistroAuditoria>>
}
