package com.universidad.sigelab.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Escáner QR a pantalla completa sobre la pantalla actual. Para flujos que leen un
 * equipo y siguen en la misma pantalla (mantenimiento; luego préstamos).
 */
@Composable
fun DialogoEscanerQr(
    titulo: String,
    onLeido: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box {
                ConPermisoCamara(
                    textoPermiso = "SIGELAB necesita la cámara para leer las etiquetas QR de los equipos.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                ) {
                    EscanerQr(activo = true, alLeer = onLeido, modifier = Modifier.fillMaxSize())
                }
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(onClick = onCerrar) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cerrar escáner")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(titulo, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
