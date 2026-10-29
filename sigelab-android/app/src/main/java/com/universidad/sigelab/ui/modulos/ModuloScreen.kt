package com.universidad.sigelab.ui.modulos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.universidad.sigelab.domain.model.Modulo
import com.universidad.sigelab.ui.dashboard.icono

/**
 * Punto de entrada de cada módulo. Las reglas de negocio ya están en domain/usecase;
 * aquí se conectan sus pantallas (listas, formularios, escáner QR) a medida que se construyen.
 */
@Composable
fun ModuloScreen(modulo: Modulo, soloLectura: Boolean, onVolver: () -> Unit) {
    PantallaSimple(titulo = modulo.titulo, onVolver = onVolver) {
        Icon(modulo.icono(), contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        if (soloLectura) AssistChip(onClick = {}, label = { Text("Solo consulta") })
        Text(modulo.descripcion(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SinAccesoScreen(onVolver: () -> Unit) {
    PantallaSimple(titulo = "Acceso restringido", onVolver = onVolver) {
        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(48.dp))
        Text("Su rol no tiene permiso para este módulo.", textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaSimple(titulo: String, onVolver: () -> Unit, contenido: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
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
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            contenido()
        }
    }
}

private fun Modulo.descripcion(): String = when (this) {
    Modulo.LABORATORIOS -> "CRUD de laboratorios con código único (CatalogoRepository)."
    Modulo.TIPOS_EQUIPO -> "Catálogo de categorías de equipo (CatalogoRepository)."
    Modulo.EQUIPOS -> "Registro y edición de equipos (estado inicial DISPONIBLE) y consulta por QR del CódigoInventario, que no cambia el estado (EquipoRepository)."
    Modulo.PRESTAMOS -> "Crear préstamo con validación de disponibilidad y duplicados, y devoluciones por condición BUENO / DAÑADO (CrearPrestamoUseCase, ProcesarDevolucionUseCase)."
    Modulo.MANTENIMIENTO -> "Ingreso de equipos dañados o en revisión y cierre REPARADO → DISPONIBLE / NO REPARABLE → BAJA (IngresarMantenimientoUseCase, CerrarMantenimientoUseCase)."
    Modulo.USUARIOS -> "Alta de usuario en SQL Server y registro del rostro inicial con /face/register (RegistrarUsuarioUseCase + CamaraFrontal)."
    Modulo.REPORTES -> "Préstamos vencidos, historial por equipo, auditoría de acciones y consultas por estado."
}
