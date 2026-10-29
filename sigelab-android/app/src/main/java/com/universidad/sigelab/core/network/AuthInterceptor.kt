package com.universidad.sigelab.core.network

import com.universidad.sigelab.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agrega "Authorization: Bearer <token>" si hay sesión y la petición no trae ya ese header
 * (/face/verify envía su propio token de pre-autenticación).
 * Un 401 con el token de sesión significa que expiró: se cierra la sesión y la navegación vuelve al login.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionManager.accessToken
        if (token == null || original.header(HEADER) != null) return chain.proceed(original)

        val respuesta = chain.proceed(original.newBuilder().header(HEADER, "Bearer $token").build())
        if (respuesta.code == 401) sessionManager.cerrar()
        return respuesta
    }

    private companion object {
        const val HEADER = "Authorization"
    }
}
