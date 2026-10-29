package com.universidad.sigelab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Color(0xFF1F4E9A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E6FB),
    onPrimaryContainer = Color(0xFF0B2A5C),
    secondary = Color(0xFF4A6FA5),
    secondaryContainer = Color(0xFFE3ECF8),
    onSecondaryContainer = Color(0xFF122B4F),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFF5F7FB),
    surfaceContainerLow = Color.White,
    surfaceVariant = Color(0xFFE4E9F1),
    onSurfaceVariant = Color(0xFF4A5566),
    outlineVariant = Color(0xFFCBD3DF),
    error = Color(0xFFC62828),
    errorContainer = Color(0xFFFDE2E1),
    onErrorContainer = Color(0xFF6B0F0F),
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF0B2A5C),
    primaryContainer = Color(0xFF1F3E73),
    onPrimaryContainer = Color(0xFFD8E6FB),
    secondary = Color(0xFFB5C8E8),
    secondaryContainer = Color(0xFF2A3A55),
    onSecondaryContainer = Color(0xFFDCE6F7),
    background = Color(0xFF0F141B),
    surface = Color(0xFF0F141B),
    surfaceContainerLow = Color(0xFF171E28),
    surfaceVariant = Color(0xFF263040),
    onSurfaceVariant = Color(0xFFB8C2D1),
    outlineVariant = Color(0xFF3A4556),
    error = Color(0xFFFF8A80),
    errorContainer = Color(0xFF5C1A17),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Colores por estado de equipo, tomados del diagrama "Flujo principal del ciclo de un equipo". */
@Immutable
data class ColoresEstado(
    val total: Color,
    val disponible: Color,
    val prestado: Color,
    val danado: Color,
    val mantenimiento: Color,
    val baja: Color,
    val vencido: Color,
)

private val ColoresClaros = ColoresEstado(
    total = Color(0xFF1F4E9A),
    disponible = Color(0xFF2E7D32),
    prestado = Color(0xFF1565C0),
    // Tonos oscurecidos para que el texto de las etiquetas de estado cumpla contraste 4.5:1.
    danado = Color(0xFF9A5B00),
    mantenimiento = Color(0xFFBF360C),
    baja = Color(0xFF616161),
    vencido = Color(0xFFC62828),
)

private val ColoresOscuros = ColoresEstado(
    total = Color(0xFFA9C7FF),
    disponible = Color(0xFF81C784),
    prestado = Color(0xFF90CAF9),
    danado = Color(0xFFFFCC80),
    mantenimiento = Color(0xFFFFAB91),
    baja = Color(0xFFBDBDBD),
    vencido = Color(0xFFEF9A9A),
)

val LocalColoresEstado = staticCompositionLocalOf { ColoresClaros }

@Composable
fun SigelabTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalColoresEstado provides if (oscuro) ColoresOscuros else ColoresClaros) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            content = contenido,
        )
    }
}
