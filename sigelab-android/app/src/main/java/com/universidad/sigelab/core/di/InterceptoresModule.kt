package com.universidad.sigelab.core.di

import com.universidad.sigelab.core.demo.ParaDemo
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptoresModule {

    /** Vacío en release; en debug lo llena el modo demo. */
    @Multibinds
    @ParaDemo
    abstract fun interceptoresDemo(): Set<Interceptor>
}
