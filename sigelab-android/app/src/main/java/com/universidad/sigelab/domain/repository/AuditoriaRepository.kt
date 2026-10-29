package com.universidad.sigelab.domain.repository

interface AuditoriaRepository {
    /**
     * Registra un LOGIN_FALLIDO que ocurrió en el dispositivo sin llegar al servidor
     * (sin red, permiso de cámara negado, verificación cancelada…).
     * Nunca falla: si no hay red, el evento se guarda en Room y se reenvía después.
     */
    suspend fun registrarLoginFallido(nombreUsuario: String, idUsuario: Int?, motivo: String)

    /** Reenvía los eventos encolados. Devuelve true si no queda ninguno pendiente. */
    suspend fun enviarPendientes(): Boolean
}
