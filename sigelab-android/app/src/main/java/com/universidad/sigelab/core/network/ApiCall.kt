package com.universidad.sigelab.core.network

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.universidad.sigelab.domain.model.AppError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.DateTimeException

/**
 * Ejecuta una llamada a la API y traduce cualquier fallo a [AppError].
 * [alErrorHttp] permite a un repositorio dar otro significado a un código
 * (en el login, 401 = credenciales inválidas y no "sesión expirada").
 */
suspend fun <T> apiCall(
    alErrorHttp: (codigo: Int, detalle: String?) -> AppError? = { _, _ -> null },
    bloque: suspend () -> T,
): Result<T> = try {
    Result.success(bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    // Retrofit ya dejó el cuerpo de error en memoria, leerlo no bloquea la red.
    val detalle = e.response()?.errorBody()?.string()?.let(::extraerDetalle)
    Result.failure(alErrorHttp(e.code(), detalle) ?: errorPorCodigo(e.code(), detalle))
} catch (e: IOException) {
    Result.failure(AppError.SinConexion())
} catch (e: AppError) {
    Result.failure(e)
} catch (e: JsonParseException) {
    Result.failure(AppError.Servidor(-1, "Respuesta inesperada del servidor"))
} catch (e: IllegalArgumentException) {
    // Valores que no encajan en el dominio (estado o rol desconocido).
    Result.failure(AppError.Servidor(-1, e.message))
} catch (e: DateTimeException) {
    Result.failure(AppError.Servidor(-1, "Fecha con formato inesperado: ${e.message}"))
}

/** Para endpoints sin cuerpo de respuesta (Response<Unit>). */
fun Response<*>.exigirExito() {
    if (!isSuccessful) throw HttpException(this)
}

/** FastAPI responde {"detail": "texto"} o, en errores 422, {"detail": [{"msg": "..."}]}. */
private fun extraerDetalle(cuerpo: String): String? = runCatching {
    val detail = JsonParser.parseString(cuerpo).asJsonObject.get("detail") ?: return@runCatching null
    when {
        detail.isJsonPrimitive -> detail.asString
        detail.isJsonArray -> detail.asJsonArray.mapNotNull { it.asJsonObject.get("msg")?.asString }.joinToString("\n")
        else -> null
    }
}.getOrNull()

private fun errorPorCodigo(codigo: Int, detalle: String?): AppError = when (codigo) {
    400, 422 -> AppError.Validacion(listOf(detalle ?: "Datos inválidos"))
    401 -> AppError.SesionExpirada()
    403 -> AppError.SinPermiso()
    404 -> AppError.NoEncontrado(detalle)
    409 -> AppError.Conflicto(detalle)
    else -> AppError.Servidor(codigo, detalle)
}
