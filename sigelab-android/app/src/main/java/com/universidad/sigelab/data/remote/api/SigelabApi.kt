package com.universidad.sigelab.data.remote.api

import com.universidad.sigelab.data.remote.dto.CrearPrestamoRequest
import com.universidad.sigelab.data.remote.dto.CrearPrestamoResponse
import com.universidad.sigelab.data.remote.dto.CrearUsuarioRequest
import com.universidad.sigelab.data.remote.dto.CrearUsuarioResponse
import com.universidad.sigelab.data.remote.dto.DashboardMetricasDto
import com.universidad.sigelab.data.remote.dto.DevolucionRequest
import com.universidad.sigelab.data.remote.dto.DevolucionResponse
import com.universidad.sigelab.data.remote.dto.EquipoDto
import com.universidad.sigelab.data.remote.dto.EquipoRequest
import com.universidad.sigelab.data.remote.dto.EventoAuditoriaRequest
import com.universidad.sigelab.data.remote.dto.FinalizarMantenimientoRequest
import com.universidad.sigelab.data.remote.dto.IngresoMantenimientoRequest
import com.universidad.sigelab.data.remote.dto.LaboratorioDto
import com.universidad.sigelab.data.remote.dto.LoginRequest
import com.universidad.sigelab.data.remote.dto.LoginResponse
import com.universidad.sigelab.data.remote.dto.MantenimientoDto
import com.universidad.sigelab.data.remote.dto.PrestamoDto
import com.universidad.sigelab.data.remote.dto.RegistroAuditoriaDto
import com.universidad.sigelab.data.remote.dto.TipoEquipoDto
import com.universidad.sigelab.data.remote.dto.UsuarioDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API de negocio (gateway REST delante de SQL Server). Android nunca abre una conexión
 * JDBC: las credenciales de la BD quedarían dentro del APK. Cada endpoint ejecuta las mismas
 * consultas que el repositorio equivalente de escritorio (ver backend-referencia).
 */
interface SigelabApi {

    // --- Autenticación y auditoría ---
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    /** Sin token: solo acepta LOGIN_FALLIDO ocurridos en el dispositivo. */
    @POST("auditoria/eventos")
    suspend fun registrarEvento(@Body body: EventoAuditoriaRequest): Response<Unit>

    // --- Dashboard ---
    @GET("dashboard/metricas")
    suspend fun metricas(): DashboardMetricasDto

    // --- Laboratorios (crear y actualizar, como LaboratorioView) ---
    @GET("laboratorios")
    suspend fun laboratorios(): List<LaboratorioDto>

    @POST("laboratorios")
    suspend fun crearLaboratorio(@Body body: LaboratorioDto): LaboratorioDto

    @PUT("laboratorios/{id}")
    suspend fun actualizarLaboratorio(@Path("id") id: Int, @Body body: LaboratorioDto): LaboratorioDto

    // --- Tipos de equipo (solo crear, como TipoEquipoView) ---
    @GET("tipos-equipo")
    suspend fun tiposEquipo(): List<TipoEquipoDto>

    @POST("tipos-equipo")
    suspend fun crearTipoEquipo(@Body body: TipoEquipoDto): TipoEquipoDto

    // --- Equipos ---
    @GET("equipos")
    suspend fun equipos(): List<EquipoDto>

    /** Lectura del QR (contiene el CodigoInventario). GET: no cambia el estado. */
    @GET("equipos/codigo/{codigo}")
    suspend fun equipoPorCodigo(@Path("codigo") codigo: String): EquipoDto

    @POST("equipos")
    suspend fun crearEquipo(@Body body: EquipoRequest): EquipoDto

    @PUT("equipos/{id}")
    suspend fun actualizarEquipo(@Path("id") id: Int, @Body body: EquipoRequest): EquipoDto

    // --- Préstamos y devoluciones ---
    @GET("prestamos")
    suspend fun prestamosActivos(): List<PrestamoDto>

    @POST("prestamos")
    suspend fun crearPrestamo(@Body body: CrearPrestamoRequest): CrearPrestamoResponse

    @POST("devoluciones")
    suspend fun registrarDevolucion(@Body body: DevolucionRequest): DevolucionResponse

    // --- Mantenimiento ---
    @GET("mantenimientos")
    suspend fun mantenimientosEnProceso(): List<MantenimientoDto>

    @POST("mantenimientos")
    suspend fun ingresarMantenimiento(@Body body: IngresoMantenimientoRequest): Response<Unit>

    @PUT("mantenimientos/{id}/finalizar")
    suspend fun finalizarMantenimiento(
        @Path("id") id: Int,
        @Body body: FinalizarMantenimientoRequest,
    ): Response<Unit>

    // --- Reportes ---
    /** "Consultar Historial": sin código devuelve todos los mantenimientos. */
    @GET("reportes/mantenimientos")
    suspend fun historialMantenimiento(@Query("codigo") codigo: String?): List<MantenimientoDto>

    /** Fechas en ISO (yyyy-MM-dd), rango inclusivo. Solo ADMIN. */
    @GET("auditoria")
    suspend fun auditoria(
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
        @Query("accion") accion: String?,
        @Query("limite") limite: Int = 200,
    ): List<RegistroAuditoriaDto>

    // --- Usuarios (solo ADMIN) ---
    @GET("usuarios")
    suspend fun usuarios(): List<UsuarioDto>

    @POST("usuarios")
    suspend fun crearUsuario(@Body body: CrearUsuarioRequest): CrearUsuarioResponse
}
