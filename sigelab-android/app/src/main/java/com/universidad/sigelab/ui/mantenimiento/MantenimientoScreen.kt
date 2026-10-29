package com.universidad.sigelab.ui.mantenimiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.ResultadoMantenimiento
import com.universidad.sigelab.domain.model.TipoMantenimiento
import com.universidad.sigelab.domain.rules.ReglasEquipo
import com.universidad.sigelab.ui.camera.DialogoEscanerQr
import com.universidad.sigelab.ui.components.EtiquetaEstado
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val PESTANA_EN_PROCESO = 0
private const val PESTANA_INGRESO = 1
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "Gestión de Mantenimiento" de escritorio, en dos pestañas para pantalla de teléfono. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MantenimientoScreen(
    onVolver: () -> Unit,
    viewModel: MantenimientoViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(PESTANA_EN_PROCESO) }
    var escaneando by rememberSaveable { mutableStateOf(false) }
    var busqueda by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onBusqueda(busqueda) }
    LaunchedEffect(Unit) {
        viewModel.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Mantenimiento") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    IconButton(onClick = { escaneando = true }) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Escanear equipo")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = pestana) {
                Tab(
                    selected = pestana == PESTANA_EN_PROCESO,
                    onClick = { pestana = PESTANA_EN_PROCESO },
                    text = { Text("En proceso · ${ui.enProceso.size}") },
                )
                Tab(
                    selected = pestana == PESTANA_INGRESO,
                    onClick = { pestana = PESTANA_INGRESO },
                    text = { Text("Registrar ingreso") },
                )
            }
            PullToRefreshBox(
                isRefreshing = ui.cargando,
                onRefresh = viewModel::cargar,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ui.error?.let { error ->
                        item(key = "error") {
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (pestana == PESTANA_EN_PROCESO) {
                        enProceso(ui, onAbrir = viewModel::abrirCierre)
                    } else {
                        registrarIngreso(
                            ui = ui,
                            busqueda = busqueda,
                            onTipo = viewModel::onTipo,
                            onBusqueda = {
                                busqueda = it
                                viewModel.onBusqueda(it)
                            },
                            onAbrir = { viewModel.abrirIngreso(it) },
                        )
                    }
                }
            }
        }
    }

    if (escaneando) {
        DialogoEscanerQr(
            titulo = "Escanee el equipo",
            onLeido = { texto ->
                // La cámara entrega varias lecturas seguidas: solo se usa la primera.
                if (escaneando) {
                    escaneando = false
                    viewModel.onQrLeido(texto)
                }
            },
            onCerrar = { escaneando = false },
        )
    }

    ui.ingreso?.let { ingreso ->
        DialogoIngreso(
            ingreso = ingreso,
            enviando = ui.enviando,
            error = ui.errorDialogo,
            onConfirmar = viewModel::confirmarIngreso,
            onCancelar = viewModel::cerrarDialogo,
        )
    }
    ui.cierre?.let { mantenimiento ->
        DialogoCierre(
            mantenimiento = mantenimiento,
            enviando = ui.enviando,
            error = ui.errorDialogo,
            onConfirmar = viewModel::confirmarCierre,
            onCancelar = viewModel::cerrarDialogo,
        )
    }
}

// ------------------------------------------------------------------- pestañas

private fun LazyListScope.enProceso(ui: MantenimientoUiState, onAbrir: (Mantenimiento) -> Unit) {
    item(key = "titulo-proceso") { Encabezado("Mantenimientos en proceso") }
    if (ui.enProceso.isEmpty() && !ui.cargando) {
        item(key = "vacio-proceso") { TextoVacio("No hay mantenimientos en proceso.") }
    }
    items(ui.enProceso, key = { "m-${it.id}" }) { mantenimiento ->
        FilaEnProceso(mantenimiento, onClick = { onAbrir(mantenimiento) })
    }
}

private fun LazyListScope.registrarIngreso(
    ui: MantenimientoUiState,
    busqueda: String,
    onTipo: (TipoMantenimiento) -> Unit,
    onBusqueda: (String) -> Unit,
    onAbrir: (Equipo) -> Unit,
) {
    item(key = "tipo") {
        Column {
            Text("Tipo Mantenimiento", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TipoMantenimiento.entries.forEachIndexed { i, tipo ->
                    SegmentedButton(
                        selected = ui.tipo == tipo,
                        onClick = { onTipo(tipo) },
                        shape = SegmentedButtonDefaults.itemShape(i, TipoMantenimiento.entries.size),
                    ) { Text(tipo.name) }
                }
            }
            Text(
                if (ui.tipo == TipoMantenimiento.PREVENTIVO) "Se listan los equipos DISPONIBLES." else "Se listan los equipos DAÑADOS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    item(key = "busqueda") {
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusqueda,
            placeholder = { Text("Buscar por código, nombre o serie") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (busqueda.isNotEmpty()) {
                    IconButton(onClick = { onBusqueda("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item(key = "titulo-afectado") { Encabezado("Equipo afectado") }
    if (ui.candidatos.isEmpty()) {
        item(key = "vacio-candidatos") {
            TextoVacio(
                when {
                    busqueda.isNotBlank() -> "Ningún equipo coincide con la búsqueda."
                    ui.tipo == TipoMantenimiento.PREVENTIVO -> "No hay equipos disponibles."
                    else -> "No hay equipos dañados."
                },
            )
        }
    }
    items(ui.candidatos, key = { "c-${it.id}" }) { equipo ->
        FilaCandidato(equipo, onClick = { onAbrir(equipo) })
    }
}

/** Columnas de la tabla de escritorio: ID Mant., Código, Equipo, Tipo, Fecha Ingreso, Técnico. */
@Composable
private fun FilaEnProceso(mantenimiento: Mantenimiento, onClick: () -> Unit) {
    val dias = mantenimiento.diasEnTaller(LocalDate.now())
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${mantenimiento.id} · ${mantenimiento.codigoInventario}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    mantenimiento.tipo.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(mantenimiento.nombreEquipo, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                mantenimiento.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Técnico: ${mantenimiento.responsable} · Ingreso: ${FORMATO_FECHA.format(mantenimiento.fechaIngreso)} " +
                    "(${if (dias == 0L) "hoy" else if (dias == 1L) "1 día" else "$dias días"})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaCandidato(equipo: Equipo, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(equipo.codigoInventario, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(equipo.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    equipo.laboratorio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            EtiquetaEstado(equipo.estado)
        }
    }
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TextoVacio(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    )
}

// ------------------------------------------------------------------- diálogos

@Composable
private fun DialogoIngreso(
    ingreso: IngresoPendiente,
    enviando: Boolean,
    error: String?,
    onConfirmar: (descripcion: String, responsable: String) -> Unit,
    onCancelar: () -> Unit,
) {
    val equipo = ingreso.equipo
    var responsable by rememberSaveable(equipo.id) { mutableStateOf("") }
    var descripcion by rememberSaveable(equipo.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Registrar ingreso · ${ingreso.tipo.name}") },
        text = {
            Column {
                Text("${equipo.codigoInventario} · ${equipo.nombre}", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                CambioDeEstado(equipo.estado, EstadoEquipo.MANTENIMIENTO)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = responsable,
                    onValueChange = { responsable = it },
                    label = { Text("Responsable") },
                    supportingText = { Text("Técnico a cargo") },
                    singleLine = true,
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción problema") },
                    minLines = 3,
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(descripcion, responsable) }, enabled = !enviando) {
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Registrar ingreso")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !enviando) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DialogoCierre(
    mantenimiento: Mantenimiento,
    enviando: Boolean,
    error: String?,
    onConfirmar: (ResultadoMantenimiento, String) -> Unit,
    onCancelar: () -> Unit,
) {
    var resultado by rememberSaveable(mantenimiento.id) { mutableStateOf(ResultadoMantenimiento.REPARADO) }
    var observaciones by rememberSaveable(mantenimiento.id) { mutableStateOf("") }
    var confirmarBaja by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cierre de mantenimiento") },
        text = {
            Column {
                Text("${mantenimiento.codigoInventario} · ${mantenimiento.nombreEquipo}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${mantenimiento.tipo.name} · Técnico: ${mantenimiento.responsable}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    mantenimiento.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text("Resultado", style = MaterialTheme.typography.labelLarge)
                Column(Modifier.selectableGroup()) {
                    ResultadoMantenimiento.entries.forEach { opcion ->
                        OpcionResultado(
                            opcion = opcion,
                            seleccionada = resultado == opcion,
                            habilitada = !enviando,
                            onSeleccionar = { resultado = opcion },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones finales (opcional)") },
                    minLines = 3,
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // La baja es definitiva: se pide una segunda confirmación.
                    if (resultado == ResultadoMantenimiento.NO_REPARABLE) {
                        confirmarBaja = true
                    } else {
                        onConfirmar(resultado, observaciones)
                    }
                },
                enabled = !enviando,
            ) {
                if (enviando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Finalizar mantenimiento")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !enviando) { Text("Cancelar") }
        },
    )

    if (confirmarBaja) {
        AlertDialog(
            onDismissRequest = { confirmarBaja = false },
            title = { Text("¿Dar de baja ${mantenimiento.codigoInventario}?") },
            text = { Text("El equipo pasa al estado BAJA: ya no podrá prestarse ni volver a mantenimiento.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBaja = false
                    onConfirmar(ResultadoMantenimiento.NO_REPARABLE, observaciones)
                }) { Text("Dar de baja", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBaja = false }) { Text("Volver") }
            },
        )
    }
}

@Composable
private fun OpcionResultado(
    opcion: ResultadoMantenimiento,
    seleccionada: Boolean,
    habilitada: Boolean,
    onSeleccionar: () -> Unit,
) {
    val estadoFinal = ReglasEquipo.estadoTrasMantenimiento(opcion)
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, enabled = habilitada, role = Role.RadioButton, onClick = onSeleccionar)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = seleccionada, onClick = null, enabled = habilitada)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(opcion.name, style = MaterialTheme.typography.bodyLarge)
            CambioDeEstado(EstadoEquipo.MANTENIMIENTO, estadoFinal)
        }
    }
}

/** "Estado actual → estado final" con los colores del ciclo de vida. */
@Composable
private fun CambioDeEstado(desde: EstadoEquipo, hacia: EstadoEquipo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EtiquetaEstado(desde)
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "pasa a",
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(16.dp),
        )
        EtiquetaEstado(hacia)
    }
}
