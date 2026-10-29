package com.universidad.sigelab.ui.catalogos

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universidad.sigelab.domain.model.EstadoRegistro
import com.universidad.sigelab.domain.usecase.CampoCatalogo
import com.universidad.sigelab.domain.usecase.MENSAJE_CAPACIDAD_INVALIDA

/** "Gestión de Laboratorios" de escritorio: Código (automático), Nombre, Ubicación, Capacidad y Estado. */
@Composable
fun LaboratoriosScreen(
    onVolver: () -> Unit,
    viewModel: LaboratoriosViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    CatalogoScaffold(
        titulo = "Gestión de Laboratorios",
        textoNuevo = "Nuevo laboratorio",
        textoVacio = "Aún no hay laboratorios registrados.",
        ui = ui,
        mensajes = viewModel.mensajes,
        clave = { it.id },
        tituloFila = { "${it.codigo} · ${it.nombre}" },
        detalleFila = { lab -> listOfNotNull(lab.ubicacion.ifBlank { null }, "Capacidad: ${lab.capacidad}").joinToString(" · ") },
        onVolver = onVolver,
        onRefrescar = viewModel::cargar,
        onNuevo = viewModel::abrirNuevo,
        onEditar = viewModel::abrirFormulario,
        etiquetaFila = { lab -> AssistChip(onClick = {}, enabled = false, label = { Text(lab.estado.name) }) },
    ) { laboratorio ->
        val esNuevo = laboratorio.id == 0
        var nombre by rememberSaveable(laboratorio.id) { mutableStateOf(laboratorio.nombre) }
        var ubicacion by rememberSaveable(laboratorio.id) { mutableStateOf(laboratorio.ubicacion) }
        var capacidad by rememberSaveable(laboratorio.id) {
            mutableStateOf(if (laboratorio.capacidad > 0) laboratorio.capacidad.toString() else "")
        }
        var estado by rememberSaveable(laboratorio.id) { mutableStateOf(laboratorio.estado) }

        DialogoFormulario(
            titulo = if (esNuevo) "Nuevo laboratorio" else "Actualizar laboratorio",
            textoConfirmar = if (esNuevo) "Guardar" else "Actualizar",
            enviando = ui.enviando,
            error = ui.errorDialogo,
            onGuardar = {
                val numero = capacidad.trim().toIntOrNull()
                if (numero == null) {
                    viewModel.mostrarError(CampoCatalogo.CAPACIDAD, MENSAJE_CAPACIDAD_INVALIDA)
                } else {
                    viewModel.guardar(laboratorio.copy(nombre = nombre, ubicacion = ubicacion, capacidad = numero, estado = estado))
                }
            },
            onCancelar = viewModel::cerrarFormulario,
        ) {
            Text(
                "Código: ${if (esNuevo) "(automático)" else laboratorio.codigo}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            CampoCatalogoTexto(
                etiqueta = "Nombre",
                valor = nombre,
                onCambio = {
                    nombre = it
                    viewModel.limpiarError(CampoCatalogo.NOMBRE)
                },
                error = ui.erroresCampo[CampoCatalogo.NOMBRE],
                enabled = !ui.enviando,
            )
            CampoCatalogoTexto(
                etiqueta = "Ubicación",
                valor = ubicacion,
                onCambio = {
                    ubicacion = it
                    viewModel.limpiarError(CampoCatalogo.UBICACION)
                },
                error = ui.erroresCampo[CampoCatalogo.UBICACION],
                enabled = !ui.enviando,
                ayuda = "Edificio, piso o aula (opcional)",
            )
            CampoCatalogoTexto(
                etiqueta = "Capacidad",
                valor = capacidad,
                onCambio = {
                    capacidad = it.filter(Char::isDigit).take(6)
                    viewModel.limpiarError(CampoCatalogo.CAPACIDAD)
                },
                error = ui.erroresCampo[CampoCatalogo.CAPACIDAD],
                enabled = !ui.enviando,
                tipoTeclado = KeyboardType.Number,
            )
            // Escritorio crea siempre en ACTIVO; el estado solo se cambia al actualizar.
            if (!esNuevo) {
                Text("Estado", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    EstadoRegistro.entries.forEachIndexed { i, opcion ->
                        SegmentedButton(
                            selected = estado == opcion,
                            onClick = { estado = opcion },
                            enabled = !ui.enviando,
                            shape = SegmentedButtonDefaults.itemShape(i, EstadoRegistro.entries.size),
                        ) { Text(opcion.name) }
                    }
                }
            }
        }
    }
}

/** "Gestión de Tipos de Equipo" de escritorio: solo Nombre y Descripción, y solo altas. */
@Composable
fun TiposEquipoScreen(
    onVolver: () -> Unit,
    viewModel: TiposEquipoViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    CatalogoScaffold(
        titulo = "Gestión de Tipos de Equipo",
        textoNuevo = "Nuevo tipo",
        textoVacio = "Aún no hay tipos de equipo registrados.",
        ui = ui,
        mensajes = viewModel.mensajes,
        clave = { it.id },
        tituloFila = { it.nombre },
        detalleFila = { it.descripcion },
        onVolver = onVolver,
        onRefrescar = viewModel::cargar,
        onNuevo = viewModel::abrirNuevo,
        onEditar = null,
    ) { tipo ->
        var nombre by rememberSaveable { mutableStateOf(tipo.nombre) }
        var descripcion by rememberSaveable { mutableStateOf(tipo.descripcion.orEmpty()) }

        DialogoFormulario(
            titulo = "Nuevo tipo de equipo",
            textoConfirmar = "Guardar",
            enviando = ui.enviando,
            error = ui.errorDialogo,
            onGuardar = { viewModel.guardar(tipo.copy(nombre = nombre, descripcion = descripcion)) },
            onCancelar = viewModel::cerrarFormulario,
        ) {
            CampoCatalogoTexto(
                etiqueta = "Nombre",
                valor = nombre,
                onCambio = {
                    nombre = it
                    viewModel.limpiarError(CampoCatalogo.NOMBRE)
                },
                error = ui.erroresCampo[CampoCatalogo.NOMBRE],
                enabled = !ui.enviando,
                ayuda = "Por ejemplo: Laptop, Proyector, Router",
            )
            CampoCatalogoTexto(
                etiqueta = "Descripción",
                valor = descripcion,
                onCambio = {
                    descripcion = it
                    viewModel.limpiarError(CampoCatalogo.DESCRIPCION)
                },
                error = ui.erroresCampo[CampoCatalogo.DESCRIPCION],
                enabled = !ui.enviando,
                lineas = 3,
            )
        }
    }
}
