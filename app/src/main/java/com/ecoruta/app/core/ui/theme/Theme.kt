package com.ecoruta.app.core.ui.theme

// Tema visual principal de la aplicación basado en Material Design 3
// Integra el esquema de colores, la tipografía y las formas personalizadas

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EcoRutaColorScheme = lightColorScheme(
    primary = EcoPrimary,
    onPrimary = EcoWhite,
    secondary = EcoGreenSecondary,
    onSecondary = EcoWhite,
    tertiary = EcoAccent,
    onTertiary = EcoBlack,
    background = EcoWhite,
    onBackground = EcoTitle,
    surface = EcoWhite,
    onSurface = EcoTitle,
    surfaceVariant = EcoFieldBackground,
    onSurfaceVariant = EcoSubtitle,
    outline = EcoBorder,
    error = EcoError,
    onError = EcoWhite,
)

@Composable
fun EcoRutaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EcoRutaColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
