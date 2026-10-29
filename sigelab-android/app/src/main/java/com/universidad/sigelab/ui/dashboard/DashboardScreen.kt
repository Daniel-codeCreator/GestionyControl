package com.universidad.sigelab.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.DashboardMetricas
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.domain.model.Rol
import com.universidad.sigelab.domain.model.Usuario
import com.universidad.sigelab.ui.theme.LocalColoresEstado
import com.universidad.sigelab.ui.theme.SigelabTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onAbrirModulo: (Modulo) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    DashboardContenido(
        ui = ui,
        onRefrescar = viewModel::refrescar,
        onCerrarSesion = viewModel::cerrarSesion,
        onAbrirModulo = onAbrirModulo,
    )
}

private data class Metrica(
    val titulo: String,
    val valor: Int?,
    val color: Color,
    val icono: ImageVector,
    val resaltar: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContenido(
    ui: DashboardUiState,
    onRefrescar: () -> Unit,
    onCerrarSesion: () -> Unit,
    onAbrirModulo: (Modulo) -> Unit,
) {
    var confirmarSalida by remember { mutableStateOf(false) }
    val colores = LocalColoresEstado.current
    val m = ui.metricas

    val equipos = listOf(
        Metrica("Total equipos", m?.totalEquipos, colores.total, Icons.Outlined.Devices),
        Metrica("Disponibles", m?.disponibles, colores.disponible, Icons.Outlined.CheckCircle),
        Metrica("Prestados", m?.prestados, colores.prestado, Icons.Outlined.SwapHoriz),
        Metrica("Dañados", m?.danados, colores.danado, Icons.Outlined.ReportProblem),
        Metrica("En mantenimiento", m?.enMantenimiento, colores.mantenimiento, Icons.Outlined.Build),
        Metrica("De baja", m?.deBaja, colores.baja, Icons.Outlined.Block),
    )
    val prestamos = listOf(
        Metrica("Préstamos activos", m?.prestamosActivos, colores.prestado, Icons.AutoMirrored.Outlined.Assignment),
        Metrica(
            "Préstamos vencidos",
            m?.prestamosVencidos,
            colores.vencido,
            Icons.Outlined.EventBusy,
            resaltar = (m?.prestamosVencidos ?: 0) > 0,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SIGELAB", fontWeight = FontWeight.Bold)
                        ui.usuario?.let {
                            Text(
                                "${it.nombreCompleto} · ${it.rol.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefrescar) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar métricas")
                    }
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Cerrar sesión")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = ui.cargando,
            onRefresh = onRefrescar,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LazyVerticalGrid(
                // 2 columnas en teléfono, más en tablet.
                columns = GridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EstadoActualizacion(m?.actualizadoEn, ui.error)
                }

                encabezado("Equipos")
                items(equipos, key = { it.titulo }) { TarjetaMetrica(it) }

                encabezado("Préstamos")
                items(prestamos, key = { it.titulo }) { TarjetaMetrica(it) }

                encabezado("Módulos")
                items(ui.modulos, key = { it.modulo.name }) { item ->
                    BotonModulo(item, onClick = { onAbrirModulo(item.modulo) })
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Para volver a entrar necesitará su contraseña y su rostro.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onCerrarSesion()
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            },
        )
    }
}

private fun LazyGridScope.encabezado(texto: String) {
    item(span = { GridItemSpan(maxLineSpan) }, contentType = "encabezado") {
        Text(
            texto.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun EstadoActualizacion(actualizadoEn: Instant?, error: String?) {
    val hora = actualizadoEn?.let { FORMATO_HORA.format(it.atZone(ZoneId.systemDefault())) }
    when {
        error != null -> Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (hora != null) "$error. Mostrando datos de las $hora." else error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        hora != null -> Text(
            "Actualizado a las $hora · se actualiza cada 15 s",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> Text(
            "Cargando métricas…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TarjetaMetrica(metrica: Metrica) {
    val descripcion = "${metrica.titulo}: ${metrica.valor ?: "sin datos"}"
    ElevatedCard(Modifier.clearAndSetSemantics { contentDescription = descripcion }) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(metrica.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(metrica.icono, contentDescription = null, tint = metrica.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(targetState = metrica.valor, label = "valor-${metrica.titulo}") { valor ->
                Text(
                    valor?.toString() ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (metrica.resaltar) metrica.color else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                metrica.titulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BotonModulo(item: ModuloUi, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(item.modulo.icono(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.modulo.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                if (item.soloLectura) {
                    Text(
                        "Solo consulta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

fun Modulo.icono(): ImageVector = when (this) {
    Modulo.LABORATORIOS -> Icons.Outlined.Science
    Modulo.TIPOS_EQUIPO -> Icons.Outlined.Category
    Modulo.EQUIPOS -> Icons.Outlined.Computer
    Modulo.PRESTAMOS -> Icons.Outlined.SwapHoriz
    Modulo.MANTENIMIENTO -> Icons.Outlined.Build
    Modulo.USUARIOS -> Icons.Outlined.PersonAdd
    Modulo.REPORTES -> Icons.Outlined.Assessment
}

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss")

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun DashboardPreview() {
    SigelabTheme {
        DashboardContenido(
            ui = DashboardUiState(
                usuario = Usuario(1, "admin", "Ana Pérez", Rol.ADMIN),
                metricas = DashboardMetricas(48, 31, 9, 3, 2, 3, 6, 2, Instant.now()),
                cargando = false,
                modulos = Modulo.entries.map { ModuloUi(it, soloLectura = false) },
            ),
            onRefrescar = {},
            onCerrarSesion = {},
            onAbrirModulo = {},
        )
    }
}
