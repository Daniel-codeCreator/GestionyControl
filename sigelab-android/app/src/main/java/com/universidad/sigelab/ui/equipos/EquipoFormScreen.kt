package com.universidad.sigelab.ui.equipos

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.usecase.CampoEquipo
import com.universidad.sigelab.ui.components.MensajeError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipoFormScreen(
    onVolver: () -> Unit,
    onGuardado: (idEquipo: Int) -> Unit,
    viewModel: EquipoFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.guardadoId) {
        val id = ui.guardadoId ?: return@LaunchedEffect
        viewModel.navegacionHecha()
        onGuardado(id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.esNuevo) "Nuevo equipo" else "Actualizar equipo") },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !ui.guardando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
            if (ui.cargando) {
                CircularProgressIndicator(Modifier.padding(32.dp))
            } else {
                Formulario(ui, viewModel)
            }
        }
    }
}

@Composable
private fun Formulario(ui: EquipoFormUiState, vm: EquipoFormViewModel) {
    val f = ui.formulario

    Column(
        Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Igual que escritorio: el código EQP00001 se genera solo y no se edita. Además es el contenido
        // del QR, así que cambiarlo dejaría inservibles las etiquetas impresas.
        OutlinedTextField(
            value = if (ui.esNuevo) "(automático)" else f.codigo,
            onValueChange = {},
            label = { Text("Código") },
            enabled = false,
            supportingText = { Text(if (ui.esNuevo) "Lo asigna el sistema al guardar (EQP00001, EQP00002…)." else "Es el contenido de la etiqueta QR.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        CampoTexto("Nombre", f.nombre, vm::onNombre, ui.errores[CampoEquipo.NOMBRE])

        Spacer(Modifier.height(8.dp))
        Selector(
            etiqueta = "Tipo de equipo",
            opciones = ui.tipos,
            seleccionadoId = f.idTipo,
            idDe = { it.id },
            nombreDe = { it.nombre },
            onSeleccion = vm::onTipo,
            error = ui.errores[CampoEquipo.TIPO],
        )
        Spacer(Modifier.height(8.dp))
        Selector(
            etiqueta = "Laboratorio",
            opciones = ui.laboratorios,
            seleccionadoId = f.idLaboratorio,
            idDe = { it.id },
            nombreDe = { "${it.codigo} · ${it.nombre}" },
            onSeleccion = vm::onLaboratorio,
            error = ui.errores[CampoEquipo.LABORATORIO],
        )

        CampoTexto("Marca", f.marca, vm::onMarca, ui.errores[CampoEquipo.MARCA], opcional = true)
        CampoTexto("Modelo", f.modelo, vm::onModelo, ui.errores[CampoEquipo.MODELO], opcional = true)
        CampoTexto(
            "Serie",
            f.serie,
            vm::onSerie,
            ui.errores[CampoEquipo.SERIE],
            opcional = true,
            capitalizacion = KeyboardCapitalization.Characters,
            accion = ImeAction.Done,
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (ui.esNuevo) {
                    "Estado inicial: Disponible. Después solo cambia con préstamos, devoluciones y mantenimiento."
                } else {
                    "Estado actual: ${ui.estadoActual.etiqueta}. Solo cambia con préstamos, devoluciones y mantenimiento."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        MensajeError(ui.error)
        if (ui.error != null && (ui.tipos.isEmpty() || ui.laboratorios.isEmpty())) {
            TextButton(onClick = vm::cargar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Reintentar")
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = vm::guardar,
            enabled = !ui.guardando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (ui.guardando) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            } else {
                Text(if (ui.esNuevo) "Guardar" else "Actualizar")
            }
        }
    }
}

@Composable
private fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    opcional: Boolean = false,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    accion: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(if (opcional) "$etiqueta (opcional)" else etiqueta) },
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = capitalizacion, keyboardType = KeyboardType.Text, imeAction = accion),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccionadoId: Int,
    idDe: (T) -> Int,
    nombreDe: (T) -> String,
    onSeleccion: (Int) -> Unit,
    error: String?,
) {
    var abierto by remember { mutableStateOf(false) }
    val seleccionado = opciones.firstOrNull { idDe(it) == seleccionadoId }

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it && opciones.isNotEmpty() },
    ) {
        OutlinedTextField(
            value = seleccionado?.let(nombreDe).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            isError = error != null,
            supportingText = if (error != null) { { Text(error) } } else null,
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(nombreDe(opcion)) },
                    onClick = {
                        onSeleccion(idDe(opcion))
                        abierto = false
                    },
                )
            }
        }
    }
}
