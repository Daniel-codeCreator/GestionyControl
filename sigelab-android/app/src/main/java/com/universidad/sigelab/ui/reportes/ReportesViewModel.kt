package com.universidad.sigelab.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.domain.model.AccionAuditoria
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.PeriodoAuditoria
import com.universidad.sigelab.domain.model.RegistroAuditoria
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.repository.PrestamoRepository
import com.universidad.sigelab.domain.rules.QrEquipo
import com.universidad.sigelab.domain.rules.ReportesCsv
import com.universidad.sigelab.domain.usecase.ConsultarAuditoriaUseCase
import com.universidad.sigelab.domain.usecase.ConsultarHistorialMantenimientoUseCase
import com.universidad.sigelab.domain.usecase.PrestamoVencido
import com.universidad.sigelab.domain.usecase.ResumenInventario
import com.universidad.sigelab.domain.usecase.equiposPorEstado
import com.universidad.sigelab.domain.usecase.prestamosVencidos
import com.universidad.sigelab.domain.usecase.resumirInventario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Los tres reportes de ReportesView de escritorio (en el mismo orden) y la auditoría,
 * que es un agregado de la app solo para ADMIN.
 */
enum class PestanaReporte(val titulo: String, val reporte: String) {
    POR_ESTADO("Equipos", "Equipos por estado"),
    VENCIDOS("Vencidos", "Préstamos vencidos"),
    MANTENIMIENTOS("Historial", "Historial de mantenimiento"),
    AUDITORIA("Auditoría", "Auditoría"),
}

data class PorEstadoUi(
    /** null = TODOS. */
    val filtro: EstadoEquipo? = null,
    val equipos: List<Equipo> = emptyList(),
    val resumen: ResumenInventario = ResumenInventario(),
    val sincronizando: Boolean = false,
    val error: String? = null,
)

data class VencidosUi(
    val lista: List<PrestamoVencido> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val cargado: Boolean = false,
)

data class HistorialMantenimientoUi(
    /** "Código Inventario Equipo"; vacío = todos. */
    val codigo: String = "",
    val registros: List<Mantenimiento> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val consultado: Boolean = false,
)

data class AuditoriaUi(
    val periodo: PeriodoAuditoria = PeriodoAuditoria.SIETE_DIAS,
    val accion: AccionAuditoria? = null,
    val registros: List<RegistroAuditoria> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val cargado: Boolean = false,
)

data class ReportesUiState(
    /** Auditoría solo aparece para ADMIN. */
    val pestanas: List<PestanaReporte> = PestanaReporte.entries - PestanaReporte.AUDITORIA,
    val porEstado: PorEstadoUi = PorEstadoUi(),
    val vencidos: VencidosUi = VencidosUi(),
    val historial: HistorialMantenimientoUi = HistorialMantenimientoUi(),
    val auditoria: AuditoriaUi = AuditoriaUi(),
)

data class ReporteCompartible(val titulo: String, val csv: String)

@HiltViewModel
class ReportesViewModel @Inject constructor(
    private val prestamoRepository: PrestamoRepository,
    private val equipoRepository: EquipoRepository,
    private val consultarHistorialMantenimiento: ConsultarHistorialMantenimientoUseCase,
    private val consultarAuditoria: ConsultarAuditoriaUseCase,
    private val sessionManager: SessionManager,
    private val reloj: Clock,
) : ViewModel() {

    private val estado = MutableStateFlow(ReportesUiState())
    private var equiposSincronizados = false
    private var trabajoHistorial: Job? = null
    private var trabajoAuditoria: Job? = null

    val ui: StateFlow<ReportesUiState> = combine(
        estado,
        equipoRepository.observarEquipos(),
        sessionManager.sesion,
    ) { s, equipos, sesion ->
        s.copy(
            pestanas = PestanaReporte.entries.filter { it != PestanaReporte.AUDITORIA || sesion?.usuario?.rol?.veAuditoria == true },
            // "Equipos por estado" sale de la copia local, que se actualiza al abrir la pestaña.
            porEstado = s.porEstado.copy(
                equipos = equiposPorEstado(equipos, s.porEstado.filtro),
                resumen = resumirInventario(equipos),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportesUiState())

    /** Cada pestaña carga sus datos la primera vez que se muestra. */
    fun alMostrar(pestana: PestanaReporte) {
        when (pestana) {
            PestanaReporte.POR_ESTADO -> if (!equiposSincronizados) sincronizarEquipos()
            PestanaReporte.VENCIDOS -> if (!estado.value.vencidos.cargado) cargarVencidos()
            PestanaReporte.MANTENIMIENTOS -> if (!estado.value.historial.consultado) consultarHistorial()
            PestanaReporte.AUDITORIA -> if (!estado.value.auditoria.cargado) cargarAuditoria()
        }
    }

    // --- equipos por estado ---

    fun onFiltroEstado(filtro: EstadoEquipo?) = estado.update { it.copy(porEstado = it.porEstado.copy(filtro = filtro)) }

    fun sincronizarEquipos() {
        equiposSincronizados = true
        estado.update { it.copy(porEstado = it.porEstado.copy(sincronizando = true, error = null)) }
        viewModelScope.launch {
            val error = equipoRepository.sincronizar().exceptionOrNull()
            estado.update {
                it.copy(
                    porEstado = it.porEstado.copy(
                        sincronizando = false,
                        error = error?.let { e -> "${e.message}. Se muestra la copia guardada en el teléfono." },
                    ),
                )
            }
        }
    }

    // --- préstamos vencidos ---

    fun cargarVencidos() {
        estado.update { it.copy(vencidos = it.vencidos.copy(cargando = true, error = null)) }
        viewModelScope.launch {
            val activos = prestamoRepository.prestamosActivos()
            estado.update { s ->
                s.copy(
                    vencidos = VencidosUi(
                        lista = activos.getOrNull()?.let { prestamosVencidos(it, LocalDateTime.now(reloj)) } ?: s.vencidos.lista,
                        error = activos.exceptionOrNull()?.message,
                        cargado = true,
                    ),
                )
            }
        }
    }

    // --- historial de mantenimiento ---

    fun onCodigoHistorial(codigo: String) = estado.update { it.copy(historial = it.historial.copy(codigo = codigo)) }

    /** "Consultar Historial" de escritorio. */
    fun consultarHistorial() {
        val rol = rolActual() ?: return
        val codigo = estado.value.historial.codigo
        estado.update { it.copy(historial = it.historial.copy(cargando = true, error = null)) }
        trabajoHistorial?.cancel()
        trabajoHistorial = viewModelScope.launch {
            val registros = consultarHistorialMantenimiento(rol, codigo)
            estado.update {
                it.copy(
                    historial = it.historial.copy(
                        registros = registros.getOrDefault(it.historial.registros),
                        cargando = false,
                        error = registros.exceptionOrNull()?.message,
                        consultado = true,
                    ),
                )
            }
        }
    }

    /** El QR contiene el código de inventario: se usa como filtro, sin consultar el equipo. */
    fun onQrHistorial(texto: String) {
        val codigo = QrEquipo.extraerCodigo(texto)
        if (codigo == null) {
            estado.update { it.copy(historial = it.historial.copy(error = "No se encontró un código QR legible.")) }
            return
        }
        onCodigoHistorial(codigo)
        consultarHistorial()
    }

    // --- auditoría ---

    fun onPeriodo(periodo: PeriodoAuditoria) {
        estado.update { it.copy(auditoria = it.auditoria.copy(periodo = periodo)) }
        cargarAuditoria()
    }

    fun onAccion(accion: AccionAuditoria?) {
        estado.update { it.copy(auditoria = it.auditoria.copy(accion = accion)) }
        cargarAuditoria()
    }

    fun cargarAuditoria() {
        val rol = rolActual() ?: return
        val filtro = estado.value.auditoria
        estado.update { it.copy(auditoria = it.auditoria.copy(cargando = true, error = null)) }
        // Un cambio de filtro cancela la consulta anterior para que no pise a la nueva.
        trabajoAuditoria?.cancel()
        trabajoAuditoria = viewModelScope.launch {
            val registros = consultarAuditoria(rol, filtro.periodo, filtro.accion?.name)
            estado.update {
                it.copy(
                    auditoria = it.auditoria.copy(
                        registros = registros.getOrDefault(it.auditoria.registros),
                        cargando = false,
                        error = registros.exceptionOrNull()?.message,
                        cargado = true,
                    ),
                )
            }
        }
    }

    // --- compartir ---

    /** CSV del reporte visible, con las columnas de la tabla de escritorio, o null si no tiene datos. */
    fun paraCompartir(pestana: PestanaReporte): ReporteCompartible? {
        val s = ui.value
        val titulo = "SIGELAB · ${pestana.reporte}"
        return when (pestana) {
            PestanaReporte.POR_ESTADO -> s.porEstado.equipos.takeIf { it.isNotEmpty() }
                ?.let { ReporteCompartible(titulo, ReportesCsv.equiposPorEstado(it)) }
            PestanaReporte.VENCIDOS -> s.vencidos.lista.takeIf { it.isNotEmpty() }
                ?.let { ReporteCompartible(titulo, ReportesCsv.vencidos(it)) }
            PestanaReporte.MANTENIMIENTOS -> s.historial.registros.takeIf { it.isNotEmpty() }
                ?.let { ReporteCompartible(titulo, ReportesCsv.historialMantenimiento(it)) }
            PestanaReporte.AUDITORIA -> s.auditoria.registros.takeIf { it.isNotEmpty() }
                ?.let { ReporteCompartible(titulo, ReportesCsv.auditoria(it)) }
        }
    }

    private fun rolActual(): Rol? = sessionManager.sesion.value?.usuario?.rol
}
