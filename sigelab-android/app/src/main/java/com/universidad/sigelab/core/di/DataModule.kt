package com.universidad.sigelab.core.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.universidad.sigelab.data.local.AuditoriaPendienteDao
import com.universidad.sigelab.data.local.DashboardDao
import com.universidad.sigelab.data.local.EquipoDao
import com.universidad.sigelab.data.local.MIGRACION_1_2
import com.universidad.sigelab.data.local.SigelabDatabase
import com.universidad.sigelab.data.repository.AuditoriaRepositoryImpl
import com.universidad.sigelab.data.repository.AuthRepositoryImpl
import com.universidad.sigelab.data.repository.CatalogoRepositoryImpl
import com.universidad.sigelab.data.repository.DashboardRepositoryImpl
import com.universidad.sigelab.data.repository.EquipoRepositoryImpl
import com.universidad.sigelab.data.repository.MantenimientoRepositoryImpl
import com.universidad.sigelab.data.repository.PrestamoRepositoryImpl
import com.universidad.sigelab.data.repository.ReporteRepositoryImpl
import com.universidad.sigelab.data.repository.UsuarioRepositoryImpl
import com.universidad.sigelab.domain.repository.AuditoriaRepository
import com.universidad.sigelab.domain.repository.AuthRepository
import com.universidad.sigelab.domain.repository.CatalogoRepository
import com.universidad.sigelab.domain.repository.DashboardRepository
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.repository.MantenimientoRepository
import com.universidad.sigelab.domain.repository.PrestamoRepository
import com.universidad.sigelab.domain.repository.ReporteRepository
import com.universidad.sigelab.domain.repository.UsuarioRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /** Solo caché y bandeja de auditoría: ante un cambio de esquema se puede recrear. */
    @Provides
    @Singleton
    fun database(@ApplicationContext contexto: Context): SigelabDatabase =
        Room.databaseBuilder(contexto, SigelabDatabase::class.java, "sigelab.db")
            .addMigrations(MIGRACION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun dashboardDao(db: SigelabDatabase): DashboardDao = db.dashboardDao()

    @Provides
    fun equipoDao(db: SigelabDatabase): EquipoDao = db.equipoDao()

    @Provides
    fun auditoriaPendienteDao(db: SigelabDatabase): AuditoriaPendienteDao = db.auditoriaPendienteDao()

    @Provides
    @Singleton
    fun workManager(@ApplicationContext contexto: Context): WorkManager = WorkManager.getInstance(contexto)

    @Provides
    fun reloj(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun auditoria(impl: AuditoriaRepositoryImpl): AuditoriaRepository
    @Binds abstract fun dashboard(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds abstract fun catalogo(impl: CatalogoRepositoryImpl): CatalogoRepository
    @Binds abstract fun equipo(impl: EquipoRepositoryImpl): EquipoRepository
    @Binds abstract fun prestamo(impl: PrestamoRepositoryImpl): PrestamoRepository
    @Binds abstract fun mantenimiento(impl: MantenimientoRepositoryImpl): MantenimientoRepository
    @Binds abstract fun usuario(impl: UsuarioRepositoryImpl): UsuarioRepository
    @Binds abstract fun reporte(impl: ReporteRepositoryImpl): ReporteRepository
}
