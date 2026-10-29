package com.universidad.sigelab.domain.usecase

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.NuevoUsuario
import com.universidad.sigelab.domain.model.ResultadoRegistroUsuario
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.UsuarioRegistrado
import com.universidad.sigelab.domain.repository.UsuarioRepository
import javax.inject.Inject

enum class CampoUsuario { NOMBRE, APELLIDO, NOMBRE_USUARIO, CORREO, CONTRASENA }

/** Error ligado a un campo, para mostrarlo debajo del campo correspondiente. */
data class ErrorUsuario(val campo: CampoUsuario, val mensaje: String)

/**
 * Módulo "Crear Usuario" (solo ADMIN). Usa los mensajes de UsuarioService de escritorio para los
 * campos obligatorios y el mismo procedimiento CrearUsuario; además registra el rostro inicial.
 * Formato de usuario y correo, y la contraseña de 8 caracteres con letras y números, son
 * comprobaciones adicionales de la app.
 */
class RegistrarUsuarioUseCase @Inject constructor(
    private val repo: UsuarioRepository,
) {
    fun validar(nuevo: NuevoUsuario): List<ErrorUsuario> = buildList {
        when {
            nuevo.nombre.isBlank() -> add(ErrorUsuario(CampoUsuario.NOMBRE, "El nombre es obligatorio."))
            nuevo.nombre.trim().length > LARGO_NOMBRE -> add(ErrorUsuario(CampoUsuario.NOMBRE, "Máximo $LARGO_NOMBRE caracteres"))
        }
        when {
            nuevo.apellido.isBlank() -> add(ErrorUsuario(CampoUsuario.APELLIDO, "El apellido es obligatorio."))
            nuevo.apellido.trim().length > LARGO_NOMBRE -> add(ErrorUsuario(CampoUsuario.APELLIDO, "Máximo $LARGO_NOMBRE caracteres"))
        }
        when {
            nuevo.nombreUsuario.isBlank() -> add(ErrorUsuario(CampoUsuario.NOMBRE_USUARIO, "El nombre de usuario es obligatorio."))
            !REGEX_USUARIO.matches(nuevo.nombreUsuario.trim()) ->
                add(ErrorUsuario(CampoUsuario.NOMBRE_USUARIO, "Use 3 a 50 caracteres: letras, números, punto o guion bajo"))
        }
        when {
            nuevo.correo.isBlank() -> add(ErrorUsuario(CampoUsuario.CORREO, "El correo electrónico es obligatorio."))
            nuevo.correo.trim().length > LARGO_CORREO || !REGEX_CORREO.matches(nuevo.correo.trim()) ->
                add(ErrorUsuario(CampoUsuario.CORREO, "Correo inválido"))
        }
        when {
            nuevo.contrasena.isBlank() -> add(ErrorUsuario(CampoUsuario.CONTRASENA, "La contraseña es obligatoria."))
            nuevo.contrasena.length < 8 || nuevo.contrasena.none(Char::isDigit) || nuevo.contrasena.none(Char::isLetter) ->
                add(ErrorUsuario(CampoUsuario.CONTRASENA, "Mínimo 8 caracteres, con letras y números"))
        }
    }

    suspend fun listar(rolActual: Rol): Result<List<UsuarioRegistrado>> {
        if (!rolActual.puedeEditar(Modulo.USUARIOS)) return Result.failure(AppError.SinPermiso())
        return repo.listar()
    }

    suspend operator fun invoke(
        rolActual: Rol,
        nuevo: NuevoUsuario,
        fotoJpeg: ByteArray,
    ): Result<ResultadoRegistroUsuario> {
        if (!rolActual.puedeEditar(Modulo.USUARIOS)) return Result.failure(AppError.SinPermiso())
        val errores = validar(nuevo)
        if (errores.isNotEmpty()) return Result.failure(AppError.Validacion(errores.map { it.mensaje }))

        val limpio = nuevo.copy(
            nombre = nuevo.nombre.trim(),
            apellido = nuevo.apellido.trim(),
            nombreUsuario = nuevo.nombreUsuario.trim(),
            correo = nuevo.correo.trim(),
        )
        val idUsuario = repo.crear(limpio).getOrElse { return Result.failure(it) }
        val rostro = repo.registrarRostro(idUsuario, fotoJpeg)
        return Result.success(
            ResultadoRegistroUsuario(idUsuario, rostro.isSuccess, rostro.exceptionOrNull()?.message),
        )
    }

    /** El usuario ya existe pero su rostro falló: se reintenta solo /face/register. */
    suspend fun reintentarRostro(rolActual: Rol, idUsuario: Int, fotoJpeg: ByteArray): Result<Unit> {
        if (!rolActual.puedeEditar(Modulo.USUARIOS)) return Result.failure(AppError.SinPermiso())
        return repo.registrarRostro(idUsuario, fotoJpeg)
    }

    private companion object {
        // Largos de Usuario en SQL Server: Nombre/Apellido 100, NombreUsuario 50, Correo 150.
        const val LARGO_NOMBRE = 100
        const val LARGO_CORREO = 150
        val REGEX_USUARIO = Regex("^[A-Za-z0-9._]{3,50}$")
        val REGEX_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
