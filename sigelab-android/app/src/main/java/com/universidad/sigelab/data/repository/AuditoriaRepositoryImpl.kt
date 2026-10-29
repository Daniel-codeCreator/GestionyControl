package com.universidad.sigelab.data.repository

import androidx.work.WorkManager
import com.universidad.sigelab.core.network.apiCall
import com.universidad.sigelab.core.network.exigirExito
import com.universidad.sigelab.data.local.AuditoriaPendienteDao
import com.universidad.sigelab.data.local.AuditoriaPendienteEntity
import com.universidad.sigelab.data.remote.api.SigelabApi
import com.universidad.sigelab.data.sync.AuditoriaSyncWorker
import com.universidad.sigelab.data.toRequest
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.repository.AuditoriaRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Patrón outbox: el evento se guarda primero en Room y WorkManager lo envía en cuanto
 * hay red. Así el registro sobrevive a la falta de conexión y al cierre de la app,
 * y la pantalla de login no espera a la red para mostrar el error.
 */
@Singleton
class AuditoriaRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
    private val dao: AuditoriaPendienteDao,
    private val workManager: WorkManager,
) : AuditoriaRepository {

    override suspend fun registrarLoginFallido(nombreUsuario: String, idUsuario: Int?, motivo: String) {
        dao.insertar(
            AuditoriaPendienteEntity(
                accion = LOGIN_FALLIDO,
                nombreUsuario = nombreUsuario,
                idUsuario = idUsuario,
                detalle = motivo.take(400),
                fechaCliente = Instant.now().toString(),
            ),
        )
        AuditoriaSyncWorker.programar(workManager)
    }

    override suspend fun enviarPendientes(): Boolean {
        for (evento in dao.pendientes(LOTE)) {
            val resultado = apiCall { api.registrarEvento(evento.toRequest()).exigirExito() }
            // Un 4xx no se arregla reintentando: se descarta para no bloquear la cola.
            if (resultado.isFailure && resultado.exceptionOrNull() !is AppError.Validacion) return false
            dao.eliminar(evento)
        }
        return dao.contar() == 0
    }

    private companion object {
        const val LOGIN_FALLIDO = "LOGIN_FALLIDO"
        const val LOTE = 50
    }
}
