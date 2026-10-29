package com.universidad.sigelab.ui.catalogos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow

/**
 * Estructura común de Laboratorios y Tipos de Equipo: la tabla de escritorio se muestra como
 * tarjetas con el conteo de equipos, y el botón Guardar abre el formulario.
 * [onEditar] null = el catálogo solo admite altas (Tipos de Equipo en escritorio).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> CatalogoScaffold(
    titulo: String,
    textoNuevo: String,
    textoVacio: String,
    ui: CatalogoUiState<T>,
    mensajes: Flow<String>,
    clave: (T) -> Int,
    tituloFila: (T) -> String,
    detalleFila: (T) -> String?,
    onVolver: () -> Unit,
    onRefrescar: () -> Unit,
    onNuevo: () -> Unit,
    onEditar: ((T) -> Unit)?,
    etiquetaFila: @Composable (T) -> Unit = {},
    formulario: @Composable (T) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(mensajes) {
        mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevo,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(textoNuevo) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.cargando,
            onRefresh = onRefrescar,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ui.error?.let { error ->
                    item(key = "error") {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (ui.items.isEmpty() && !ui.cargando) {
                    item(key = "vacio") {
                        Text(
                            textoVacio,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                }
                items(ui.items, key = clave) { item ->
                    FilaCatalogo(
                        titulo = tituloFila(item),
                        detalle = detalleFila(item),
                        equipos = ui.equiposPorItem[clave(item)] ?: 0,
                        onClick = onEditar?.let { editar -> { editar(item) } },
                        etiqueta = { etiquetaFila(item) },
                    )
                }
            }
        }
    }

    ui.editando?.let { formulario(it) }
}

@Composable
private fun FilaCatalogo(
    titulo: String,
    detalle: String?,
    equipos: Int,
    onClick: (() -> Unit)?,
    etiqueta: @Composable () -> Unit,
) {
    val contenido: @Composable () -> Unit = {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (!detalle.isNullOrBlank()) {
                    Text(
                        detalle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    when (equipos) {
                        0 -> "Sin equipos"
                        1 -> "1 equipo"
                        else -> "$equipos equipos"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            etiqueta()
        }
    }
    if (onClick != null) {
        OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) { contenido() }
    } else {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) { contenido() }
    }
}

/** Diálogo de alta/actualización compartido: título, campos y botones Guardar o Actualizar / Cancelar. */
@Composable
internal fun DialogoFormulario(
    titulo: String,
    textoConfirmar: String,
    enviando: Boolean,
    error: String?,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    campos: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                campos()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !enviando) {
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !enviando) { Text("Cancelar") }
        },
    )
}

@Composable
internal fun CampoCatalogoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    ayuda: String? = null,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1,
) {
    val apoyo = error ?: ayuda
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = if (apoyo != null) { { Text(apoyo) } } else null,
        singleLine = lineas == 1,
        minLines = lineas,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalizacion,
            keyboardType = tipoTeclado,
            imeAction = if (lineas == 1) ImeAction.Next else ImeAction.Default,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
