package com.universidad.sigelab.ui.reportes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.AccionAuditoria
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.domain.model.LIMITE_AUDITORIA
import com.universidad.sigelab.domain.model.Mantenimiento
import com.universidad.sigelab.domain.model.PeriodoAuditoria
import com.universidad.sigelab.domain.model.RegistroAuditoria
import com.universidad.sigelab.domain.usecase.PrestamoVencido
import com.universidad.sigelab.ui.camera.DialogoEscanerQr
import com.universidad.sigelab.ui.components.EtiquetaEstado
import com.universidad.sigelab.ui.theme.LocalColoresEstado
import java.time.format.DateTimeFormatter

private val FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

/** "Módulo de Reportes y Consultas" de escritorio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    onVolver: () -> Unit,
    viewModel: ReportesViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    var seleccion by rememberSaveable { mutableStateOf(PestanaReporte.POR_ESTADO) }
    var escaneando by rememberSaveable { mutableStateOf(false) }
    // Si la pestaña guardada ya no está disponible para el rol, se vuelve a la primera.
    val pestana = if (seleccion in ui.pestanas) seleccion else ui.pestanas.first()

    LaunchedEffect(pestana) { viewModel.alMostrar(pestana) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes y Consultas") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    // El CSV se arma solo al tocar; aquí basta saber si hay datos.
                    IconButton(
                        onClick = { viewModel.paraCompartir(pestana)?.let { contexto.compartirTexto(it.titulo, it.csv) } },
                        enabled = hayDatos(pestana, ui),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Compartir reporte como CSV")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = ui.pestanas.indexOf(pestana)) {
                ui.pestanas.forEach { p ->
                    Tab(
                        selected = p == pestana,
                        onClick = { seleccion = p },
                        text = { Text(p.titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
            when (pestana) {
                PestanaReporte.POR_ESTADO -> PestanaPorEstado(
                    estado = ui.porEstado,
                    onFiltro = viewModel::onFiltroEstado,
                    onRefrescar = viewModel::sincronizarEquipos,
                )
                PestanaReporte.VENCIDOS -> PestanaVencidos(ui.vencidos, onRefrescar = viewModel::cargarVencidos)
                PestanaReporte.MANTENIMIENTOS -> PestanaMantenimientos(
                    estado = ui.historial,
                    onCodigo = viewModel::onCodigoHistorial,
                    onConsultar = viewModel::consultarHistorial,
                    onEscanear = { escaneando = true },
                )
                PestanaReporte.AUDITORIA -> PestanaAuditoria(
                    estado = ui.auditoria,
                    onPeriodo = viewModel::onPeriodo,
                    onAccion = viewModel::onAccion,
                    onRefrescar = viewModel::cargarAuditoria,
                )
            }
        }
    }

    if (escaneando) {
        DialogoEscanerQr(
            titulo = "Escanee el equipo",
            onLeido = { texto ->
                if (escaneando) {
                    escaneando = false
                    viewModel.onQrHistorial(texto)
                }
            },
            onCerrar = { escaneando = false },
        )
    }
}

// ------------------------------------------------------- equipos por estado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestanaPorEstado(estado: PorEstadoUi, onFiltro: (EstadoEquipo?) -> Unit, onRefrescar: () -> Unit) {
    PullToRefreshBox(isRefreshing = estado.sincronizando, onRefresh = onRefrescar, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            encabezadoReporte(PestanaReporte.POR_ESTADO)
            item(key = "filtro") {
                Column {
                    Text("Estado:", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = estado.filtro == null,
                            onClick = { onFiltro(null) },
                            label = { Text("TODOS (${estado.resumen.total})") },
                        )
                        EstadoEquipo.entries.forEach { opcion ->
                            FilterChip(
                                selected = estado.filtro == opcion,
                                onClick = { onFiltro(opcion) },
                                label = { Text("${opcion.name} (${estado.resumen.conteos[opcion] ?: 0})") },
                            )
                        }
                    }
                }
            }
            estado.error?.let { item(key = "error") { TextoError(it) } }
            if (estado.equipos.isEmpty() && !estado.sincronizando) {
                item(key = "vacio") { TextoVacio("No hay equipos en este estado.") }
            }
            items(estado.equipos, key = { it.id }) { FilaEquipo(it) }
        }
    }
}

/** Columnas de escritorio: ID, Código, Nombre, Tipo, Laboratorio, Estado. */
@Composable
private fun FilaEquipo(equipo: Equipo) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(equipo.codigoInventario, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(equipo.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${equipo.tipo} · ${equipo.laboratorio}",
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

// --------------------------------------------------------- préstamos vencidos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestanaVencidos(estado: VencidosUi, onRefrescar: () -> Unit) {
    PullToRefreshBox(isRefreshing = estado.cargando, onRefresh = onRefrescar, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            encabezadoReporte(PestanaReporte.VENCIDOS)
            estado.error?.let { item(key = "error") { TextoError(it) } }
            if (estado.cargado && estado.lista.isEmpty() && estado.error == null) {
                item(key = "vacio") { TextoVacio("No hay préstamos vencidos.") }
            }
            if (estado.lista.isNotEmpty()) {
                item(key = "resumen") {
                    val equipos = estado.lista.sumOf { it.pendientes.size }
                    Text(
                        "${estado.lista.size} préstamo(s) vencido(s) · $equipos equipo(s) sin devolver",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(estado.lista, key = { it.prestamo.id }) { TarjetaVencido(it) }
        }
    }
}

/** Columnas de escritorio: ID Préstamo, Solicitante, Fecha Préstamo, Fecha Esperada, Estado. */
@Composable
private fun TarjetaVencido(vencido: PrestamoVencido) {
    val prestamo = vencido.prestamo
    val rojo = LocalColoresEstado.current.vencido
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    prestamo.solicitante,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when (vencido.diasDeAtraso) {
                        0L -> "Venció hoy"
                        1L -> "1 día de atraso"
                        else -> "${vencido.diasDeAtraso} días de atraso"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = rojo,
                )
            }
            Text(
                "Préstamo #${prestamo.id} · ${prestamo.estado.name} · prestado el ${FECHA.format(prestamo.fechaPrestamo)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Fecha esperada: ${FECHA_HORA.format(prestamo.fechaDevolucionPrevista)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            vencido.pendientes.forEach { detalle ->
                Text("• ${detalle.codigoInventario} · ${detalle.nombreEquipo}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ------------------------------------------------ historial de mantenimiento

@Composable
private fun PestanaMantenimientos(
    estado: HistorialMantenimientoUi,
    onCodigo: (String) -> Unit,
    onConsultar: () -> Unit,
    onEscanear: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        encabezadoReporte(PestanaReporte.MANTENIMIENTOS)
        item(key = "codigo") {
            Column {
                OutlinedTextField(
                    value = estado.codigo,
                    onValueChange = onCodigo,
                    label = { Text("Código Inventario Equipo") },
                    placeholder = { Text("Vacío = todos") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = onEscanear) {
                            Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Escanear QR")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onConsultar() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onConsultar, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
                    Text(if (estado.cargando) "Consultando…" else "Consultar Historial")
                }
            }
        }
        estado.error?.let { item(key = "error") { TextoError(it) } }
        if (estado.consultado && !estado.cargando && estado.error == null && estado.registros.isEmpty()) {
            item(key = "vacio") { TextoVacio("No hay mantenimientos registrados para este código.") }
        }
        items(estado.registros, key = { it.id }) { FilaMantenimiento(it) }
    }
}

/** Columnas de escritorio: ID Mant., Código Equipo, Equipo, Tipo, Ingreso, Salida, Resultado, Técnico. */
@Composable
private fun FilaMantenimiento(m: Mantenimiento) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${m.id} · ${m.codigoInventario}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    m.resultado?.name ?: "EN PROCESO",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (m.resultado == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(m.nombreEquipo, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${m.tipo.name} · Técnico: ${m.responsable}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Ingreso: ${FECHA.format(m.fechaIngreso)} · Salida: ${m.fechaSalida?.let(FECHA::format) ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --------------------------------------------------------------- auditoría

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestanaAuditoria(
    estado: AuditoriaUi,
    onPeriodo: (PeriodoAuditoria) -> Unit,
    onAccion: (AccionAuditoria?) -> Unit,
    onRefrescar: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = estado.cargando, onRefresh = onRefrescar, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            item(key = "periodo") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodoAuditoria.entries.forEach { periodo ->
                        FilterChip(
                            selected = estado.periodo == periodo,
                            onClick = { onPeriodo(periodo) },
                            label = { Text(periodo.etiqueta) },
                        )
                    }
                }
            }
            item(key = "accion") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = estado.accion == null, onClick = { onAccion(null) }, label = { Text("Todas") })
                    AccionAuditoria.entries.forEach { accion ->
                        FilterChip(
                            selected = estado.accion == accion,
                            onClick = { onAccion(if (estado.accion == accion) null else accion) },
                            label = { Text(accion.etiqueta) },
                        )
                    }
                }
            }
            estado.error?.let { item(key = "error") { TextoError(it) } }
            if (estado.cargado && !estado.cargando && estado.registros.isEmpty() && estado.error == null) {
                item(key = "vacio") { TextoVacio("No hay registros con estos filtros.") }
            }
            if (estado.registros.size >= LIMITE_AUDITORIA) {
                item(key = "limite") {
                    Text(
                        "Se muestran los $LIMITE_AUDITORIA registros más recientes. Acorte el período o filtre por acción.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(estado.registros, key = { it.id }) { registro ->
                FilaAuditoria(registro)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FilaAuditoria(registro: RegistroAuditoria) {
    val fallido = registro.accion == AccionAuditoria.LOGIN_FALLIDO.name
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                AccionAuditoria.etiquetaDe(registro.accion),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (fallido) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(FECHA_CORTA.format(registro.fecha), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            listOfNotNull(registro.usuario ?: "Usuario no identificado", registro.entidad).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (registro.detalle.isNotBlank()) {
            Text(
                registro.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ----------------------------------------------------------------- comunes

private fun LazyListScope.encabezadoReporte(pestana: PestanaReporte) {
    item(key = "titulo-${pestana.name}") {
        Text(
            pestana.reporte.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
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
            .padding(vertical = 24.dp),
    )
}

@Composable
private fun TextoError(texto: String) {
    Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

private fun hayDatos(pestana: PestanaReporte, ui: ReportesUiState) = when (pestana) {
    PestanaReporte.POR_ESTADO -> ui.porEstado.equipos.isNotEmpty()
    PestanaReporte.VENCIDOS -> ui.vencidos.lista.isNotEmpty()
    PestanaReporte.MANTENIMIENTOS -> ui.historial.registros.isNotEmpty()
    PestanaReporte.AUDITORIA -> ui.auditoria.registros.isNotEmpty()
}

/** Abre el menú de compartir de Android (correo, Drive, WhatsApp…) con el CSV como texto. */
private fun Context.compartirTexto(asunto: String, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, asunto)
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    startActivity(Intent.createChooser(envio, asunto))
}
