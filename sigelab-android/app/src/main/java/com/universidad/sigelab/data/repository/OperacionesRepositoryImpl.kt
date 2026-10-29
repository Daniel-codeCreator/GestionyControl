package com.universidad.sigelab.data.repository

import com.universidad.sigelab.core.network.apiCall
import com.universidad.sigelab.core.network.exigirExito
import com.universidad.sigelab.data.aIso
import com.universidad.sigelab.data.remote.api.FacialApi
import com.universidad.sigelab.data.remote.api.SigelabApi
import com.universidad.sigelab.data.remote.api.comoParteJpeg
import com.universidad.sigelab.data.remote.api.comoParteTexto
import com.universidad.sigelab.data.remote.dto.CrearPrestamoRequest
import com.universidad.sigelab.data.remote.dto.CrearUsuarioRequest
import com.universidad.sigelab.data.remote.dto.DevolucionRequest
import com.universidad.sigelab.data.remote.dto.FinalizarMantenimientoRequest
import com.universidad.sigelab.data.remote.dto.IngresoMantenimientoRequest
import com.universidad.sigelab.data.remote.dto.ItemDevolucionDto
import com.universidad.sigelab.data.toDomain
import com.universidad.sigelab.domain.model.AppError
import com.universidad.sigelab.domain.model.ItemDevolucion
import com.universidad.sigelab.domain.model.NuevoMantenimiento
import com.universidad.sigelab.domain.model.NuevoPrestamo
import com.universidad.sigelab.domain.model.NuevoUsuario
import com.universidad.sigelab.domain.model.ResultadoDevolucion
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.repository.MantenimientoRepository
import com.universidad.sigelab.domain.repository.PrestamoRepository
import com.universidad.sigelab.domain.repository.UsuarioRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrestamoRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
) : PrestamoRepository {

    override suspend fun prestamosActivos() = apiCall { api.prestamosActivos().map { it.toDomain() } }

    override suspend fun crear(nuevo: NuevoPrestamo): Result<Int> = apiCall {
        api.crearPrestamo(
            CrearPrestamoRequest(
                solicitante = nuevo.solicitante,
                idsEquipos = nuevo.idsEquipos,
                fechaDevolucionPrevista = nuevo.fechaDevolucionPrevista.aIso(),
                observaciones = nuevo.observaciones,
            ),
        ).idPrestamo
    }

    override suspend fun registrarDevolucion(idPrestamo: Int, items: List<ItemDevolucion>): Result<ResultadoDevolucion> =
        apiCall(
            alErrorHttp = { http, detalle ->
                // Mismo texto que PrestamoService de escritorio.
                if (http == 409) {
                    AppError.Conflicto(detalle ?: "No se pudo procesar la devolución. Verifique si el equipo ya fue devuelto.")
                } else {
                    null
                }
            },
        ) {
            val body = DevolucionRequest(
                idPrestamo = idPrestamo,
                items = items.map { ItemDevolucionDto(it.idEquipo, it.condicion.name, it.observacion) },
            )
            api.registrarDevolucion(body).let { ResultadoDevolucion(it.prestamoFinalizado, it.equiposPendientes) }
        }
}

@Singleton
class MantenimientoRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
) : MantenimientoRepository {

    override suspend fun enProceso() = apiCall { api.mantenimientosEnProceso().map { it.toDomain() } }

    override suspend fun ingresar(nuevo: NuevoMantenimiento) = apiCall {
        api.ingresarMantenimiento(
            IngresoMantenimientoRequest(nuevo.idEquipo, nuevo.tipo.name, nuevo.descripcion, nuevo.responsable),
        ).exigirExito()
    }

    override suspend fun finalizar(idMantenimiento: Int, resultado: ResultadoMantenimiento, observaciones: String?) =
        apiCall {
            api.finalizarMantenimiento(idMantenimiento, FinalizarMantenimientoRequest(resultado.name, observaciones))
                .exigirExito()
        }
}

@Singleton
class UsuarioRepositoryImpl @Inject constructor(
    private val api: SigelabApi,
    private val facialApi: FacialApi,
) : UsuarioRepository {

    override suspend fun listar() = apiCall { api.usuarios().map { it.toDomain() } }

    /** El procedimiento CrearUsuario responde "el nombre de usuario ya existe" o "el correo ya tiene un usuario registrado". */
    override suspend fun crear(nuevo: NuevoUsuario): Result<Int> = apiCall(
        alErrorHttp = { codigo, detalle -> if (codigo == 409) AppError.Conflicto(detalle ?: "El nombre de usuario ya existe") else null },
    ) {
        api.crearUsuario(
            CrearUsuarioRequest(
                nombre = nuevo.nombre,
                apellido = nuevo.apellido,
                nombreUsuario = nuevo.nombreUsuario,
                correo = nuevo.correo,
                contrasena = nuevo.contrasena,
                rol = nuevo.rol.name,
            ),
        ).idUsuario
    }

    override suspend fun registrarRostro(idUsuario: Int, fotoJpeg: ByteArray): Result<Unit> = apiCall {
        val respuesta = facialApi.registrar(idUsuario.comoParteTexto(), fotoJpeg.comoParteJpeg())
        if (!respuesta.ok) throw AppError.Validacion(listOf(respuesta.detalle ?: "No se pudo registrar el rostro"))
    }
}
