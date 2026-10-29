package com.universidad.sigelab.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Fila de la tabla Auditoria con el NombreUsuario resuelto. */
data class RegistroAuditoriaDto(
    @SerializedName("id_auditoria") val idAuditoria: Long,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("accion") val accion: String,
    @SerializedName("entidad") val entidad: String?,
    @SerializedName("usuario") val usuario: String?,
    @SerializedName("detalle") val detalle: String?,
)
