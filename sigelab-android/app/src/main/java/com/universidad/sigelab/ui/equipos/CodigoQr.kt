package com.universidad.sigelab.ui.equipos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.rules.QrEquipo
import kotlin.math.floor

/** Matriz mínima del QR (un punto por módulo); se escala al dibujar para que salga nítida. */
private fun matrizQr(contenido: String): BitMatrix =
    QRCodeWriter().encode(
        contenido,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )

/** QR del equipo en pantalla. Siempre negro sobre blanco, también en tema oscuro, para que se pueda leer. */
@Composable
fun CodigoQr(equipo: Equipo, modifier: Modifier = Modifier) {
    val contenido = QrEquipo.contenido(equipo)
    val matriz = remember(contenido) { matrizQr(contenido) }
    Canvas(
        modifier
            .aspectRatio(1f)
            .background(Color.White)
            .clearAndSetSemantics { contentDescription = "Código QR del equipo $contenido" },
    ) {
        val modulos = matriz.width
        val celda = floor(size.minDimension / modulos)
        val origen = Offset((size.width - celda * modulos) / 2f, (size.height - celda * modulos) / 2f)
        for (y in 0 until matriz.height) {
            for (x in 0 until modulos) {
                if (matriz[x, y]) {
                    drawRect(Color.Black, topLeft = origen + Offset(x * celda, y * celda), size = Size(celda, celda))
                }
            }
        }
    }
}

fun impresionDisponible() = PrintHelper.systemSupportsPrint()

/**
 * Abre el diálogo de impresión del sistema (también permite "Guardar como PDF"). El trabajo se
 * llama QR_<código>, como el PNG que guarda escritorio en ImagenesQR.
 */
fun imprimirEtiqueta(contexto: Context, equipo: Equipo) {
    PrintHelper(contexto).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
        .printBitmap(QrEquipo.nombreArchivo(equipo).removeSuffix(".png"), etiquetaImprimible(equipo))
}

/** QR con el código y el nombre debajo, para pegar en el equipo. */
private fun etiquetaImprimible(equipo: Equipo, pixelesPorModulo: Int = 24): Bitmap {
    val matriz = matrizQr(QrEquipo.contenido(equipo))
    val ladoQr = matriz.width * pixelesPorModulo
    val altoTexto = 140
    val bitmap = Bitmap.createBitmap(ladoQr, ladoQr + altoTexto, Bitmap.Config.ARGB_8888)
    val lienzo = android.graphics.Canvas(bitmap)
    lienzo.drawColor(android.graphics.Color.WHITE)

    val negro = Paint().apply { color = android.graphics.Color.BLACK }
    for (y in 0 until matriz.height) {
        for (x in 0 until matriz.width) {
            if (matriz[x, y]) {
                lienzo.drawRect(
                    (x * pixelesPorModulo).toFloat(),
                    (y * pixelesPorModulo).toFloat(),
                    ((x + 1) * pixelesPorModulo).toFloat(),
                    ((y + 1) * pixelesPorModulo).toFloat(),
                    negro,
                )
            }
        }
    }

    val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 52f
        typeface = Typeface.DEFAULT_BOLD
    }
    lienzo.drawText(equipo.codigoInventario, ladoQr / 2f, ladoQr + 56f, texto)
    texto.textSize = 34f
    texto.typeface = Typeface.DEFAULT
    lienzo.drawText(equipo.nombre.take(36), ladoQr / 2f, ladoQr + 110f, texto)
    return bitmap
}
