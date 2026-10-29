package com.universidad.sigelab.ui.equipos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.ui.components.EtiquetaEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquiposScreen(
    onVolver: () -> Unit,
    onAbrirEquipo: (Int) -> Unit,
    onNuevoEquipo: () -> Unit,
    onEscanear: () -> Unit,
    viewModel: EquiposViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    // El texto vive en la pantalla para que el cursor no salte; el ViewModel solo filtra.
    var busqueda by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.onBusqueda(busqueda) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestión de Equipos")
                        if (ui.soloLectura) {
                            Text(
                                "Solo consulta",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onEscanear) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Escanear QR")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!ui.soloLectura) {
                ExtendedFloatingActionButton(
                    onClick = onNuevoEquipo,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Nuevo equipo") },
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.sincronizando,
            onRefresh = viewModel::sincronizar,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Espacio inferior para que el botón flotante no tape el último equipo.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "busqueda") {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = {
                            busqueda = it
                            viewModel.onBusqueda(it)
                        },
                        placeholder = { Text("Buscar por código, nombre o serie") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (busqueda.isNotEmpty()) {
                                IconButton(onClick = {
                                    busqueda = ""
                                    viewModel.onBusqueda("")
                                }) { Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda") }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = "filtros") {
                    FiltrosEstado(ui, viewModel::onFiltro)
                }
                ui.error?.let { error ->
                    item(key = "error") { AvisoSinConexion(error) }
                }
                if (ui.equipos.isEmpty() && !ui.sincronizando) {
                    item(key = "vacio") {
                        Text(
                            if (ui.total > 0) "Ningún equipo coincide con la búsqueda o el filtro."
                            else "Aún no hay equipos guardados. Deslice hacia abajo para actualizar.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                }
                items(ui.equipos, key = { it.id }) { equipo ->
                    FilaEquipo(equipo, onClick = { onAbrirEquipo(equipo.id) })
                }
            }
        }
    }
}

@Composable
private fun FiltrosEstado(ui: EquiposUiState, onFiltro: (EstadoEquipo?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = ui.filtro == null,
            onClick = { onFiltro(null) },
            label = { Text("Todos · ${ui.total}") },
        )
        EstadoEquipo.entries.forEach { estado ->
            FilterChip(
                selected = ui.filtro == estado,
                // Tocar el filtro activo lo quita.
                onClick = { onFiltro(if (ui.filtro == estado) null else estado) },
                label = { Text("${estado.etiqueta} · ${ui.conteos[estado] ?: 0}") },
            )
        }
    }
}

@Composable
private fun FilaEquipo(equipo: Equipo, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(equipo.codigoInventario, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(equipo.nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(equipo.tipo, equipo.marca, equipo.modelo, equipo.laboratorio).filter { it.isNotBlank() }.joinToString(" · "),
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
private fun AvisoSinConexion(mensaje: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(mensaje, style = MaterialTheme.typography.bodySmall)
        }
    }
}
