package com.universidad.sigelab.domain

import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.DetallePrestamo
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.EstadoPrestamo
import com.universidad.sigelab.domain.model.ItemDevolucion
import com.universidad.sigelab.domain.model.Laboratorio
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.NuevoMantenimiento
import com.universidad.sigelab.domain.model.NuevoPrestamo
import com.universidad.sigelab.domain.model.NuevoUsuario
import com.universidad.sigelab.domain.model.PreAutenticacion
import com.universidad.sigelab.domain.model.Prestamo
import com.universidad.sigelab.domain.model.ResultadoDevolucion
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.Sesion
import com.universidad.sigelab.domain.model.TipoEquipo
import com.universidad.sigelab.domain.model.TipoMantenimiento
import com.universidad.sigelab.domain.model.Usuario
import com.universidad.sigelab.domain.model.UsuarioRegistrado
import com.universidad.sigelab.domain.repository.AuditoriaRepository
import com.universidad.sigelab.domain.repository.AuthRepository
import com.universidad.sigelab.domain.repository.CatalogoRepository
import com.universidad.sigelab.domain.repository.EquipoRepository
import com.universidad.sigelab.domain.repository.MantenimientoRepository
import com.universidad.sigelab.domain.repository.PrestamoRepository
import com.universidad.sigelab.domain.repository.UsuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

/** Código con el formato que genera escritorio: EQP00001. */
fun codigo(id: Int) = "EQP%05d".format(id)

fun equipo(id: Int, estado: EstadoEquipo = EstadoEquipo.DISPONIBLE) = Equipo(
    id = id,
    codigoInventario = codigo(id),
    nombre = "Equipo $id",
    idTipoEquipo = 1,
    tipo = "Laptop",
    idLaboratorio = 1,
    laboratorio = "Electrónica",
    estado = estado,
)

fun prestamo(vararg detalles: Pair<Int, Boolean>, estado: EstadoPrestamo = EstadoPrestamo.ACTIVO) = Prestamo(
    id = 10,
    solicitante = "Ana Pérez",
    fechaPrestamo = LocalDateTime.of(2026, 10, 20, 8, 0),
    fechaDevolucionPrevista = LocalDateTime.of(2026, 10, 27, 8, 0),
    estado = estado,
    observaciones = "Práctica de redes",
    detalles = detalles.map { (id, devuelto) -> DetallePrestamo(id, codigo(id), "Equipo $id", devuelto) },
)

fun mantenimiento(id: Int, idEquipo: Int, tipo: TipoMantenimiento = TipoMantenimiento.CORRECTIVO) = Mantenimiento(
    id = id,
    idEquipo = idEquipo,
    codigoInventario = codigo(idEquipo),
    nombreEquipo = "Equipo $idEquipo",
    tipo = tipo,
    fechaIngreso = LocalDateTime.of(2026, 10, 20, 15, 0),
    descripcion = "No enciende",
    responsable = "Carlos Ruiz",
)

val preAuth = PreAutenticacion(Usuario(3, "jlopez", "Juan López", Rol.ENCARGADO), "pre-token")

class FakePrestamoRepository : PrestamoRepository {
    var creado: NuevoPrestamo? = null
    var devolucion: Pair<Int, List<ItemDevolucion>>? = null

    override suspend fun prestamosActivos() = Result.success(emptyList<Prestamo>())

    override suspend fun crear(nuevo: NuevoPrestamo): Result<Int> {
        creado = nuevo
        return Result.success(99)
    }

    override suspend fun registrarDevolucion(idPrestamo: Int, items: List<ItemDevolucion>): Result<ResultadoDevolucion> {
        devolucion = idPrestamo to items
        return Result.success(ResultadoDevolucion(prestamoFinalizado = false, equiposPendientes = 0))
    }
}

class FakeMantenimientoRepository : MantenimientoRepository {
    val ingresos = mutableListOf<NuevoMantenimiento>()
    val cierres = mutableListOf<Pair<ResultadoMantenimiento, String?>>()

    override suspend fun enProceso() = Result.success(emptyList<Mantenimiento>())

    override suspend fun ingresar(nuevo: NuevoMantenimiento): Result<Unit> {
        ingresos += nuevo
        return Result.success(Unit)
    }

    override suspend fun finalizar(idMantenimiento: Int, resultado: ResultadoMantenimiento, observaciones: String?): Result<Unit> {
        cierres += resultado to observaciones
        return Result.success(Unit)
    }
}

class FakeAuthRepository(
    var credenciales: Result<PreAutenticacion> = Result.success(preAuth),
    var rostro: Result<Sesion> = Result.success(Sesion(preAuth.usuario, "access")),
) : AuthRepository {
    var llamadasLogin = 0

    override suspend fun validarCredenciales(usuario: String, contrasena: String): Result<PreAutenticacion> {
        llamadasLogin++
        return credenciales
    }

    override suspend fun verificarRostro(pre: PreAutenticacion, fotoJpeg: ByteArray) = rostro
    override suspend fun cerrarSesion() = Unit
}

class FakeAuditoriaRepository : AuditoriaRepository {
    val eventos = mutableListOf<String>()

    override suspend fun registrarLoginFallido(nombreUsuario: String, idUsuario: Int?, motivo: String) {
        eventos += "$nombreUsuario|$idUsuario|$motivo"
    }

    override suspend fun enviarPendientes() = true
}

class FakeEquipoRepository(
    private val porCodigo: Map<String, Equipo> = emptyMap(),
) : EquipoRepository {
    val codigosBuscados = mutableListOf<String>()
    val guardados = mutableListOf<Equipo>()

    override fun observarEquipos(estado: EstadoEquipo?): Flow<List<Equipo>> = flowOf(emptyList())
    override fun observarEquipo(id: Int): Flow<Equipo?> = flowOf(null)
    override suspend fun sincronizar() = Result.success(Unit)

    override suspend fun buscarPorCodigo(codigoInventario: String): Result<Equipo> {
        codigosBuscados += codigoInventario
        return porCodigo[codigoInventario]?.let { Result.success(it) }
            ?: Result.failure(AppError.NoEncontrado("No existe $codigoInventario"))
    }

    override suspend fun guardar(equipo: Equipo): Result<Equipo> {
        guardados += equipo
        return Result.success(if (equipo.id == 0) equipo.copy(id = 500, codigoInventario = codigo(500)) else equipo)
    }
}

class FakeCatalogoRepository : CatalogoRepository {
    val laboratoriosGuardados = mutableListOf<Laboratorio>()
    val tiposCreados = mutableListOf<TipoEquipo>()

    override suspend fun laboratorios() = Result.success(emptyList<Laboratorio>())

    override suspend fun guardarLaboratorio(laboratorio: Laboratorio): Result<Laboratorio> {
        laboratoriosGuardados += laboratorio
        return Result.success(if (laboratorio.id == 0) laboratorio.copy(id = 77, codigo = "LAB-00077") else laboratorio)
    }

    override suspend fun tiposEquipo() = Result.success(emptyList<TipoEquipo>())

    override suspend fun crearTipoEquipo(tipo: TipoEquipo): Result<TipoEquipo> {
        tiposCreados += tipo
        return Result.success(tipo.copy(id = 9))
    }
}

class FakeUsuarioRepository(
    var rostro: Result<Unit> = Result.success(Unit),
) : UsuarioRepository {
    val creados = mutableListOf<NuevoUsuario>()

    override suspend fun listar() = Result.success(emptyList<UsuarioRegistrado>())

    override suspend fun crear(nuevo: NuevoUsuario): Result<Int> {
        creados += nuevo
        return Result.success(42)
    }

    override suspend fun registrarRostro(idUsuario: Int, fotoJpeg: ByteArray) = rostro
}
