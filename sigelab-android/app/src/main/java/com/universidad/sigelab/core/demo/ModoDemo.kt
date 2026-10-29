package com.universidad.sigelab.core.demo

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Qualifier

/**
 * Modo demo: un servidor simulado dentro de la app responde en lugar de la API, con datos de
 * ejemplo, para recorrer y presentar las pantallas sin backend. Solo existe en las compilaciones
 * debug (src/debug); en release [disponible] es false y el APK no lleva ningún código del demo.
 */
interface ModoDemo {
    val disponible: Boolean
    val activo: StateFlow<Boolean>

    /** Usuarios de ejemplo para mostrar en el login, por ejemplo "admin.demo · ADMIN". */
    val usuariosDeEjemplo: List<String>
    val contrasenaDeEjemplo: String

    /** Cambia de modo y vacía las cachés para no mezclar datos de ejemplo con los reales. */
    suspend fun cambiar(activo: Boolean)
}

/** Interceptores de OkHttp que solo aporta la compilación debug (el servidor simulado). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ParaDemo
