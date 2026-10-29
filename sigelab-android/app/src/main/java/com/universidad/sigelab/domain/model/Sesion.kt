package com.universidad.sigelab.domain.model

data class Usuario(
    val id: Int,
    val nombreUsuario: String,
    val nombreCompleto: String,
    val rol: Rol,
)

/** Resultado del paso 1 (credenciales OK y usuario ACTIVO). Falta validar el rostro. */
data class PreAutenticacion(
    val usuario: Usuario,
    /** Token de corta duración que solo sirve para llamar a /face/verify. */
    val token: String,
)

/** Sesión completa: ambos factores superados. */
data class Sesion(
    val usuario: Usuario,
    val accessToken: String,
)

/** Datos del procedimiento CrearUsuario; el usuario nace ACTIVO. */
data class NuevoUsuario(
    val nombre: String,
    val apellido: String,
    val nombreUsuario: String,
    val correo: String,
    val contrasena: String,
    val rol: Rol,
)

/** Fila de la tabla "USUARIOS REGISTRADOS" de escritorio. */
data class UsuarioRegistrado(
    val id: Int,
    val nombreCompleto: String,
    val nombreUsuario: String,
    val correo: String,
    /** null si el IdRol no corresponde a ADMIN, ENCARGADO ni DOCENTE. */
    val rol: Rol?,
    val estado: EstadoRegistro,
)

data class ResultadoRegistroUsuario(
    val idUsuario: Int,
    val rostroRegistrado: Boolean,
    /** Si el usuario se creó pero el rostro falló, permite reintentar solo el registro facial. */
    val errorRostro: String?,
)
