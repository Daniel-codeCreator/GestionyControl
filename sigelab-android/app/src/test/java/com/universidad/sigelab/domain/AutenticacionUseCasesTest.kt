package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.NuevoUsuario
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.usecase.CampoUsuario
import com.universidad.sigelab.domain.usecase.IniciarSesionUseCase
import com.universidad.sigelab.domain.usecase.RegistrarUsuarioUseCase
import com.universidad.sigelab.domain.usecase.VerificarRostroUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutenticacionUseCasesTest {

    private val foto = byteArrayOf(1, 2, 3)

    @Test
    fun `campos vacios no llegan al servidor`() = runBlocking {
        val auth = FakeAuthRepository()
        val resultado = IniciarSesionUseCase(auth, FakeAuditoriaRepository())("  ", "")
        assertTrue(resultado.exceptionOrNull() is AppError.Validacion)
        assertEquals(0, auth.llamadasLogin)
    }

    @Test
    fun `credenciales invalidas las audita el servidor, no la app`() = runBlocking {
        val auditoria = FakeAuditoriaRepository()
        val auth = FakeAuthRepository(credenciales = Result.failure(AppError.CredencialesInvalidas()))
        IniciarSesionUseCase(auth, auditoria)("jlopez", "mala")
        assertTrue(auditoria.eventos.isEmpty())
    }

    @Test
    fun `sin conexion en el paso 1 se audita LOGIN_FALLIDO localmente con el motivo de escritorio`() = runBlocking {
        val auditoria = FakeAuditoriaRepository()
        val auth = FakeAuthRepository(credenciales = Result.failure(AppError.SinConexion()))
        IniciarSesionUseCase(auth, auditoria)(" jlopez ", "clave")
        assertEquals(listOf("jlopez|null|Excepción de login: No hay conexión con el servidor"), auditoria.eventos)
    }

    @Test
    fun `mensajes de login iguales a escritorio`() = runBlocking {
        val caso = IniciarSesionUseCase(FakeAuthRepository(), FakeAuditoriaRepository())
        assertEquals("Ingrese el usuario.", caso(" ", "x").exceptionOrNull()?.message)
        assertEquals("Ingrese la contraseña.", caso("ana", "").exceptionOrNull()?.message)
        // No se revela si la cuenta existe o está inactiva.
        assertEquals(AppError.CredencialesInvalidas().message, AppError.UsuarioInactivo().message)
    }

    @Test
    fun `rostro que no coincide no duplica la auditoria del servidor`() = runBlocking {
        val auditoria = FakeAuditoriaRepository()
        val auth = FakeAuthRepository(rostro = Result.failure(AppError.RostroNoValidado(0.41)))
        val resultado = VerificarRostroUseCase(auth, auditoria)(preAuth, foto)
        assertTrue(resultado.exceptionOrNull() is AppError.RostroNoValidado)
        assertTrue(auditoria.eventos.isEmpty())
    }

    @Test
    fun `facial-service caido en el paso 2 se audita con el IdUsuario`() = runBlocking {
        val auditoria = FakeAuditoriaRepository()
        val auth = FakeAuthRepository(rostro = Result.failure(AppError.Servidor(503, null)))
        VerificarRostroUseCase(auth, auditoria)(preAuth, foto)
        assertTrue(auditoria.eventos.single().startsWith("jlopez|3|"))
    }

    private val nuevo = NuevoUsuario("Ana", "Pérez", "aperez", "ana@uni.edu", "clave1234", Rol.DOCENTE)

    @Test
    fun `solo ADMIN puede registrar usuarios`() = runBlocking {
        val repo = FakeUsuarioRepository()
        val resultado = RegistrarUsuarioUseCase(repo)(Rol.ENCARGADO, nuevo, foto)
        assertTrue(resultado.exceptionOrNull() is AppError.SinPermiso)
        assertTrue(repo.creados.isEmpty())
        assertTrue(RegistrarUsuarioUseCase(repo).listar(Rol.ENCARGADO).exceptionOrNull() is AppError.SinPermiso)
    }

    @Test
    fun `campos obligatorios con los mensajes de UsuarioService y datos sin espacios`() = runBlocking {
        val repo = FakeUsuarioRepository()
        val vacio = NuevoUsuario(" ", "", "", "", "", Rol.DOCENTE)
        assertEquals(
            listOf(
                "El nombre es obligatorio.",
                "El apellido es obligatorio.",
                "El nombre de usuario es obligatorio.",
                "El correo electrónico es obligatorio.",
                "La contraseña es obligatoria.",
            ),
            RegistrarUsuarioUseCase(repo).validar(vacio).map { it.mensaje },
        )

        RegistrarUsuarioUseCase(repo)(Rol.ADMIN, nuevo.copy(nombre = " Ana ", correo = " ana@uni.edu "), foto)
        assertEquals("Ana", repo.creados.single().nombre)
        assertEquals("ana@uni.edu", repo.creados.single().correo)
    }

    @Test
    fun `usuario creado aunque falle el rostro permite reintentar solo el rostro`() = runBlocking {
        val repo = FakeUsuarioRepository(rostro = Result.failure(AppError.Validacion(listOf("No se detectó un rostro"))))
        val resultado = RegistrarUsuarioUseCase(repo)(Rol.ADMIN, nuevo, foto).getOrThrow()
        assertEquals(42, resultado.idUsuario)
        assertFalse(resultado.rostroRegistrado)
        assertEquals("No se detectó un rostro", resultado.errorRostro)
    }

    @Test
    fun `validacion senala el campo de cada error`() {
        val errores = RegistrarUsuarioUseCase(FakeUsuarioRepository())
            .validar(nuevo.copy(apellido = " ", nombreUsuario = "a b", correo = "sin-arroba", contrasena = "corta"))
        assertEquals(
            listOf(CampoUsuario.APELLIDO, CampoUsuario.NOMBRE_USUARIO, CampoUsuario.CORREO, CampoUsuario.CONTRASENA),
            errores.map { it.campo },
        )
        assertTrue(RegistrarUsuarioUseCase(FakeUsuarioRepository()).validar(nuevo).isEmpty())
    }

    @Test
    fun `reintentar el rostro no vuelve a crear el usuario y exige ADMIN`() = runBlocking {
        val repo = FakeUsuarioRepository()
        val caso = RegistrarUsuarioUseCase(repo)

        assertTrue(caso.reintentarRostro(Rol.ENCARGADO, 42, foto).exceptionOrNull() is AppError.SinPermiso)
        assertTrue(caso.reintentarRostro(Rol.ADMIN, 42, foto).isSuccess)
        assertTrue(repo.creados.isEmpty())
    }
}
