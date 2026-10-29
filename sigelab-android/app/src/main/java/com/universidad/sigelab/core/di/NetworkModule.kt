package com.universidad.sigelab.core.di

import com.google.gson.Gson
import com.universidad.sigelab.BuildConfig
import com.universidad.sigelab.core.demo.ParaDemo
import com.universidad.sigelab.core.network.AuthInterceptor
import com.universidad.sigelab.data.remote.api.FacialApi
import com.universidad.sigelab.data.remote.api.SigelabApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SigelabRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FacialRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun gson(): Gson = Gson()

    @Provides
    @Singleton
    fun okHttpClient(
        authInterceptor: AuthInterceptor,
        @ParaDemo interceptoresDemo: Set<@JvmSuppressWildcards Interceptor>,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            // Nunca BODY: el log incluiría contraseñas y fotos del rostro.
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        },
                    )
                }
                // Después del token: el servidor simulado lo usa para saber el rol del usuario.
                interceptoresDemo.forEach(::addInterceptor)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            // Generar el embedding facial puede tardar unos segundos en CPU.
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @SigelabRetrofit
    fun sigelabRetrofit(cliente: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SIGELAB_API_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @FacialRetrofit
    fun facialRetrofit(cliente: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.FACIAL_API_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun sigelabApi(@SigelabRetrofit retrofit: Retrofit): SigelabApi = retrofit.create(SigelabApi::class.java)

    @Provides
    @Singleton
    fun facialApi(@FacialRetrofit retrofit: Retrofit): FacialApi = retrofit.create(FacialApi::class.java)
}
