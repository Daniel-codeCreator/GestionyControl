package com.universidad.sigelab.data.repository

import com.universidad.sigelab.core.network.apiCall
import com.universidad.sigelab.core.session.SessionManager
import com.universidad.sigelab.data.remote.api.FacialApi
import com.universidad.sigelab.data.remote.api.SigelabApi
import com.universidad.sigelab.data.remote.api.comoParteJpeg
import com.universidad.sigelab.data.remote.api.comoParteTexto
import com.universidad.sigelab.data.remote.dto.LoginRequest
import com.universidad.sigelab.data.toDomain
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.PreAutenticacion
import com.universidad.sigelab.domain.model.Sesion
import com.universidad.sigelab.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
    private val facialApi: FacialApi,
    private val sessionManager: SessionManager,
) : AuthRepository {

    /**
     * La contraseña viaja en claro dentro de HTTPS y el servidor la compara con BCrypt.
     * El hash nunca sale de SQL Server: compararlo en el teléfono obligaría a descargarlo.
     */
    override suspend fun validarCredenciales(usuario: String, contrasena: String): Result<PreAutenticacion> =
        apiCall(
            alErrorHttp = { codigo, _ ->
                when (codigo) {
                    401 -> AppError.CredencialesInvalidas()
                    403 -> AppError.UsuarioInactivo()
                    else -> null
                }
            },
        ) {
            api.login(LoginRequest(usuario, contrasena)).toDomain()
        }

    override suspend fun verificarRostro(pre: PreAutenticacion, fotoJpeg: ByteArray): Result<Sesion> =
        apiCall(
            alErrorHttp = { codigo, detalle ->
                when (codigo) {
                    // El token del paso 1 dura pocos minutos: hay que reiniciar el login.
                    401 -> AppError.SesionExpirada()
                    404 -> AppError.NoEncontrado(detalle ?: "El usuario no tiene un rostro registrado")
                    422 -> AppError.Validacion(listOf(detalle ?: "No se detectó un rostro en la imagen"))
                    else -> null
                }
            },
        ) {
            val respuesta = facialApi.verificar(
                autorizacion = "Bearer ${pre.token}",
                idUsuario = pre.usuario.id.comoParteTexto(),
                imagen = fotoJpeg.comoParteJpeg(),
            )
            val token = respuesta.accessToken
            if (!respuesta.match || token.isNullOrBlank()) throw AppError.RostroNoValidado(respuesta.score)
            Sesion(pre.usuario, token)
        }.onSuccess(sessionManager::iniciar)

    /** El token es un JWT sin estado en el servidor: basta con olvidarlo. */
    override suspend fun cerrarSesion() {
        sessionManager.cerrar()
    }
}
