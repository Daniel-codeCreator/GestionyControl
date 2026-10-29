package com.universidad.sigelab.core.demo

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

/** Release: el modo demo no existe. La versión con el servidor simulado está en src/debug. */
@Module
@InstallIn(SingletonComponent::class)
object ModoDemoModule {

    @Provides
    @Singleton
    fun modoDemo(): ModoDemo = object : ModoDemo {
        override val disponible = false
        override val activo: StateFlow<Boolean> = MutableStateFlow(false)
        override val usuariosDeEjemplo = emptyList<String>()
        override val contrasenaDeEjemplo = ""
        override suspend fun cambiar(activo: Boolean) = Unit
    }
}
