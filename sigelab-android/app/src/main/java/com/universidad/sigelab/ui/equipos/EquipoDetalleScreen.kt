package com.universidad.sigelab.ui.equipos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.Equipo
import com.universidad.sigelab.ui.components.EtiquetaEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipoDetalleScreen(
    onVolver: () -> Unit,
    onEditar: (Int) -> Unit,
    viewModel: EquipoDetalleViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val equipo = ui.equipo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(equipo?.codigoInventario ?: "Equipo") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (equipo != null && ui.puedeEditar) {
                        IconButton(onClick = { onEditar(equipo.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar equipo")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                ui.cargando -> CircularProgressIndicator(Modifier.padding(32.dp))
                equipo == null -> Text(
                    "Este equipo no está en la copia local. Vuelva a la lista y actualícela.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
                else -> DetalleEquipo(equipo)
            }
        }
    }
}

@Composable
private fun DetalleEquipo(equipo: Equipo) {
    val contexto = LocalContext.current

    Column(
        Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        equipo.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    EtiquetaEstado(equipo.estado)
                }
                Spacer(Modifier.height(12.dp))
                // Misma ficha que muestra escritorio al leer un QR.
                Dato("Código", equipo.codigoInventario)
                Dato("Tipo", equipo.tipo)
                Dato("Marca", equipo.marca.ifBlank { "—" })
                Dato("Modelo", equipo.modelo.ifBlank { "—" })
                Dato("Serie", equipo.numeroSerie.ifBlank { "—" })
                Dato("Laboratorio", equipo.laboratorio)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Etiqueta QR", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                CodigoQr(
                    equipo,
                    Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(8.dp))
                Text(equipo.codigoInventario, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (impresionDisponible()) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { imprimirEtiqueta(contexto, equipo) }) {
                        Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Imprimir etiqueta")
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Leer este QR solo consulta el equipo. Su estado cambia únicamente con préstamos, devoluciones y mantenimiento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
    }
}
