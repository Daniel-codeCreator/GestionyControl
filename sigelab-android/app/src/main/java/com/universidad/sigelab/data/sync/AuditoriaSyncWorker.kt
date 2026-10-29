package com.universidad.sigelab.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.universidad.sigelab.domain.repository.AuditoriaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Envía la bandeja de auditoría cuando hay red; reintenta con backoff si el servidor no responde. */
@HiltWorker
class AuditoriaSyncWorker @AssistedInject constructor(
    @Assisted contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val auditoria: AuditoriaRepository,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result =
        if (auditoria.enviarPendientes()) Result.success() else Result.retry()

    companion object {
        private const val NOMBRE = "auditoria-sync"

        fun programar(workManager: WorkManager) {
            val peticion = OneTimeWorkRequestBuilder<AuditoriaSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            workManager.enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.KEEP, peticion)
        }
    }
}
