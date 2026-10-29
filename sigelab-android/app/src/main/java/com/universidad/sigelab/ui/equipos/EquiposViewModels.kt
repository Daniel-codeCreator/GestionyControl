package com.universidad.sigelab.ui.equipos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.TipoEquipo
import com.universidad.sigelab.domain.repository.CatalogoRepository
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.usecase.CampoEquipo
import com.universidad.sigelab.domain.usecase.ConsultarEquipoPorQrUseCase
import com.universidad.sigelab.domain.usecase.GuardarEquipoUseCase
import com.universidad.sigelab.domain.usecase.filtrarEquipos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Argumento de navegación compartido por el detalle y el formulario (0 = equipo nuevo). */
const val ARG_ID_EQUIPO = "idEquipo"

// ------------------------------------------------------------------------- lista

data class EquiposUiState(
    val equipos: List<Equipo> = emptyList(),
    val conteos: Map<EstadoEquipo, Int> = emptyMap(),
    val total: Int = 0,
    val filtro: EstadoEquipo? = null,
    val sincronizando: Boolean = false,
    val error: String? = null,
    val soloLectura: Boolean = true,
)

/** La lista sale de Room; al abrir la pantalla (o deslizar) se reemplaza con la del servidor. */
@HiltViewModel
class EquiposViewModel @Inject constructor(
    private val equipoRepository: EquipoRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private data class EstadoSync(val cargando: Boolean = false, val error: String? = null)

    private val filtro = MutableStateFlow<EstadoEquipo?>(null)
    private val busqueda = MutableStateFlow("")
    private val sync = MutableStateFlow(EstadoSync())

    val ui: StateFlow<EquiposUiState> = combine(
        equipoRepository.observarEquipos(),
        filtro,
        busqueda,
        sync,
        sessionManager.sesion,
    ) { todos, estado, texto, s, sesion ->
        EquiposUiState(
            equipos = filtrarEquipos(todos, estado, texto),
            conteos = todos.groupingBy { it.estado }.eachCount(),
            total = todos.size,
            filtro = estado,
            sincronizando = s.cargando,
            error = s.error,
            soloLectura = sesion?.usuario?.rol?.puedeEditar(Modulo.EQUIPOS) != true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EquiposUiState(sincronizando = true))

    init {
        sincronizar()
    }

    fun sincronizar() {
        if (sync.value.cargando) return
        sync.value = EstadoSync(cargando = true)
        viewModelScope.launch {
            val error = equipoRepository.sincronizar().exceptionOrNull()
            sync.value = EstadoSync(error = error?.let { "${it.message}. Se muestra la copia guardada en el teléfono." })
        }
    }

    fun onFiltro(estado: EstadoEquipo?) {
        filtro.value = estado
    }

    fun onBusqueda(texto: String) {
        busqueda.value = texto
    }
}

// ------------------------------------------------------------------------ detalle

data class EquipoDetalleUiState(
    val cargando: Boolean = true,
    val equipo: Equipo? = null,
    val puedeEditar: Boolean = false,
)

@HiltViewModel
class EquipoDetalleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    equipoRepository: EquipoRepository,
    sessionManager: SessionManager,
) : ViewModel() {

    private val idEquipo: Int = checkNotNull(savedStateHandle[ARG_ID_EQUIPO])

    val ui: StateFlow<EquipoDetalleUiState> = combine(
        equipoRepository.observarEquipo(idEquipo),
        sessionManager.sesion,
    ) { equipo, sesion ->
        EquipoDetalleUiState(
            cargando = false,
            equipo = equipo,
            puedeEditar = sesion?.usuario?.rol?.puedeEditar(Modulo.EQUIPOS) == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EquipoDetalleUiState())
}

// --------------------------------------------------------------------- formulario

/** Campos de EquiposView de escritorio. El código no se edita: lo genera el servidor (EQP00001). */
data class FormularioEquipo(
    val codigo: String = "",
    val nombre: String = "",
    val idTipo: Int = 0,
    val idLaboratorio: Int = 0,
    val marca: String = "",
    val modelo: String = "",
    val serie: String = "",
)

data class EquipoFormUiState(
    val esNuevo: Boolean = true,
    val cargando: Boolean = true,
    val formulario: FormularioEquipo = FormularioEquipo(),
    val estadoActual: EstadoEquipo = EstadoEquipo.DISPONIBLE,
    val tipos: List<TipoEquipo> = emptyList(),
    val laboratorios: List<Laboratorio> = emptyList(),
    val errores: Map<CampoEquipo, String> = emptyMap(),
    val guardando: Boolean = false,
    val error: String? = null,
    /** Evento de un solo uso: id del equipo recién guardado. */
    val guardadoId: Int? = null,
)

@HiltViewModel
class EquipoFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogoRepository: CatalogoRepository,
    private val equipoRepository: EquipoRepository,
    private val guardarEquipo: GuardarEquipoUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val idEquipo: Int = savedStateHandle[ARG_ID_EQUIPO] ?: 0

    private val _ui = MutableStateFlow(EquipoFormUiState(esNuevo = idEquipo == 0))
    val ui: StateFlow<EquipoFormUiState> = _ui.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val tipos = catalogoRepository.tiposEquipo()
            val laboratorios = catalogoRepository.laboratorios()
            val existente = if (idEquipo == 0) null else equipoRepository.observarEquipo(idEquipo).first()
            val fallo = tipos.exceptionOrNull() ?: laboratorios.exceptionOrNull()
            _ui.update { estado ->
                estado.copy(
                    cargando = false,
                    tipos = tipos.getOrDefault(emptyList()).sortedBy { it.nombre },
                    laboratorios = laboratorios.getOrDefault(emptyList()).sortedBy { it.nombre },
                    formulario = existente
                        ?.let {
                            FormularioEquipo(
                                codigo = it.codigoInventario,
                                nombre = it.nombre,
                                idTipo = it.idTipoEquipo,
                                idLaboratorio = it.idLaboratorio,
                                marca = it.marca,
                                modelo = it.modelo,
                                serie = it.numeroSerie,
                            )
                        }
                        ?: estado.formulario,
                    estadoActual = existente?.estado ?: estado.estadoActual,
                    error = when {
                        fallo != null -> "No se pudieron cargar los tipos y laboratorios: ${fallo.message}"
                        idEquipo != 0 && existente == null -> "El equipo no está en la copia local. Actualice la lista."
                        else -> null
                    },
                )
            }
        }
    }

    fun onNombre(valor: String) = editar(CampoEquipo.NOMBRE) { copy(nombre = valor) }
    fun onTipo(id: Int) = editar(CampoEquipo.TIPO) { copy(idTipo = id) }
    fun onLaboratorio(id: Int) = editar(CampoEquipo.LABORATORIO) { copy(idLaboratorio = id) }
    fun onMarca(valor: String) = editar(CampoEquipo.MARCA) { copy(marca = valor) }
    fun onModelo(valor: String) = editar(CampoEquipo.MODELO) { copy(modelo = valor) }
    fun onSerie(valor: String) = editar(CampoEquipo.SERIE) { copy(serie = valor) }

    private fun editar(campo: CampoEquipo, cambio: FormularioEquipo.() -> FormularioEquipo) = _ui.update {
        it.copy(formulario = it.formulario.cambio(), errores = it.errores - campo, error = null)
    }

    fun guardar() {
        val estado = _ui.value
        if (estado.guardando || estado.cargando) return
        val rol = sessionManager.sesion.value?.usuario?.rol
        if (rol == null || !rol.puedeEditar(Modulo.EQUIPOS)) {
            _ui.update { it.copy(error = AppError.SinPermiso().message) }
            return
        }
        val f = estado.formulario
        val equipo = Equipo(
            id = idEquipo,
            codigoInventario = f.codigo,
            nombre = f.nombre,
            idTipoEquipo = f.idTipo,
            tipo = estado.tipos.firstOrNull { it.id == f.idTipo }?.nombre.orEmpty(),
            idLaboratorio = f.idLaboratorio,
            laboratorio = estado.laboratorios.firstOrNull { it.id == f.idLaboratorio }?.nombre.orEmpty(),
            estado = estado.estadoActual,
            marca = f.marca,
            modelo = f.modelo,
            numeroSerie = f.serie,
        )
        val errores = guardarEquipo.validar(equipo)
        if (errores.isNotEmpty()) {
            _ui.update { it.copy(errores = errores.associate { e -> e.campo to e.mensaje }) }
            return
        }

        _ui.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            guardarEquipo(rol, equipo)
                .onSuccess { guardado -> _ui.update { it.copy(guardando = false, guardadoId = guardado.id) } }
                .onFailure { e -> _ui.update { it.copy(guardando = false, error = e.message) } }
        }
    }

    fun navegacionHecha() = _ui.update { it.copy(guardadoId = null) }
}

// ------------------------------------------------------------------------ escáner

data class EscanerUiState(
    val consultando: Boolean = false,
    val codigo: String? = null,
    val error: String? = null,
    /** Evento de un solo uso: equipo encontrado, se navega a su detalle. */
    val encontrado: Equipo? = null,
) {
    /** La cámara solo entrega lecturas cuando no hay una consulta ni un resultado pendiente. */
    val escaneando: Boolean get() = !consultando && error == null && encontrado == null
}

@HiltViewModel
class EscanerQrViewModel @Inject constructor(
    private val consultarEquipoPorQr: ConsultarEquipoPorQrUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(EscanerUiState())
    val ui: StateFlow<EscanerUiState> = _ui.asStateFlow()

    fun onCodigoLeido(texto: String) {
        if (_ui.value.escaneando) consultar(texto)
    }

    fun buscarManual(texto: String) {
        if (!_ui.value.consultando && texto.isNotBlank()) consultar(texto)
    }

    private fun consultar(texto: String) {
        val codigo = texto.trim()
        _ui.update { it.copy(consultando = true, codigo = codigo, error = null) }
        viewModelScope.launch {
            consultarEquipoPorQr(texto)
                .onSuccess { equipo -> _ui.update { it.copy(consultando = false, encontrado = equipo) } }
                // El repositorio ya trae el texto de QrService de escritorio para un código inexistente.
                .onFailure { e -> _ui.update { it.copy(consultando = false, error = e.message) } }
        }
    }

    fun escanearOtraVez() = _ui.update { EscanerUiState() }

    fun navegacionHecha() = _ui.update { it.copy(encontrado = null) }
}
