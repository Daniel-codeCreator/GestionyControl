package com.universidad.sigelab.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboard_snapshot WHERE id = 0")
    fun observar(): Flow<DashboardSnapshotEntity?>

    @Upsert
    suspend fun guardar(snapshot: DashboardSnapshotEntity)

    @Query("DELETE FROM dashboard_snapshot")
    suspend fun borrar()
}

@Dao
interface EquipoDao {
    @Query("SELECT * FROM equipos ORDER BY codigoInventario")
    fun observarTodos(): Flow<List<EquipoEntity>>

    /** [estado] es el nombre del enum (Room guarda los enum como TEXT). */
    @Query("SELECT * FROM equipos WHERE estado = :estado ORDER BY codigoInventario")
    fun observarPorEstado(estado: String): Flow<List<EquipoEntity>>

    @Query("SELECT * FROM equipos WHERE id = :id")
    fun observarPorId(id: Int): Flow<EquipoEntity?>

    @Query("SELECT * FROM equipos WHERE codigoInventario = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): EquipoEntity?

    @Upsert
    suspend fun guardar(equipos: List<EquipoEntity>)

    @Query("DELETE FROM equipos")
    suspend fun borrarTodo()

    @Transaction
    suspend fun reemplazarTodo(equipos: List<EquipoEntity>) {
        borrarTodo()
        guardar(equipos)
    }
}

@Dao
interface AuditoriaPendienteDao {
    @Insert
    suspend fun insertar(evento: AuditoriaPendienteEntity)

    @Query("SELECT * FROM auditoria_pendiente ORDER BY id LIMIT :limite")
    suspend fun pendientes(limite: Int): List<AuditoriaPendienteEntity>

    @Query("SELECT COUNT(*) FROM auditoria_pendiente")
    suspend fun contar(): Int

    @Delete
    suspend fun eliminar(evento: AuditoriaPendienteEntity)

    /** Eventos de los usuarios de ejemplo del modo demo (terminan en ".demo"): nunca van al servidor real. */
    @Query("DELETE FROM auditoria_pendiente WHERE nombreUsuario LIKE '%.demo'")
    suspend fun borrarDeDemo()
}
