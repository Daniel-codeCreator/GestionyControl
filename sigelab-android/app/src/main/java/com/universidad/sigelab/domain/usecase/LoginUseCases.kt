package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.PreAutenticacion
import com.universidad.sigelab.domain.model.Sesion
import com.universidad.sigelab.domain.repository.AuditoriaRepository
import com.universidad.sigelab.domain.repository.AuthRepository
import javax.inject.Inject

/*
 * Regla de auditoría del login (cada intento termina en LOGIN_EXITOSO o LOGIN_FALLIDO):
 *  - Si la petición llegó al servidor, el servidor audita (401, 403, rostro no coincide, éxito).
 *  - Si no llegó (sin red, error 5xx) o el fallo es del dispositivo (permiso de cámara,
 *    cancelación), la app audita con AuditoriaRepository, que encola en Room si no hay red.
 * Los motivos usan los mismos textos que Login.java de escritorio.
 */
private fun Throwable.requiereAuditoriaLocal() = this is AppError.SinConexion || this is AppError.Servidor

/** Motivo de escritorio cuando la verificación se corta por un error del cliente. */
fun motivoExcepcionLogin(e: Throwable) = "Excepción de login: ${e.message}"

/** Motivo de escritorio cuando el usuario cancela o cierra la verificación facial. */
const val MOTIVO_VERIFICACION_INTERRUMPIDA = "Proceso de verificación interrumpido"

/** Paso 1: usuario + contraseña. */
class IniciarSesionUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val auditoria: AuditoriaRepository,
) {
    suspend operator fun invoke(usuario: String, contrasena: String): Result<PreAutenticacion> {
        val nombre = usuario.trim()
        if (nombre.isEmpty()) return Result.failure(AppError.Validacion(listOf("Ingrese el usuario.")))
        if (contrasena.isEmpty()) return Result.failure(AppError.Validacion(listOf("Ingrese la contraseña.")))
        return auth.validarCredenciales(nombre, contrasena).onFailure { e ->
            if (e.requiereAuditoriaLocal()) auditoria.registrarLoginFallido(nombre, null, motivoExcepcionLogin(e))
        }
    }
}

/** Paso 2: rostro capturado con la cámara frontal. */
class VerificarRostroUseCase @Inject constructor(
    private val auth: AuthRepository,
    private val auditoria: AuditoriaRepository,
) {
    suspend operator fun invoke(pre: PreAutenticacion, fotoJpeg: ByteArray): Result<Sesion> =
        auth.verificarRostro(pre, fotoJpeg).onFailure { e ->
            if (e.requiereAuditoriaLocal()) {
                auditoria.registrarLoginFallido(pre.usuario.nombreUsuario, pre.usuario.id, motivoExcepcionLogin(e))
            }
        }
}

/** Fallos del paso 2 que ocurren en el dispositivo: permiso negado, error de cámara, cancelación. */
class RegistrarLoginFallidoUseCase @Inject constructor(
    private val auditoria: AuditoriaRepository,
) {
    suspend operator fun invoke(pre: PreAutenticacion, motivo: String) =
        auditoria.registrarLoginFallido(pre.usuario.nombreUsuario, pre.usuario.id, motivo)
}
