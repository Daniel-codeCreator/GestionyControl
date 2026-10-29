package com.universidad.sigelab.data.remote.api

import com.universidad.sigelab.data.remote.dto.FaceRegisterResponse
import com.universidad.sigelab.data.remote.dto.FaceVerifyResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * facial-service (Python / FastAPI). En escritorio el servicio usa la webcam del PC;
 * desde el teléfono la foto viaja en la petición (multipart), así que el servicio
 * debe aceptar el archivo [CAMPO_IMAGEN].
 */
interface FacialApi {

    /** Paso 2 del login. Autoriza con el token de pre-autenticación del paso 1. */
    @Multipart
    @POST("face/verify")
    suspend fun verificar(
        @Header("Authorization") autorizacion: String,
        @Part("id_usuario") idUsuario: RequestBody,
        @Part imagen: MultipartBody.Part,
    ): FaceVerifyResponse

    /** Rostro inicial de un usuario nuevo. Autoriza con el token de sesión del ADMIN. */
    @Multipart
    @POST("face/register")
    suspend fun registrar(
        @Part("id_usuario") idUsuario: RequestBody,
        @Part imagen: MultipartBody.Part,
    ): FaceRegisterResponse

    companion object {
        const val CAMPO_IMAGEN = "imagen"
    }
}

internal fun Int.comoParteTexto(): RequestBody = toString().toRequestBody("text/plain".toMediaType())

internal fun ByteArray.comoParteJpeg(): MultipartBody.Part =
    MultipartBody.Part.createFormData(
        FacialApi.CAMPO_IMAGEN,
        "rostro.jpg",
        toRequestBody("image/jpeg".toMediaType()),
    )
