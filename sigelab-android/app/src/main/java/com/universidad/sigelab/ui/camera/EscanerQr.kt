package com.universidad.sigelab.ui.camera

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Cámara trasera que lee códigos QR con ML Kit (modelo incluido en el APK, funciona sin red).
 * Llama a [alLeer] en el hilo principal con el texto del QR mientras [activo] sea true.
 */
@Composable
fun EscanerQr(
    activo: Boolean,
    alLeer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val alLeerActual by rememberUpdatedState(alLeer)
    val activoActual by rememberUpdatedState(activo)
    var linterna by remember { mutableStateOf(false) }

    // Declarados primero para liberarse al final: Compose desecha los efectos en orden inverso.
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val escaner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            escaner.close()
            ejecutor.shutdown()
        }
    }

    val controlador = remember {
        LifecycleCameraController(contexto).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }
    DisposableEffect(lifecycleOwner) {
        controlador.setImageAnalysisAnalyzer(ejecutor, AnalizadorQr(escaner) { texto -> if (activoActual) alLeerActual(texto) })
        controlador.bindToLifecycle(lifecycleOwner)
        onDispose {
            controlador.clearImageAnalysisAnalyzer()
            controlador.unbind()
        }
    }
    LaunchedEffect(linterna) {
        controlador.enableTorch(linterna)
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = controlador
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        MarcoVisor(Modifier.fillMaxSize())
        FilledTonalIconButton(
            onClick = { linterna = !linterna },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            Icon(
                if (linterna) Icons.Outlined.FlashOff else Icons.Outlined.FlashOn,
                contentDescription = if (linterna) "Apagar linterna" else "Encender linterna",
            )
        }
    }
}

private class AnalizadorQr(
    private val escaner: BarcodeScanner,
    private val alDetectar: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imagen: ImageProxy) {
        val media = imagen.image
        if (media == null) {
            imagen.close()
            return
        }
        // Los listeners de Task corren en el hilo principal.
        escaner.process(InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees))
            .addOnSuccessListener { codigos -> codigos.firstNotNullOfOrNull { it.rawValue }?.let(alDetectar) }
            .addOnCompleteListener { imagen.close() }
    }
}

/** Oscurece todo menos un recuadro central con esquinas blancas. */
@Composable
private fun MarcoVisor(modifier: Modifier) {
    Canvas(modifier) {
        val lado = size.minDimension * 0.65f
        val izquierda = (size.width - lado) / 2f
        val arriba = (size.height - lado) / 2f
        val sombra = Color.Black.copy(alpha = 0.45f)
        drawRect(sombra, size = Size(size.width, arriba))
        drawRect(sombra, topLeft = Offset(0f, arriba + lado), size = Size(size.width, size.height - arriba - lado))
        drawRect(sombra, topLeft = Offset(0f, arriba), size = Size(izquierda, lado))
        drawRect(sombra, topLeft = Offset(izquierda + lado, arriba), size = Size(size.width - izquierda - lado, lado))

        val largo = lado * 0.14f
        val grosor = 4.dp.toPx()
        listOf(
            Offset(izquierda, arriba) to Offset(1f, 1f),
            Offset(izquierda + lado, arriba) to Offset(-1f, 1f),
            Offset(izquierda, arriba + lado) to Offset(1f, -1f),
            Offset(izquierda + lado, arriba + lado) to Offset(-1f, -1f),
        ).forEach { (esquina, sentido) ->
            drawLine(Color.White, esquina, esquina + Offset(sentido.x * largo, 0f), grosor, StrokeCap.Round)
            drawLine(Color.White, esquina, esquina + Offset(0f, sentido.y * largo), grosor, StrokeCap.Round)
        }
    }
}
