package com.universidad.sigelab.domain.repository

import com.universidad.sigelab.domain.model.PreAutenticacion
import com.universidad.sigelab.domain.model.Sesion

interface AuthRepository {
    /** Paso 1. El servidor compara con BCrypt, exige estado ACTIVO y audita LOGIN_FALLIDO si falla. */
    suspend fun validarCredenciales(usuario: String, contrasena: String): Result<PreAutenticacion>

    /** Paso 2. Envía la foto a /face/verify; el servidor audita LOGIN_EXITOSO o LOGIN_FALLIDO. */
    suspend fun verificarRostro(pre: PreAutenticacion, fotoJpeg: ByteArray): Result<Sesion>

    suspend fun cerrarSesion()
}
