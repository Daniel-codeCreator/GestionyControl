package com.universidad.sigelab.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.universidad.sigelab.domain.model.EstadoEquipo
import com.universidad.sigelab.ui.theme.LocalColoresEstado

@Composable
fun EstadoEquipo.color(): Color {
    val colores = LocalColoresEstado.current
    return when (this) {
        EstadoEquipo.DISPONIBLE -> colores.disponible
        EstadoEquipo.PRESTADO -> colores.prestado
        EstadoEquipo.DANADO -> colores.danado
        EstadoEquipo.MANTENIMIENTO -> colores.mantenimiento
        EstadoEquipo.BAJA -> colores.baja
    }
}

/** Píldora con el estado del equipo, en el color del diagrama del ciclo de vida. */
@Composable
fun EtiquetaEstado(estado: EstadoEquipo, modifier: Modifier = Modifier) {
    val color = estado.color()
    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            estado.etiqueta,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
