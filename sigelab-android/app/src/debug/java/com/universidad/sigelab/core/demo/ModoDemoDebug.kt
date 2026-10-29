package com.universidad.sigelab.core.demo

import android.content.Context
import com.universidad.sigelab.data.local.AuditoriaPendienteDao
import com.universidad.sigelab.data.local.DashboardDao
import com.universidad.sigelab.data.local.EquipoDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** Interruptor del modo demo; se recuerda entre aperturas de la app. */
@Singleton
class ModoDemoDebug @Inject constructor(
    @ApplicationContext contexto: Context,
    private val equipoDao: EquipoDao,
    private val dashboardDao: DashboardDao,
    private val auditoriaDao: AuditoriaPendienteDao,
) : ModoDemo {

    private val preferencias = contexto.getSharedPreferences("modo_demo", Context.MODE_PRIVATE)
    private val _activo = MutableStateFlow(preferencias.getBoolean(CLAVE, false))

    override val disponible = true
    override val activo: StateFlow<Boolean> = _activo.asStateFlow()
    override val usuariosDeEjemplo = ServidorDemo.USUARIOS_DE_EJEMPLO
    override val contrasenaDeEjemplo = ServidorDemo.CONTRASENA

    override suspend fun cambiar(activo: Boolean) {
        if (activo == _activo.value) return
        preferencias.edit().putBoolean(CLAVE, activo).apply()
        // Las listas guardadas en el teléfono no deben mezclar datos de ejemplo con los reales.
        equipoDao.borrarTodo()
        dashboardDao.borrar()
        if (!activo) auditoriaDao.borrarDeDemo()
        _activo.value = activo
    }

    private companion object {
        const val CLAVE = "activo"
    }
}

/**
 * Con el modo demo activo, responde el [ServidorDemo] sin salir a la red. Va después de
 * AuthInterceptor, así recibe el token y el servidor simulado aplica los roles.
 */
class InterceptorDemo @Inject constructor(
    private val modo: ModoDemoDebug,
    private val servidor: ServidorDemo,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!modo.activo.value) return chain.proceed(chain.request())
        // Una pequeña espera para que en la presentación se vean los indicadores de carga.
        Thread.sleep(LATENCIA_MS)
        return servidor.responder(chain.request())
    }

    private companion object {
        const val LATENCIA_MS = 350L
    }
}
