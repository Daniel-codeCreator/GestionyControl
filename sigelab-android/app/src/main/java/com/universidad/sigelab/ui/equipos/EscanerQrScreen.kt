package com.universidad.sigelab.ui.equipos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.ui.camera.ConPermisoCamara
import com.universidad.sigelab.ui.camera.EscanerQr

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EscanerQrScreen(
    onEquipoEncontrado: (idEquipo: Int) -> Unit,
    onVolver: () -> Unit,
    viewModel: EscanerQrViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptica = LocalHapticFeedback.current

    LaunchedEffect(ui.encontrado) {
        val equipo = ui.encontrado ?: return@LaunchedEffect
        haptica.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.navegacionHecha()
        onEquipoEncontrado(equipo.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escanear QR") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize(),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ConPermisoCamara(
                    textoPermiso = "SIGELAB necesita la cámara para leer las etiquetas QR de los equipos.",
                    modifier = Modifier.padding(16.dp),
                ) {
                    EscanerQr(
                        activo = ui.escaneando,
                        alLeer = viewModel::onCodigoLeido,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            PanelInferior(
                ui = ui,
                onEscanearOtraVez = viewModel::escanearOtraVez,
                onBuscarManual = viewModel::buscarManual,
            )
        }
    }
}

@Composable
private fun PanelInferior(
    ui: EscanerUiState,
    onEscanearOtraVez: () -> Unit,
    onBuscarManual: (String) -> Unit,
) {
    var manual by rememberSaveable { mutableStateOf("") }
    val foco = LocalFocusManager.current
    val buscar = {
        foco.clearFocus()
        onBuscarManual(manual)
    }

    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                when {
                    ui.consultando -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Buscando ${ui.codigo}…", style = MaterialTheme.typography.bodyMedium)
                    }
                    ui.error != null -> Column {
                        Text(ui.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onEscanearOtraVez) { Text("Escanear otra vez") }
                    }
                    else -> Text(
                        "Apunte a la etiqueta QR del equipo. Leerla no cambia su estado.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Para etiquetas dañadas o sin luz suficiente.
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it },
                label = { Text("O escriba el código de inventario") },
                singleLine = true,
                enabled = !ui.consultando,
                trailingIcon = {
                    IconButton(onClick = buscar, enabled = manual.isNotBlank() && !ui.consultando) {
                        Icon(Icons.Outlined.Search, contentDescription = "Buscar código")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { buscar() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
