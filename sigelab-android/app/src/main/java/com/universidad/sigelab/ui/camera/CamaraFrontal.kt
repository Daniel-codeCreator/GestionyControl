package com.universidad.sigelab.ui.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import android.util.Size as TamanoCamara

/**
 * Vista previa de la cámara frontal con guía ovalada y botón de captura.
 * Entrega un JPEG ya rotado y reducido (lado mayor ≤ 720 px), listo para multipart.
 * La usan el login (/face/verify) y "Crear Usuario" (/face/register).
 */
@Composable
fun CamaraFrontal(
    procesando: Boolean,
    textoBoton: String,
    alCapturar: (ByteArray) -> Unit,
    alFallar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alCapturarActual by rememberUpdatedState(alCapturar)
    val alFallarActual by rememberUpdatedState(alFallar)
    var tomandoFoto by remember { mutableStateOf(false) }

    val controlador = remember {
        LifecycleCameraController(contexto).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(TamanoCamara(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                )
                .build()
        }
    }
    // Decodificar y comprimir fuera del hilo principal.
    val ejecutor = remember { Executors.newSingleThreadExecutor() }

    // Compose desecha los efectos en orden inverso: el ejecutor se apaga después de soltar la cámara.
    DisposableEffect(Unit) {
        onDispose { ejecutor.shutdown() }
    }
    DisposableEffect(lifecycleOwner) {
        controlador.bindToLifecycle(lifecycleOwner)
        onDispose { controlador.unbind() }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        // TextureView: respeta el recorte redondeado de Compose.
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        controller = controlador
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            GuiaRostro(Modifier.fillMaxSize())
            if (procesando || tomandoFoto) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                tomandoFoto = true
                capturar(
                    controlador = controlador,
                    ejecutor = ejecutor,
                    principal = ContextCompat.getMainExecutor(contexto),
                    alCapturar = { jpeg ->
                        tomandoFoto = false
                        alCapturarActual(jpeg)
                    },
                    alFallar = { mensaje ->
                        tomandoFoto = false
                        alFallarActual(mensaje)
                    },
                )
            },
            enabled = !procesando && !tomandoFoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Outlined.Face, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(textoBoton)
        }
    }
}

@Composable
private fun GuiaRostro(modifier: Modifier) {
    Canvas(modifier) {
        val ancho = size.width * 0.64f
        val alto = size.height * 0.62f
        drawOval(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset((size.width - ancho) / 2f, (size.height - alto) / 2f - size.height * 0.03f),
            size = Size(ancho, alto),
            style = Stroke(
                width = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)),
            ),
        )
    }
}

private fun capturar(
    controlador: LifecycleCameraController,
    ejecutor: Executor,
    principal: Executor,
    alCapturar: (ByteArray) -> Unit,
    alFallar: (String) -> Unit,
) {
    controlador.takePicture(
        ejecutor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val resultado = runCatching { image.use { it.aJpegNormalizado() } }
                principal.execute {
                    resultado.fold(alCapturar) { alFallar(it.message ?: "No se pudo procesar la foto") }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                principal.execute { alFallar(exception.message ?: "No se pudo capturar la foto") }
            }
        },
    )
}

/** Corrige la rotación del sensor, reduce el tamaño y comprime a JPEG. */
internal fun ImageProxy.aJpegNormalizado(ladoMaximo: Int = 720, calidad: Int = 85): ByteArray {
    val original = toBitmap()
    val escala = minOf(1f, ladoMaximo.toFloat() / maxOf(original.width, original.height))
    val matriz = Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
        postScale(escala, escala)
    }
    val ajustada = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
    val jpeg = ByteArrayOutputStream().use { salida ->
        ajustada.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
        salida.toByteArray()
    }
    if (ajustada !== original) ajustada.recycle()
    original.recycle()
    return jpeg
}
