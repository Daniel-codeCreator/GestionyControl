package com.universidad.sigelab.ui.usuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.NuevoUsuario
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.UsuarioRegistrado
import com.universidad.sigelab.domain.usecase.CampoUsuario
import com.universidad.sigelab.domain.usecase.RegistrarUsuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormularioUsuario(
    val nombre: String = "",
    val apellido: String = "",
    val nombreUsuario: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val rol: Rol = Rol.ENCARGADO,
) {
    /** Como el procedimiento CrearUsuario de escritorio, el usuario siempre nace ACTIVO. */
    fun aNuevoUsuario() = NuevoUsuario(
        nombre = nombre.trim(),
        apellido = apellido.trim(),
        nombreUsuario = nombreUsuario.trim(),
        correo = correo.trim(),
        contrasena = contrasena,
        rol = rol,
    )
}

/** Tabla "USUARIOS REGISTRADOS" de escritorio. */
data class UsuariosRegistradosUi(
    val lista: List<UsuarioRegistrado> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
)

/** JPEG de la cámara. Clase normal (no data) para comparar por identidad y no por contenido. */
class FotoCapturada(val jpeg: ByteArray)

sealed interface PasoCrearUsuario {
    data object Datos : PasoCrearUsuario
    data object Rostro : PasoCrearUsuario
    data class Listo(val idUsuario: Int, val nombreUsuario: String) : PasoCrearUsuario
}

data class CrearUsuarioUiState(
    val paso: PasoCrearUsuario = PasoCrearUsuario.Datos,
    val formulario: FormularioUsuario = FormularioUsuario(),
    val erroresCampo: Map<CampoUsuario, String> = emptyMap(),
    val errorConfirmacion: String? = null,
    val foto: FotoCapturada? = null,
    /** Distinto de null cuando el usuario ya existe en SQL Server pero su rostro aún no se registró. */
    val idUsuarioCreado: Int? = null,
    val enviando: Boolean = false,
    val error: String? = null,
)

/**
 * Flujo del diagrama "ADMIN crea el usuario → registro del reconocimiento facial":
 * datos → foto → POST usuarios + POST face/register. Si el rostro falla, el usuario ya
 * creado no se repite: solo se reintenta el registro facial con otra foto.
 */
@HiltViewModel
class CrearUsuarioViewModel @Inject constructor(
    private val registrarUsuario: RegistrarUsuarioUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(CrearUsuarioUiState())
    val ui: StateFlow<CrearUsuarioUiState> = _ui.asStateFlow()

    private val _registrados = MutableStateFlow(UsuariosRegistradosUi())
    val registrados: StateFlow<UsuariosRegistradosUi> = _registrados.asStateFlow()

    init {
        cargarUsuarios()
    }

    fun cargarUsuarios() {
        val rol = sessionManager.sesion.value?.usuario?.rol ?: return
        _registrados.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = registrarUsuario.listar(rol)
            _registrados.update {
                UsuariosRegistradosUi(
                    lista = resultado.getOrNull()?.sortedBy { u -> u.id } ?: it.lista,
                    error = resultado.exceptionOrNull()?.message,
                )
            }
        }
    }

    // --- Paso 1: datos ---

    fun onNombre(valor: String) = editar(CampoUsuario.NOMBRE) { copy(nombre = valor) }
    fun onApellido(valor: String) = editar(CampoUsuario.APELLIDO) { copy(apellido = valor) }
    fun onNombreUsuario(valor: String) = editar(CampoUsuario.NOMBRE_USUARIO) { copy(nombreUsuario = valor) }
    fun onCorreo(valor: String) = editar(CampoUsuario.CORREO) { copy(correo = valor) }
    fun onContrasena(valor: String) = editar(CampoUsuario.CONTRASENA) { copy(contrasena = valor) }
    fun onRol(rol: Rol) = editar(null) { copy(rol = rol) }

    fun onConfirmacion(valor: String) = _ui.update {
        it.copy(formulario = it.formulario.copy(confirmacion = valor), errorConfirmacion = null, error = null)
    }

    private fun editar(campo: CampoUsuario?, cambio: FormularioUsuario.() -> FormularioUsuario) = _ui.update {
        it.copy(
            formulario = it.formulario.cambio(),
            erroresCampo = if (campo == null) it.erroresCampo else it.erroresCampo - campo,
            error = null,
        )
    }

    fun continuarARostro() {
        val formulario = _ui.value.formulario
        val errores = registrarUsuario.validar(formulario.aNuevoUsuario()).associate { it.campo to it.mensaje }
        val errorConfirmacion = if (formulario.confirmacion != formulario.contrasena) "Las contraseñas no coinciden" else null
        if (errores.isNotEmpty() || errorConfirmacion != null) {
            _ui.update {
                it.copy(erroresCampo = errores, errorConfirmacion = errorConfirmacion, error = "Revise los campos marcados")
            }
            return
        }
        _ui.update { it.copy(paso = PasoCrearUsuario.Rostro, error = null) }
    }

    fun volverADatos() {
        val estado = _ui.value
        if (estado.enviando || estado.idUsuarioCreado != null) return
        _ui.update { it.copy(paso = PasoCrearUsuario.Datos, error = null) }
    }

    // --- Paso 2: rostro ---

    fun onFotoCapturada(jpeg: ByteArray) = _ui.update { it.copy(foto = FotoCapturada(jpeg), error = null) }

    fun repetirFoto() {
        if (_ui.value.enviando) return
        _ui.update { it.copy(foto = null, error = null) }
    }

    fun onErrorCamara(mensaje: String) = _ui.update { it.copy(error = "No se pudo usar la cámara: $mensaje") }

    fun registrar() {
        val estado = _ui.value
        val foto = estado.foto ?: return
        if (estado.enviando) return
        val rolActual = sessionManager.sesion.value?.usuario?.rol
        if (rolActual == null) {
            _ui.update { it.copy(error = AppError.SesionExpirada().message) }
            return
        }
        _ui.update { it.copy(enviando = true, error = null) }

        viewModelScope.launch {
            val idExistente = estado.idUsuarioCreado
            if (idExistente == null) {
                registrarUsuario(rolActual, estado.formulario.aNuevoUsuario(), foto.jpeg)
                    .onSuccess { r ->
                        if (r.rostroRegistrado) listo(r.idUsuario) else rostroPendiente(r.idUsuario, r.errorRostro)
                    }
                    .onFailure(::falloAlCrear)
            } else {
                registrarUsuario.reintentarRostro(rolActual, idExistente, foto.jpeg)
                    .onSuccess { listo(idExistente) }
                    .onFailure { e -> rostroPendiente(idExistente, e.message) }
            }
        }
    }

    private fun rostroPendiente(idUsuario: Int, motivo: String?) {
        _ui.update {
            it.copy(
                enviando = false,
                idUsuarioCreado = idUsuario,
                // El usuario ya existe: la contraseña no se vuelve a necesitar.
                formulario = it.formulario.copy(contrasena = "", confirmacion = ""),
                error = "El rostro no se registró${motivo?.let { m -> ": $m" }.orEmpty()}. Reintente o repita la foto.",
            )
        }
        cargarUsuarios()
    }

    private fun falloAlCrear(error: Throwable) = _ui.update {
        when (error) {
            // Datos rechazados por el servidor (p. ej. usuario repetido): se corrigen en el paso 1.
            is AppError.Validacion, is AppError.Conflicto ->
                it.copy(enviando = false, paso = PasoCrearUsuario.Datos, error = error.message)
            else -> it.copy(enviando = false, error = error.message)
        }
    }

    private fun listo(idUsuario: Int) {
        _ui.update { CrearUsuarioUiState(paso = PasoCrearUsuario.Listo(idUsuario, it.formulario.nombreUsuario.trim())) }
        cargarUsuarios()
    }

    fun crearOtro() = _ui.update { CrearUsuarioUiState() }
}
