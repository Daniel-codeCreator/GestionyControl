package com.universidad.sigelab.ui.prestamos

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.CondicionDevolucion
import com.universidad.sigelab.domain.model.DetallePrestamo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.Prestamo
import com.universidad.sigelab.domain.rules.ReglasEquipo
import com.universidad.sigelab.ui.camera.DialogoEscanerQr
import com.universidad.sigelab.ui.components.EtiquetaEstado
import com.universidad.sigelab.ui.components.MensajeError
import com.universidad.sigelab.ui.theme.LocalColoresEstado
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val PESTANA_PRESTAMO = 0
private const val PESTANA_ACTIVOS = 1
private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "Gestión de Préstamos y Devoluciones de Equipos" de escritorio, en dos pestañas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrestamosScreen(
    onVolver: () -> Unit,
    viewModel: PrestamosViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableIntStateOf(PESTANA_PRESTAMO) }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var escaneando by rememberSaveable { mutableStateOf(false) }
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onBusqueda(busqueda) }
    LaunchedEffect(Unit) {
        viewModel.mensajes.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Préstamos y Devoluciones") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pestana = PESTANA_PRESTAMO
                        escaneando = true
                    }) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Escanear Código QR")
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
                    selected = pestana == PESTANA_PRESTAMO,
                    onClick = { pestana = PESTANA_PRESTAMO },
                    text = { Text("Registrar préstamo") },
                )
                Tab(
                    selected = pestana == PESTANA_ACTIVOS,
                    onClick = { pestana = PESTANA_ACTIVOS },
                    text = { Text("Activos · ${ui.activos.sumOf { it.pendientes.size }}") },
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
                    if (pestana == PESTANA_PRESTAMO) {
                        registrarPrestamo(
                            ui = ui,
                            busqueda = busqueda,
                            onBusqueda = {
                                busqueda = it
                                viewModel.onBusqueda(it)
                            },
                            onElegirFecha = { eligiendoFecha = true },
                            vm = viewModel,
                        )
                    } else {
                        prestamosActivos(ui.activos, cargando = ui.cargando, onRecibir = viewModel::abrirDevolucion)
                    }
                }
            }
        }
    }

    if (escaneando) {
        DialogoEscanerQr(
            titulo = "Escanee el equipo a prestar",
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

    if (eligiendoFecha) {
        DialogoFecha(
            inicial = ui.formulario.fechaFin,
            onElegir = {
                eligiendoFecha = false
                viewModel.onFechaFin(it)
            },
            onCerrar = { eligiendoFecha = false },
        )
    }

    ui.devolucion?.let { pendiente ->
        DialogoDevolucion(
            pendiente = pendiente,
            procesando = ui.procesando,
            error = ui.errorDevolucion,
            vistaPrevia = viewModel::vistaPrevia,
            onProcesar = viewModel::procesar,
            onCancelar = viewModel::cerrarDevolucion,
        )
    }
}

// ------------------------------------------------------- registrar préstamo

private fun LazyListScope.registrarPrestamo(
    ui: PrestamosUiState,
    busqueda: String,
    onBusqueda: (String) -> Unit,
    onElegirFecha: () -> Unit,
    vm: PrestamosViewModel,
) {
    val f = ui.formulario
    item(key = "solicitante") {
        OutlinedTextField(
            value = f.solicitante,
            onValueChange = vm::onSolicitante,
            label = { Text("Estudiante / Usuario") },
            singleLine = true,
            enabled = !ui.registrando,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item(key = "fecha") {
        OutlinedTextField(
            value = FECHA.format(f.fechaFin),
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha Fin Estimada") },
            supportingText = { Text("El préstamo vence al final de ese día.") },
            trailingIcon = {
                IconButton(onClick = onElegirFecha, enabled = !ui.registrando) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Elegir fecha")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item(key = "observaciones") {
        OutlinedTextField(
            value = f.observaciones,
            onValueChange = vm::onObservaciones,
            label = { Text("Observaciones") },
            supportingText = { Text("Obligatorias, como en escritorio.") },
            minLines = 2,
            enabled = !ui.registrando,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    item(key = "titulo-disponibles") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "EQUIPOS DISPONIBLES · ${ui.seleccionados.size} seleccionado(s)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
    item(key = "acciones-seleccion") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::seleccionarTodos, enabled = ui.disponibles.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text("Seleccionar todos")
            }
            OutlinedButton(onClick = vm::quitarSeleccion, enabled = ui.seleccionados.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text("Quitar selección")
            }
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
    if (ui.disponibles.isEmpty() && !ui.cargando) {
        item(key = "vacio-disponibles") {
            TextoVacio(if (busqueda.isBlank()) "No hay equipos disponibles." else "Ningún equipo disponible coincide con la búsqueda.")
        }
    }
    items(ui.disponibles, key = { "d-${it.id}" }) { equipo ->
        val marcado = equipo.id in f.seleccion
        OutlinedCard(onClick = { vm.alternar(equipo.id) }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = marcado, onCheckedChange = { vm.alternar(equipo.id) })
                Column(Modifier.weight(1f)) {
                    Text(equipo.codigoInventario, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(equipo.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    item(key = "registrar") {
        Column {
            MensajeError(ui.errorFormulario)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = vm::registrar,
                enabled = !ui.registrando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (ui.registrando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Text("Registrar Préstamo (${ui.seleccionados.size})")
                }
            }
            TextButton(
                onClick = vm::limpiar,
                enabled = !ui.registrando,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Limpiar Campos") }
        }
    }
}

// --------------------------------------------------------- préstamos activos

/** Tabla de escritorio: ID Préstamo, ID Equipo, Solicitante, Equipo, Código, Fecha Préstamo, Estado. */
private fun LazyListScope.prestamosActivos(
    activos: List<Prestamo>,
    cargando: Boolean,
    onRecibir: (Prestamo, DetallePrestamo) -> Unit,
) {
    if (activos.isEmpty() && !cargando) {
        item(key = "vacio-activos") { TextoVacio("No hay préstamos activos.") }
    }
    items(activos, key = { "p-${it.id}" }) { prestamo ->
        TarjetaPrestamo(prestamo, onRecibir = { detalle -> onRecibir(prestamo, detalle) })
    }
}

@Composable
private fun TarjetaPrestamo(prestamo: Prestamo, onRecibir: (DetallePrestamo) -> Unit) {
    val vencido = prestamo.estaVencido(LocalDateTime.now())
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        prestamo.solicitante,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (vencido) "VENCIDO" else prestamo.estado.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (vencido) LocalColoresEstado.current.vencido else MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    "Préstamo #${prestamo.id} · ${FECHA.format(prestamo.fechaPrestamo)} → ${FECHA.format(prestamo.fechaDevolucionPrevista)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                prestamo.observaciones?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            prestamo.pendientes.forEachIndexed { i, detalle ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRecibir(detalle) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(detalle.codigoInventario, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(detalle.nombreEquipo, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { onRecibir(detalle) }) { Text("Recibir") }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ diálogos

@Composable
private fun DialogoDevolucion(
    pendiente: DevolucionPendiente,
    procesando: Boolean,
    error: String?,
    vistaPrevia: (CondicionDevolucion) -> com.universidad.sigelab.domain.usecase.VistaPreviaDevolucion?,
    onProcesar: (CondicionDevolucion, String) -> Unit,
    onCancelar: () -> Unit,
) {
    var condicion by rememberSaveable(pendiente.detalle.idEquipo) { mutableStateOf(CondicionDevolucion.BUENO) }
    var observacion by rememberSaveable(pendiente.detalle.idEquipo) { mutableStateOf("") }
    val previa = vistaPrevia(condicion)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Procesar devolución") },
        text = {
            Column {
                Text(
                    "${pendiente.detalle.codigoInventario} · ${pendiente.detalle.nombreEquipo}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "Préstamo #${pendiente.prestamo.id} · ${pendiente.prestamo.solicitante}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text("Condición Entrega", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    CondicionDevolucion.entries.forEachIndexed { i, opcion ->
                        SegmentedButton(
                            selected = condicion == opcion,
                            onClick = { condicion = opcion },
                            enabled = !procesando,
                            shape = SegmentedButtonDefaults.itemShape(i, CondicionDevolucion.entries.size),
                        ) { Text(opcion.name) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EtiquetaEstado(EstadoEquipo.PRESTADO)
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "pasa a",
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(16.dp),
                    )
                    EtiquetaEstado(ReglasEquipo.estadoTrasDevolucion(condicion))
                }
                if (previa?.prestamoFinalizado == true) {
                    Text(
                        "Es el último equipo pendiente: el préstamo quedará FINALIZADO.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = observacion,
                    onValueChange = { observacion = it },
                    label = { Text("Observaciones Devolución") },
                    minLines = 2,
                    enabled = !procesando,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onProcesar(condicion, observacion) }, enabled = !procesando) {
                if (procesando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Procesar Devolución")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar, enabled = !procesando) { Text("Cancelar") }
        },
    )
}

/** Calendario que solo permite elegir desde hoy (PrestamoService rechaza fechas pasadas). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoFecha(inicial: LocalDate, onElegir: (LocalDate) -> Unit, onCerrar: () -> Unit) {
    val hoyUtc = remember { LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = inicial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= hoyUtc
        },
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    ?.let(onElegir) ?: onCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    ) {
        DatePicker(state = estado)
    }
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
