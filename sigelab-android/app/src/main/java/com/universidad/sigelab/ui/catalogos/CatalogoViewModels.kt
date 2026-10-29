package com.universidad.sigelab.ui.catalogos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.TipoEquipo
import com.universidad.sigelab.domain.repository.CatalogoRepository
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.usecase.CampoCatalogo
import com.universidad.sigelab.domain.usecase.CrearTipoEquipoUseCase
import com.universidad.sigelab.domain.usecase.ErrorCatalogo
import com.universidad.sigelab.domain.usecase.GuardarLaboratorioUseCase
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

data class CatalogoUiState<T>(
    val items: List<T> = emptyList(),
    /** Equipos de la copia local que usan cada elemento (id → cantidad). */
    val equiposPorItem: Map<Int, Int> = emptyMap(),
    val cargando: Boolean = true,
    val error: String? = null,
    /** Elemento del formulario abierto (id 0 = nuevo); null si está cerrado. */
    val editando: T? = null,
    val erroresCampo: Map<CampoCatalogo, String> = emptyMap(),
    val enviando: Boolean = false,
    val errorDialogo: String? = null,
)

/**
 * Lógica común de los catálogos base: listar, crear y (solo laboratorios) actualizar, igual que
 * las pantallas de escritorio. Ninguno se elimina: un laboratorio se da de baja con estado INACTIVO.
 * Las subclases llaman a [cargar] en su propio init (sus dependencias aún no existen en el de esta clase).
 */
abstract class CatalogoViewModel<T : Any>(
    private val equipoRepository: EquipoRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    /** Campo donde se muestra un 409 del servidor (valor repetido). */
    protected abstract val campoUnico: CampoCatalogo

    protected abstract fun vacio(): T
    protected abstract fun idDe(item: T): Int
    protected abstract fun ordenDe(item: T): String
    protected abstract fun idEnEquipo(equipo: Equipo): Int
    protected abstract fun validar(item: T, existentes: List<T>): List<ErrorCatalogo>
    protected abstract fun mensajeGuardado(guardado: T, esNuevo: Boolean): String
    protected abstract suspend fun listarDelServidor(): Result<List<T>>
    protected abstract suspend fun guardarEnServidor(rol: Rol, item: T, existentes: List<T>): Result<T>

    private val estado = MutableStateFlow(CatalogoUiState<T>())

    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    val ui: StateFlow<CatalogoUiState<T>> = combine(estado, equipoRepository.observarEquipos()) { s, equipos ->
        s.copy(equiposPorItem = equipos.groupingBy { idEnEquipo(it) }.eachCount())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogoUiState())

    fun cargar() {
        estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val sincronizacion = async { equipoRepository.sincronizar() }
            val resultado = listarDelServidor()
            sincronizacion.await()
            estado.update {
                it.copy(
                    cargando = false,
                    items = resultado.getOrNull()?.sortedBy { item -> ordenDe(item).lowercase() } ?: it.items,
                    error = resultado.exceptionOrNull()?.message,
                )
            }
        }
    }

    // --- formulario ---

    fun abrirNuevo() = abrirFormulario(vacio())

    fun abrirFormulario(item: T) =
        estado.update { it.copy(editando = item, erroresCampo = emptyMap(), errorDialogo = null) }

    fun cerrarFormulario() {
        if (estado.value.enviando) return
        estado.update { it.copy(editando = null, erroresCampo = emptyMap(), errorDialogo = null) }
    }

    fun limpiarError(campo: CampoCatalogo) = estado.update { it.copy(erroresCampo = it.erroresCampo - campo) }

    /** Error que detecta la pantalla antes de armar el objeto (por ejemplo, capacidad no numérica). */
    fun mostrarError(campo: CampoCatalogo, mensaje: String) =
        estado.update { it.copy(erroresCampo = it.erroresCampo + (campo to mensaje)) }

    fun guardar(item: T) {
        val actual = estado.value
        if (actual.enviando) return
        val rol = rolActual() ?: return
        val errores = validar(item, actual.items)
        if (errores.isNotEmpty()) {
            estado.update { it.copy(erroresCampo = errores.associate { e -> e.campo to e.mensaje }) }
            return
        }

        estado.update { it.copy(enviando = true, errorDialogo = null) }
        viewModelScope.launch {
            guardarEnServidor(rol, item, actual.items)
                .onSuccess { guardado ->
                    estado.update {
                        it.copy(
                            enviando = false,
                            editando = null,
                            items = (it.items.filterNot { i -> idDe(i) == idDe(guardado) } + guardado)
                                .sortedBy { i -> ordenDe(i).lowercase() },
                        )
                    }
                    val esNuevo = idDe(item) == 0
                    _mensajes.send(mensajeGuardado(guardado, esNuevo))
                    // Los equipos guardados en el teléfono muestran este nombre: se refrescan.
                    if (!esNuevo) equipoRepository.sincronizar()
                }
                .onFailure { e ->
                    estado.update {
                        if (e is AppError.Conflicto) {
                            it.copy(enviando = false, erroresCampo = it.erroresCampo + (campoUnico to (e.message ?: "Valor repetido")))
                        } else {
                            it.copy(enviando = false, errorDialogo = e.message)
                        }
                    }
                }
        }
    }

    private fun rolActual(): Rol? {
        val rol = sessionManager.sesion.value?.usuario?.rol
        if (rol == null) estado.update { it.copy(errorDialogo = AppError.SesionExpirada().message) }
        return rol
    }
}

@HiltViewModel
class LaboratoriosViewModel @Inject constructor(
    private val catalogoRepository: CatalogoRepository,
    private val guardarLaboratorio: GuardarLaboratorioUseCase,
    equipoRepository: EquipoRepository,
    sessionManager: SessionManager,
) : CatalogoViewModel<Laboratorio>(equipoRepository, sessionManager) {

    override val campoUnico get() = CampoCatalogo.NOMBRE

    override fun vacio() = Laboratorio(id = 0, codigo = "", nombre = "", ubicacion = "", capacidad = 0)
    override fun idDe(item: Laboratorio) = item.id
    override fun ordenDe(item: Laboratorio) = item.codigo
    override fun idEnEquipo(equipo: Equipo) = equipo.idLaboratorio
    override fun validar(item: Laboratorio, existentes: List<Laboratorio>) = guardarLaboratorio.validar(item)
    override suspend fun listarDelServidor() = catalogoRepository.laboratorios()

    /** Mismos mensajes que LaboratorioView de escritorio. */
    override fun mensajeGuardado(guardado: Laboratorio, esNuevo: Boolean) =
        if (esNuevo) "Laboratorio registrado exitosamente. Código asignado: ${guardado.codigo}" else "Laboratorio actualizado exitosamente."

    override suspend fun guardarEnServidor(rol: Rol, item: Laboratorio, existentes: List<Laboratorio>) =
        guardarLaboratorio(rol, item)

    init {
        cargar()
    }
}

@HiltViewModel
class TiposEquipoViewModel @Inject constructor(
    private val catalogoRepository: CatalogoRepository,
    private val crearTipoEquipo: CrearTipoEquipoUseCase,
    equipoRepository: EquipoRepository,
    sessionManager: SessionManager,
) : CatalogoViewModel<TipoEquipo>(equipoRepository, sessionManager) {

    override val campoUnico get() = CampoCatalogo.NOMBRE

    override fun vacio() = TipoEquipo(id = 0, nombre = "", descripcion = null)
    override fun idDe(item: TipoEquipo) = item.id
    override fun ordenDe(item: TipoEquipo) = item.nombre
    override fun idEnEquipo(equipo: Equipo) = equipo.idTipoEquipo
    override fun validar(item: TipoEquipo, existentes: List<TipoEquipo>) = crearTipoEquipo.validar(item, existentes)
    override suspend fun listarDelServidor() = catalogoRepository.tiposEquipo()

    override fun mensajeGuardado(guardado: TipoEquipo, esNuevo: Boolean) = "Tipo de equipo guardado correctamente."

    override suspend fun guardarEnServidor(rol: Rol, item: TipoEquipo, existentes: List<TipoEquipo>) =
        crearTipoEquipo(rol, item, existentes)

    init {
        cargar()
    }
}
