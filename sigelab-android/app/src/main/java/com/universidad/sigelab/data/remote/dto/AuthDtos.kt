package com.universidad.sigelab.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("usuario") val usuario: String,
    @SerializedName("contrasena") val contrasena: String,
)

data class LoginResponse(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre_usuario") val nombreUsuario: String,
    @SerializedName("nombre_completo") val nombreCompleto: String?,
    @SerializedName("rol") val rol: String,
    @SerializedName("pre_auth_token") val preAuthToken: String,
)

/** /face/verify: el rostro que no coincide se responde con 200 y match = false. */
data class FaceVerifyResponse(
    @SerializedName("match") val match: Boolean,
    @SerializedName("score") val score: Double?,
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("detail") val detalle: String?,
)

data class FaceRegisterResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("detail") val detalle: String?,
)

data class EventoAuditoriaRequest(
    @SerializedName("accion") val accion: String,
    @SerializedName("nombre_usuario") val nombreUsuario: String,
    @SerializedName("id_usuario") val idUsuario: Int?,
    @SerializedName("detalle") val detalle: String,
    @SerializedName("fecha_cliente") val fechaCliente: String,
    @SerializedName("origen") val origen: String = "ANDROID",
)

/** Parámetros del procedimiento CrearUsuario; el servidor resuelve el IdRol por nombre. */
data class CrearUsuarioRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("nombre_usuario") val nombreUsuario: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String,
    @SerializedName("rol") val rol: String,
)

data class CrearUsuarioResponse(
    @SerializedName("id_usuario") val idUsuario: Int,
)

/** Columnas de la tabla "USUARIOS REGISTRADOS" (nunca incluye la contraseña). */
data class UsuarioDto(
    @SerializedName("id_usuario") val idUsuario: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("nombre_usuario") val nombreUsuario: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("rol") val rol: String?,
    @SerializedName("estado") val estado: String?,
)
