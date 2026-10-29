package com.universidad.sigelab.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Muestra [contenido] solo con el permiso de cámara concedido: lo pide al aparecer,
 * ofrece abrir Ajustes si fue negado y lo vuelve a comprobar al regresar a la app.
 */
@Composable
fun ConPermisoCamara(
    textoPermiso: String,
    modifier: Modifier = Modifier,
    alNegarPermiso: () -> Unit = {},
    contenido: @Composable () -> Unit,
) {
    val contexto = LocalContext.current
    val alNegarActual by rememberUpdatedState(alNegarPermiso)
    var tienePermiso by remember { mutableStateOf(contexto.tienePermisoCamara()) }
    val solicitarPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        tienePermiso = concedido
        if (!concedido) alNegarActual()
    }

    LaunchedEffect(Unit) {
        if (!tienePermiso) solicitarPermiso.launch(Manifest.permission.CAMERA)
    }
    // Al volver de Ajustes el permiso pudo cambiar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        tienePermiso = contexto.tienePermisoCamara()
    }

    if (tienePermiso) {
        contenido()
    } else {
        PermisoCamara(
            texto = textoPermiso,
            onSolicitar = { solicitarPermiso.launch(Manifest.permission.CAMERA) },
            onAbrirAjustes = { contexto.abrirAjustesDeLaApp() },
            modifier = modifier,
        )
    }
}

/** [CamaraFrontal] precedida del permiso de cámara (login y Crear Usuario). */
@Composable
fun CamaraFrontalConPermiso(
    procesando: Boolean,
    textoBoton: String,
    alCapturar: (ByteArray) -> Unit,
    alFallar: (String) -> Unit,
    modifier: Modifier = Modifier,
    textoPermiso: String = "SIGELAB necesita la cámara frontal para el reconocimiento facial.",
    alNegarPermiso: () -> Unit = {},
) {
    ConPermisoCamara(textoPermiso = textoPermiso, modifier = modifier, alNegarPermiso = alNegarPermiso) {
        CamaraFrontal(
            procesando = procesando,
            textoBoton = textoBoton,
            alCapturar = alCapturar,
            alFallar = alFallar,
            modifier = modifier,
        )
    }
}

@Composable
private fun PermisoCamara(
    texto: String,
    onSolicitar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(texto, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSolicitar, modifier = Modifier.fillMaxWidth()) { Text("Permitir cámara") }
            OutlinedButton(onClick = onAbrirAjustes, modifier = Modifier.fillMaxWidth()) { Text("Abrir ajustes") }
        }
    }
}

private fun Context.tienePermisoCamara() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.abrirAjustesDeLaApp() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
