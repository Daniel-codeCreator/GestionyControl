package com.universidad.sigelab.core.demo

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Interceptor

/** Debug: modo demo con el servidor simulado. En release este módulo no existe (ver src/release). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModoDemoModule {

    @Binds
    abstract fun modoDemo(impl: ModoDemoDebug): ModoDemo

    @Binds
    @IntoSet
    @ParaDemo
    abstract fun interceptorDemo(impl: InterceptorDemo): Interceptor
}
