package com.universidad.sigelab.ui.mantenimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.TipoMantenimiento
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.repository.MantenimientoRepository
import com.universidad.sigelab.domain.usecase.AccionQrMantenimiento
import com.universidad.sigelab.domain.usecase.CerrarMantenimientoUseCase
import com.universidad.sigelab.domain.usecase.ConsultarEquipoPorQrUseCase
import com.universidad.sigelab.domain.usecase.IngresarMantenimientoUseCase
import com.universidad.sigelab.domain.usecase.accionParaEquipoEscaneado
import com.universidad.sigelab.domain.usecase.candidatosParaMantenimiento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Equipo elegido como "Equipo Afectado" y el tipo con que ingresa. */
data class IngresoPendiente(val equipo: Equipo, val tipo: TipoMantenimiento)

data class MantenimientoUiState(
    /** Tabla "Mantenimientos en Proceso" de escritorio. */
    val enProceso: List<Mantenimiento> = emptyList(),
    /** "Tipo Mantenimiento": decide qué equipos aparecen como candidatos. */
    val tipo: TipoMantenimiento = TipoMantenimiento.CORRECTIVO,
    val candidatos: List<Equipo> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val ingreso: IngresoPendiente? = null,
    /** Mantenimiento elegido para "Cierre de Mantenimientos". */
    val cierre: Mantenimiento? = null,
    val enviando: Boolean = false,
    val errorDialogo: String? = null,
)

/**
 * Pantalla "Gestión de Mantenimiento" de escritorio: registrar el ingreso de un equipo
 * (PREVENTIVO para DISPONIBLES, CORRECTIVO para DAÑADOS) y finalizar los que están EN_PROCESO
 * (REPARADO → DISPONIBLE, NO_REPARABLE → BAJA). El servidor cambia los estados en una
 * transacción con su auditoría, igual que MantenimientoRepository.
 */
@HiltViewModel
class MantenimientoViewModel @Inject constructor(
    private val mantenimientoRepository: MantenimientoRepository,
    private val equipoRepository: EquipoRepository,
    private val ingresarMantenimiento: IngresarMantenimientoUseCase,
    private val cerrarMantenimiento: CerrarMantenimientoUseCase,
    private val consultarEquipoPorQr: ConsultarEquipoPorQrUseCase,
) : ViewModel() {

    private val estado = MutableStateFlow(MantenimientoUiState())
    private val busqueda = MutableStateFlow("")

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    /** Avisos de un solo uso para el snackbar. */
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    val ui: StateFlow<MantenimientoUiState> = combine(
        estado,
        equipoRepository.observarEquipos(),
        busqueda,
    ) { s, equipos, texto ->
        s.copy(candidatos = candidatosParaMantenimiento(equipos, s.tipo, texto))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MantenimientoUiState())

    init {
        cargar()
    }

    fun cargar() {
        estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            // Los candidatos salen de la caché de Room; se actualiza en paralelo con la lista en proceso.
            val sincronizacion = async { equipoRepository.sincronizar() }
            val enProceso = mantenimientoRepository.enProceso()
            val errorEquipos = sincronizacion.await().exceptionOrNull()
            estado.update {
                it.copy(
                    cargando = false,
                    enProceso = enProceso.getOrDefault(it.enProceso),
                    error = (enProceso.exceptionOrNull() ?: errorEquipos)?.message,
                )
            }
        }
    }

    fun onTipo(tipo: TipoMantenimiento) = estado.update { it.copy(tipo = tipo) }

    fun onBusqueda(texto: String) {
        busqueda.value = texto
    }

    fun abrirIngreso(equipo: Equipo, tipo: TipoMantenimiento = estado.value.tipo) =
        estado.update { it.copy(ingreso = IngresoPendiente(equipo, tipo), cierre = null, errorDialogo = null) }

    fun abrirCierre(mantenimiento: Mantenimiento) =
        estado.update { it.copy(cierre = mantenimiento, ingreso = null, errorDialogo = null) }

    fun cerrarDialogo() {
        if (estado.value.enviando) return
        estado.update { it.copy(ingreso = null, cierre = null, errorDialogo = null) }
    }

    fun confirmarIngreso(descripcion: String, responsable: String) {
        val actual = estado.value
        val ingreso = actual.ingreso ?: return
        if (actual.enviando) return
        estado.update { it.copy(enviando = true, errorDialogo = null) }

        viewModelScope.launch {
            ingresarMantenimiento(ingreso.equipo, ingreso.tipo, descripcion, responsable)
                .onSuccess {
                    val enProceso = mantenimientoRepository.enProceso()
                    estado.update { it.copy(enviando = false, ingreso = null, enProceso = enProceso.getOrDefault(it.enProceso)) }
                    refrescarEquipo(ingreso.equipo.codigoInventario)
                    _mensajes.send("Equipo ingresado a mantenimiento correctamente.")
                }
                .onFailure { e -> estado.update { it.copy(enviando = false, errorDialogo = e.message) } }
        }
    }

    fun confirmarCierre(resultado: ResultadoMantenimiento, observaciones: String) {
        val actual = estado.value
        val mantenimiento = actual.cierre ?: return
        if (actual.enviando) return
        estado.update { it.copy(enviando = true, errorDialogo = null) }

        viewModelScope.launch {
            cerrarMantenimiento(mantenimiento, resultado, observaciones)
                .onSuccess { estadoFinal ->
                    estado.update {
                        it.copy(enviando = false, cierre = null, enProceso = it.enProceso.filterNot { m -> m.id == mantenimiento.id })
                    }
                    refrescarEquipo(mantenimiento.codigoInventario)
                    _mensajes.send("Mantenimiento finalizado. El equipo pasa al estado: ${estadoFinal.name}")
                }
                .onFailure { e -> estado.update { it.copy(enviando = false, errorDialogo = e.message) } }
        }
    }

    /** Escanear un equipo abre el ingreso con el tipo que le corresponde o el cierre si ya está en proceso. */
    fun onQrLeido(texto: String) {
        viewModelScope.launch {
            consultarEquipoPorQr(texto)
                .onSuccess { equipo ->
                    when (val accion = accionParaEquipoEscaneado(equipo, estado.value.enProceso)) {
                        is AccionQrMantenimiento.Ingresar -> {
                            onTipo(accion.tipo)
                            abrirIngreso(accion.equipo, accion.tipo)
                        }
                        is AccionQrMantenimiento.Finalizar -> abrirCierre(accion.mantenimiento)
                        is AccionQrMantenimiento.NoAplica -> _mensajes.send(accion.mensaje)
                    }
                }
                .onFailure { e -> _mensajes.send(e.message ?: "No se pudo consultar el equipo") }
        }
    }

    /** El servidor ya cambió el estado: se actualiza esa fila de la caché para que las listas lo reflejen. */
    private suspend fun refrescarEquipo(codigo: String) {
        if (codigo.isNotBlank()) equipoRepository.buscarPorCodigo(codigo)
    }
}
