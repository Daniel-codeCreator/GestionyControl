package com.universidad.sigelab.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universidad.sigelab.core.demo.ModoDemo
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.PreAutenticacion
import com.universidad.sigelab.domain.usecase.IniciarSesionUseCase
import com.universidad.sigelab.domain.usecase.MOTIVO_VERIFICACION_INTERRUMPIDA
import com.universidad.sigelab.domain.usecase.RegistrarLoginFallidoUseCase
import com.universidad.sigelab.domain.usecase.VerificarRostroUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PasoLogin {
    data object Credenciales : PasoLogin
    data class Rostro(val pre: PreAutenticacion) : PasoLogin
}

data class LoginUiState(
    val paso: PasoLogin = PasoLogin.Credenciales,
    val usuario: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val intentosRestantes: Int = MAX_INTENTOS_ROSTRO,
)

const val MAX_INTENTOS_ROSTRO = 3

/**
 * Login de dos factores. Al validar el rostro, el repositorio guarda la sesión en
 * SessionManager y la navegación pasa sola al Dashboard: esta pantalla no navega.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val iniciarSesion: IniciarSesionUseCase,
    private val verificarRostro: VerificarRostroUseCase,
    private val registrarLoginFallido: RegistrarLoginFallidoUseCase,
    private val modoDemo: ModoDemo,
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    /** Solo en compilaciones debug: datos de ejemplo en lugar del servidor. */
    val demoDisponible: Boolean = modoDemo.disponible
    val demoActivo: StateFlow<Boolean> = modoDemo.activo
    val usuariosDemo: List<String> = modoDemo.usuariosDeEjemplo
    val contrasenaDemo: String = modoDemo.contrasenaDeEjemplo

    fun onModoDemo(activo: Boolean) {
        if (_ui.value.cargando) return
        _ui.update { it.copy(error = null) }
        viewModelScope.launch { modoDemo.cambiar(activo) }
    }

    private var permisoNegadoAuditado = false

    fun onUsuarioCambia(valor: String) = _ui.update { it.copy(usuario = valor, error = null) }

    fun onContrasenaCambia(valor: String) = _ui.update { it.copy(contrasena = valor, error = null) }

    /** Paso 1: usuario + contraseña contra SQL Server (BCrypt y estado ACTIVO en el servidor). */
    fun continuar() {
        val estado = _ui.value
        if (estado.cargando || estado.paso !is PasoLogin.Credenciales) return
        _ui.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            iniciarSesion(estado.usuario, estado.contrasena)
                .onSuccess { pre ->
                    permisoNegadoAuditado = false
                    // La contraseña no se conserva en memoria más de lo necesario.
                    _ui.update {
                        it.copy(
                            paso = PasoLogin.Rostro(pre),
                            contrasena = "",
                            cargando = false,
                            intentosRestantes = MAX_INTENTOS_ROSTRO,
                        )
                    }
                }
                .onFailure { e -> _ui.update { it.copy(cargando = false, error = e.message) } }
        }
    }

    /** Paso 2: foto de la cámara frontal → /face/verify. */
    fun onFotoCapturada(jpeg: ByteArray) {
        val estado = _ui.value
        val paso = estado.paso as? PasoLogin.Rostro ?: return
        if (estado.cargando) return
        _ui.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            verificarRostro(paso.pre, jpeg)
                .onSuccess { _ui.update { it.copy(cargando = false) } }
                .onFailure(::manejarFalloRostro)
        }
    }

    private fun manejarFalloRostro(error: Throwable) {
        when (error) {
            is AppError.RostroNoValidado -> {
                val restantes = _ui.value.intentosRestantes - 1
                if (restantes <= 0) {
                    volverACredenciales("Rostro no validado en $MAX_INTENTOS_ROSTRO intentos. Inicie sesión de nuevo.")
                } else {
                    _ui.update {
                        it.copy(cargando = false, intentosRestantes = restantes, error = error.message)
                    }
                }
            }
            is AppError.SesionExpirada ->
                volverACredenciales("La verificación tardó demasiado. Ingrese sus credenciales otra vez.")
            else -> _ui.update { it.copy(cargando = false, error = error.message) }
        }
    }

    fun onErrorCamara(mensaje: String) {
        val paso = _ui.value.paso as? PasoLogin.Rostro ?: return
        _ui.update { it.copy(error = "No se pudo usar la cámara: $mensaje") }
        viewModelScope.launch { registrarLoginFallido(paso.pre, "Excepción de login: Error de cámara: $mensaje") }
    }

    fun onPermisoCamaraDenegado() {
        val paso = _ui.value.paso as? PasoLogin.Rostro ?: return
        _ui.update { it.copy(error = "Se necesita la cámara para validar su rostro.") }
        if (permisoNegadoAuditado) return
        permisoNegadoAuditado = true
        viewModelScope.launch { registrarLoginFallido(paso.pre, "$MOTIVO_VERIFICACION_INTERRUMPIDA (permiso de cámara denegado)") }
    }

    fun cancelarVerificacion() {
        val estado = _ui.value
        val paso = estado.paso as? PasoLogin.Rostro ?: return
        if (estado.cargando) return
        viewModelScope.launch { registrarLoginFallido(paso.pre, MOTIVO_VERIFICACION_INTERRUMPIDA) }
        volverACredenciales(null)
    }

    private fun volverACredenciales(mensaje: String?) =
        _ui.update { LoginUiState(usuario = it.usuario, error = mensaje) }
}
