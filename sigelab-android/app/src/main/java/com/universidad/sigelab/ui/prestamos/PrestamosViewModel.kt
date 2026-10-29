package com.universidad.sigelab.ui.prestamos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.domain.model.CondicionDevolucion
import com.universidad.sigelab.domain.model.DetallePrestamo
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.ItemDevolucion
import com.universidad.sigelab.domain.model.Prestamo
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.repository.PrestamoRepository
import com.universidad.sigelab.domain.usecase.ConsultarEquipoPorQrUseCase
import com.universidad.sigelab.domain.usecase.CrearPrestamoUseCase
import com.universidad.sigelab.domain.usecase.ProcesarDevolucionUseCase
import com.universidad.sigelab.domain.usecase.VistaPreviaDevolucion
import com.universidad.sigelab.domain.usecase.filtrarEquipos
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
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Campos de registro de PrestamoView de escritorio. */
data class FormularioPrestamo(
    /** "Estudiante / Usuario". */
    val solicitante: String = "",
    /** "Fecha Fin Estimada". */
    val fechaFin: LocalDate,
    val observaciones: String = "",
    /** Ids de los equipos marcados en la tabla de disponibles. */
    val seleccion: Set<Int> = emptySet(),
)

/** Equipo pendiente elegido para "Procesar Devolución". */
data class DevolucionPendiente(val prestamo: Prestamo, val detalle: DetallePrestamo)

data class PrestamosUiState(
    val formulario: FormularioPrestamo,
    /** Tabla de equipos DISPONIBLES (filtrada por la búsqueda). */
    val disponibles: List<Equipo> = emptyList(),
    /** Equipos seleccionados, en el orden de la tabla. */
    val seleccionados: List<Equipo> = emptyList(),
    val activos: List<Prestamo> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val errorFormulario: String? = null,
    val registrando: Boolean = false,
    val devolucion: DevolucionPendiente? = null,
    val procesando: Boolean = false,
    val errorDevolucion: String? = null,
)

/**
 * "Gestión de Préstamos y Devoluciones" de escritorio. Las reglas son las de PrestamoService
 * (CrearPrestamoUseCase y ProcesarDevolucionUseCase) y el servidor hace la transacción con su
 * auditoría (PRESTAMO_CREADO, EQUIPO_DEVUELTO), igual que PrestamoRepository.
 */
@HiltViewModel
class PrestamosViewModel @Inject constructor(
    private val prestamoRepository: PrestamoRepository,
    private val equipoRepository: EquipoRepository,
    private val crearPrestamo: CrearPrestamoUseCase,
    private val procesarDevolucion: ProcesarDevolucionUseCase,
    private val consultarEquipoPorQr: ConsultarEquipoPorQrUseCase,
    private val reloj: Clock,
) : ViewModel() {

    private val estado = MutableStateFlow(PrestamosUiState(formulario = FormularioPrestamo(fechaFin = LocalDate.now(reloj))))
    private val busqueda = MutableStateFlow("")

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    /** Mismos avisos que los JOptionPane de escritorio. */
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    val ui: StateFlow<PrestamosUiState> = combine(
        estado,
        equipoRepository.observarEquipos(EstadoEquipo.DISPONIBLE),
        busqueda,
    ) { s, disponibles, texto ->
        val ordenados = disponibles.sortedBy { it.codigoInventario }
        s.copy(
            disponibles = filtrarEquipos(ordenados, null, texto),
            // Un equipo que dejó de estar DISPONIBLE sale de la selección.
            seleccionados = ordenados.filter { it.id in s.formulario.seleccion },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), estado.value)

    init {
        cargar()
    }

    fun cargar() {
        estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val sincronizacion = async { equipoRepository.sincronizar() }
            val activos = prestamoRepository.prestamosActivos()
            val errorEquipos = sincronizacion.await().exceptionOrNull()
            estado.update {
                it.copy(
                    cargando = false,
                    activos = activos.getOrNull()?.sortedBy { p -> p.id } ?: it.activos,
                    error = (activos.exceptionOrNull() ?: errorEquipos)?.message,
                )
            }
        }
    }

    // --- registrar préstamo ---

    fun onSolicitante(valor: String) = editar { copy(solicitante = valor) }
    fun onFechaFin(fecha: LocalDate) = editar { copy(fechaFin = fecha) }
    fun onObservaciones(valor: String) = editar { copy(observaciones = valor) }
    fun onBusqueda(texto: String) {
        busqueda.value = texto
    }

    fun alternar(idEquipo: Int) = editar {
        copy(seleccion = if (idEquipo in seleccion) seleccion - idEquipo else seleccion + idEquipo)
    }

    /** "Seleccionar todos": marca los disponibles visibles. */
    fun seleccionarTodos() = editar { copy(seleccion = seleccion + ui.value.disponibles.map { it.id }) }

    /** "Quitar selección". */
    fun quitarSeleccion() = editar { copy(seleccion = emptySet()) }

    /** "Limpiar Campos". */
    fun limpiar() = estado.update {
        it.copy(formulario = FormularioPrestamo(fechaFin = LocalDate.now(reloj)), errorFormulario = null)
    }

    private fun editar(cambio: FormularioPrestamo.() -> FormularioPrestamo) =
        estado.update { it.copy(formulario = it.formulario.cambio(), errorFormulario = null) }

    fun registrar() {
        val actual = ui.value
        if (actual.registrando) return
        val f = actual.formulario
        estado.update { it.copy(registrando = true, errorFormulario = null) }
        viewModelScope.launch {
            crearPrestamo(f.solicitante, actual.seleccionados, f.fechaFin, f.observaciones)
                .onSuccess {
                    estado.update {
                        it.copy(registrando = false, formulario = FormularioPrestamo(fechaFin = LocalDate.now(reloj)))
                    }
                    _mensajes.send("Préstamo registrado exitosamente.")
                    cargar()
                }
                .onFailure { e -> estado.update { it.copy(registrando = false, errorFormulario = e.message) } }
        }
    }

    /** "Escanear Código QR": un equipo DISPONIBLE se agrega a la selección; si no, se avisa su estado. */
    fun onQrLeido(texto: String) {
        viewModelScope.launch {
            consultarEquipoPorQr(texto)
                .onSuccess { equipo ->
                    if (equipo.estado == EstadoEquipo.DISPONIBLE) {
                        editar { copy(seleccion = seleccion + equipo.id) }
                        _mensajes.send("Equipo identificado y disponible. Se agregó a la selección del préstamo: ${equipo.codigoInventario}")
                    } else {
                        _mensajes.send(
                            "Equipo identificado, pero NO está disponible: no puede prestarse. " +
                                "${equipo.codigoInventario} · Estado actual: ${equipo.estado.etiqueta}",
                        )
                    }
                }
                .onFailure { e -> _mensajes.send(e.message ?: "No se pudo leer el QR") }
        }
    }

    // --- procesar devolución ---

    fun abrirDevolucion(prestamo: Prestamo, detalle: DetallePrestamo) =
        estado.update { it.copy(devolucion = DevolucionPendiente(prestamo, detalle), errorDevolucion = null) }

    fun cerrarDevolucion() {
        if (estado.value.procesando) return
        estado.update { it.copy(devolucion = null, errorDevolucion = null) }
    }

    /** Lo que pasará al confirmar: estado final del equipo y si el préstamo queda FINALIZADO. */
    fun vistaPrevia(condicion: CondicionDevolucion): VistaPreviaDevolucion? {
        val pendiente = estado.value.devolucion ?: return null
        return procesarDevolucion.vistaPrevia(pendiente.prestamo, listOf(ItemDevolucion(pendiente.detalle.idEquipo, condicion)))
    }

    fun procesar(condicion: CondicionDevolucion, observacion: String) {
        val actual = estado.value
        val pendiente = actual.devolucion ?: return
        if (actual.procesando) return
        estado.update { it.copy(procesando = true, errorDevolucion = null) }
        val item = ItemDevolucion(pendiente.detalle.idEquipo, condicion, observacion.trim().ifEmpty { null })
        viewModelScope.launch {
            procesarDevolucion(pendiente.prestamo, listOf(item))
                .onSuccess { resultado ->
                    estado.update { it.copy(procesando = false, devolucion = null) }
                    val cierre = if (resultado.prestamoFinalizado) " El préstamo #${pendiente.prestamo.id} quedó FINALIZADO." else ""
                    _mensajes.send("Devolución registrada en condición '${condicion.name}'.$cierre")
                    cargar()
                }
                .onFailure { e -> estado.update { it.copy(procesando = false, errorDevolucion = e.message) } }
        }
    }
}
