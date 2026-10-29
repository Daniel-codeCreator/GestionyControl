package com.universidad.sigelab.core.session

import com.universidad.sigelab.domain.model.Sesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sesión en memoria. No se persiste a propósito: igual que en escritorio, cada apertura
 * de la app exige usuario + contraseña + rostro, y el token no queda guardado en el teléfono.
 */
@Singleton
class SessionManager @Inject constructor() {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    val accessToken: String? get() = _sesion.value?.accessToken

    fun iniciar(sesion: Sesion) {
        _sesion.value = sesion
    }

    fun cerrar() {
        _sesion.value = null
    }
}
