package com.universidad.sigelab.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.PreAutenticacion
import com.universidad.sigelab.ui.camera.CamaraFrontalConPermiso
import com.universidad.sigelab.ui.components.IndicadorPasos
import com.universidad.sigelab.ui.components.MensajeError

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val demoActivo by viewModel.demoActivo.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Encabezado()
                Spacer(Modifier.height(28.dp))
                if (ui.paso is PasoLogin.Credenciales) {
                    IndicadorPasos(pasoActual = 1, total = 2, etiqueta = "Credenciales")
                } else {
                    IndicadorPasos(pasoActual = 2, total = 2, etiqueta = "Reconocimiento facial")
                }
                Spacer(Modifier.height(24.dp))

                when (val paso = ui.paso) {
                    PasoLogin.Credenciales -> {
                        FormularioCredenciales(
                            ui = ui,
                            onUsuario = viewModel::onUsuarioCambia,
                            onContrasena = viewModel::onContrasenaCambia,
                            onContinuar = viewModel::continuar,
                        )
                        if (viewModel.demoDisponible) {
                            Spacer(Modifier.height(24.dp))
                            TarjetaModoDemo(
                                activo = demoActivo,
                                usuarios = viewModel.usuariosDemo,
                                contrasena = viewModel.contrasenaDemo,
                                habilitado = !ui.cargando,
                                onCambiar = viewModel::onModoDemo,
                            )
                        }
                    }
                    is PasoLogin.Rostro -> VerificacionFacial(
                        pre = paso.pre,
                        ui = ui,
                        onFoto = viewModel::onFotoCapturada,
                        onErrorCamara = viewModel::onErrorCamara,
                        onPermisoDenegado = viewModel::onPermisoCamaraDenegado,
                        onCancelar = viewModel::cancelarVerificacion,
                    )
                }
            }
        }
    }
}

/** Interruptor del modo demo (solo debug) con los usuarios de ejemplo. */
@Composable
private fun TarjetaModoDemo(
    activo: Boolean,
    usuarios: List<String>,
    contrasena: String,
    habilitado: Boolean,
    onCambiar: (Boolean) -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Modo demo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Datos de ejemplo dentro de la app, sin servidor. Solo en versiones de prueba.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = activo, onCheckedChange = onCambiar, enabled = habilitado)
            }
            if (activo) {
                Spacer(Modifier.height(8.dp))
                usuarios.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    "Contraseña: $contrasena · cualquier rostro es aceptado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Encabezado() {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Science,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(34.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "SIGELAB",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        "Sistema de Gestión y Control",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FormularioCredenciales(
    ui: LoginUiState,
    onUsuario: (String) -> Unit,
    onContrasena: (String) -> Unit,
    onContinuar: () -> Unit,
) {
    var verContrasena by rememberSaveable { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    val enviar = {
        foco.clearFocus()
        onContinuar()
    }

    OutlinedTextField(
        value = ui.usuario,
        onValueChange = onUsuario,
        label = { Text("Usuario") },
        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
        singleLine = true,
        enabled = !ui.cargando,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = ui.contrasena,
        onValueChange = onContrasena,
        label = { Text("Contraseña") },
        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { verContrasena = !verContrasena }) {
                Icon(
                    if (verContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (verContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                )
            }
        },
        visualTransformation = if (verContrasena) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        enabled = !ui.cargando,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { enviar() }),
        modifier = Modifier.fillMaxWidth(),
    )

    MensajeError(ui.error)
    Spacer(Modifier.height(20.dp))

    Button(
        onClick = enviar,
        enabled = !ui.cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (ui.cargando) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Text("Continuar")
        }
    }
}

@Composable
private fun VerificacionFacial(
    pre: PreAutenticacion,
    ui: LoginUiState,
    onFoto: (ByteArray) -> Unit,
    onErrorCamara: (String) -> Unit,
    onPermisoDenegado: () -> Unit,
    onCancelar: () -> Unit,
) {
    BackHandler(enabled = !ui.cargando, onBack = onCancelar)

    Text(
        "Hola, ${pre.usuario.nombreCompleto}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Centre su rostro en el óvalo, con buena luz y sin lentes oscuros, y presione Verificar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))

    CamaraFrontalConPermiso(
        procesando = ui.cargando,
        textoBoton = if (ui.cargando) "Verificando…" else "Verificar rostro",
        alCapturar = onFoto,
        alFallar = onErrorCamara,
        textoPermiso = "SIGELAB necesita la cámara frontal para el segundo factor de autenticación.",
        alNegarPermiso = onPermisoDenegado,
        modifier = Modifier.fillMaxWidth(),
    )

    MensajeError(ui.error)
    Spacer(Modifier.height(8.dp))
    Text(
        "Intentos restantes: ${ui.intentosRestantes}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(onClick = onCancelar, enabled = !ui.cargando) {
        Text("Cancelar y volver")
    }
}
