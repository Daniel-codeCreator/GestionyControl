package com.universidad.sigelab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DashboardSnapshotEntity::class,
        EquipoEntity::class,
        AuditoriaPendienteEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class SigelabDatabase : RoomDatabase() {
    abstract fun dashboardDao(): DashboardDao
    abstract fun equipoDao(): EquipoDao
    abstract fun auditoriaPendienteDao(): AuditoriaPendienteDao
}

/**
 * v2: marca, modelo y número de serie del equipo, como en escritorio. Es una migración real y no
 * destructiva para no perder los LOGIN_FALLIDO de auditoria_pendiente que aún no se enviaron.
 */
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (columna in listOf("marca", "modelo", "numeroSerie")) {
            db.execSQL("ALTER TABLE equipos ADD COLUMN $columna TEXT NOT NULL DEFAULT ''")
        }
    }
}
