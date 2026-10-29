package com.universidad.sigelab.domain.model

/**
 * Errores que la UI sabe mostrar. Los repositorios devuelven Result.failure(AppError).
 * Los textos de login y permisos son los de escritorio; como allí, no se distingue entre
 * contraseña incorrecta y usuario inactivo para no revelar qué cuentas existen.
 */
sealed class AppError(mensaje: String) : Exception(mensaje) {
    class SinConexion : AppError("No hay conexión con el servidor")
    class CredencialesInvalidas : AppError(MENSAJE_CREDENCIALES)
    class UsuarioInactivo : AppError(MENSAJE_CREDENCIALES)
    class RostroNoValidado(val score: Double?) : AppError("Verificación facial no coincidente.")
    class SesionExpirada : AppError("La sesión expiró. Inicie sesión nuevamente")
    class SinPermiso : AppError("Acceso Denegado para tu rol.")
    class NoEncontrado(detalle: String?) : AppError(detalle ?: "Registro no encontrado")
    class Conflicto(detalle: String?) : AppError(detalle ?: "La operación entra en conflicto con el estado actual")
    class Validacion(val errores: List<String>) : AppError(errores.joinToString("\n"))
    class Servidor(val codigo: Int, detalle: String?) : AppError(detalle ?: "Error del servidor ($codigo)")
}

private const val MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos, o usuario inactivo."
