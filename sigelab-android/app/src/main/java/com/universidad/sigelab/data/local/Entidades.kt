package com.universidad.sigelab.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.universidad.sigelab.domain.model.EstadoEquipo

/** Última respuesta de /dashboard/metricas; una sola fila (id = 0). */
@Entity(tableName = "dashboard_snapshot")
data class DashboardSnapshotEntity(
    @PrimaryKey val id: Int = 0,
    val totalEquipos: Int,
    val disponibles: Int,
    val prestados: Int,
    val danados: Int,
    val enMantenimiento: Int,
    val deBaja: Int,
    val prestamosActivos: Int,
    val prestamosVencidos: Int,
    val actualizadoEnEpochMs: Long,
)

/** Copia local de equipos para consultas y lectura de QR sin red. SQL Server sigue siendo la fuente de verdad. */
@Entity(
    tableName = "equipos",
    indices = [Index(value = ["codigoInventario"], unique = true), Index(value = ["estado"])],
)
data class EquipoEntity(
    @PrimaryKey val id: Int,
    val codigoInventario: String,
    val nombre: String,
    val idTipoEquipo: Int,
    val tipo: String,
    val idLaboratorio: Int,
    val laboratorio: String,
    val estado: EstadoEquipo,
    // Agregadas en la versión 2 (ver MIGRACION_1_2); el valor por defecto debe coincidir con la migración.
    @ColumnInfo(defaultValue = "") val marca: String = "",
    @ColumnInfo(defaultValue = "") val modelo: String = "",
    @ColumnInfo(defaultValue = "") val numeroSerie: String = "",
)

/** Bandeja de salida de auditoría: eventos LOGIN_FALLIDO que aún no llegan al servidor. */
@Entity(tableName = "auditoria_pendiente")
data class AuditoriaPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accion: String,
    val nombreUsuario: String,
    val idUsuario: Int?,
    val detalle: String,
    /** ISO-8601 UTC del momento en que ocurrió en el dispositivo. */
    val fechaCliente: String,
)
