package com.universidad.sigelab.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DashboardMetricasDto(
    @SerializedName("total_equipos") val totalEquipos: Int,
    @SerializedName("disponibles") val disponibles: Int,
    @SerializedName("prestados") val prestados: Int,
    @SerializedName("danados") val danados: Int,
    @SerializedName("en_mantenimiento") val enMantenimiento: Int,
    @SerializedName("de_baja") val deBaja: Int,
    @SerializedName("prestamos_activos") val prestamosActivos: Int,
    @SerializedName("prestamos_vencidos") val prestamosVencidos: Int,
)

/** Tabla Laboratorio. En el alta el código va vacío: el servidor genera LAB-00001. */
data class LaboratorioDto(
    @SerializedName("id_laboratorio") val idLaboratorio: Int?,
    @SerializedName("codigo") val codigo: String?,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("ubicacion") val ubicacion: String?,
    @SerializedName("capacidad") val capacidad: Int,
    @SerializedName("estado") val estado: String?,
)

data class TipoEquipoDto(
    @SerializedName("id_tipo_equipo") val idTipoEquipo: Int?,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
)

/** Columnas de EquipoRepository.listarTodos (escritorio). */
data class EquipoDto(
    @SerializedName("id_equipo") val idEquipo: Int,
    @SerializedName("codigo_inventario") val codigoInventario: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("id_tipo_equipo") val idTipoEquipo: Int,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("id_laboratorio") val idLaboratorio: Int,
    @SerializedName("laboratorio") val laboratorio: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("marca") val marca: String?,
    @SerializedName("modelo") val modelo: String?,
    @SerializedName("numero_serie") val numeroSerie: String?,
)

/**
 * Alta/actualización. No lleva código ni estado: el servidor genera EQP00001, el alta nace
 * DISPONIBLE y el estado solo cambia con préstamos, devoluciones y mantenimientos.
 */
data class EquipoRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("id_tipo_equipo") val idTipoEquipo: Int,
    @SerializedName("id_laboratorio") val idLaboratorio: Int,
    @SerializedName("marca") val marca: String,
    @SerializedName("modelo") val modelo: String,
    @SerializedName("numero_serie") val numeroSerie: String,
)
