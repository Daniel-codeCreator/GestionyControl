package com.universidad.sigelab.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.domain.model.DashboardMetricas
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.Usuario
import com.universidad.sigelab.domain.repository.AuthRepository
import com.universidad.sigelab.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class ModuloUi(val modulo: Modulo, val soloLectura: Boolean)

data class DashboardUiState(
    val usuario: Usuario? = null,
    val metricas: DashboardMetricas? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val modulos: List<ModuloUi> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val authRepository: AuthRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private sealed interface EstadoSync {
        data object Cargando : EstadoSync
        data object Ok : EstadoSync
        data class Error(val mensaje: String) : EstadoSync
    }

    private val refrescoManual = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * "Tiempo real" por sondeo: consulta el servidor cada 15 s solo mientras la pantalla
     * está visible (WhileSubscribed). Un refresco manual reinicia el ciclo.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val sincronizacion: Flow<EstadoSync> = refrescoManual
        .onStart { emit(Unit) }
        .flatMapLatest {
            flow<EstadoSync> {
                emit(EstadoSync.Cargando)
                while (true) {
                    val resultado = dashboardRepository.refrescar()
                    emit(
                        resultado.fold(
                            onSuccess = { EstadoSync.Ok },
                            onFailure = { EstadoSync.Error(it.message ?: "No se pudo actualizar") },
                        ),
                    )
                    delay(INTERVALO_SONDEO)
                }
            }
        }

    val ui: StateFlow<DashboardUiState> = combine(
        dashboardRepository.observarMetricas(),
        sincronizacion,
        sessionManager.sesion,
    ) { metricas, sync, sesion ->
        val rol = sesion?.usuario?.rol
        DashboardUiState(
            usuario = sesion?.usuario,
            metricas = metricas,
            cargando = sync is EstadoSync.Cargando,
            error = (sync as? EstadoSync.Error)?.mensaje,
            // El menú se arma con la matriz de permisos del rol: DOCENTE solo ve Equipos (consulta).
            modulos = rol?.let { r ->
                Modulo.entries.filter(r::puedeVer).map { ModuloUi(it, soloLectura = !r.puedeEditar(it)) }
            }.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun refrescar() {
        refrescoManual.tryEmit(Unit)
    }

    fun cerrarSesion() {
        viewModelScope.launch { authRepository.cerrarSesion() }
    }

    private companion object {
        val INTERVALO_SONDEO = 15.seconds
    }
}
