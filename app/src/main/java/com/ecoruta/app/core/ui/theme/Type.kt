package com.ecoruta.app.core.ui.theme

// Escala tipográfica y estilos de texto del sistema de diseño
// Define pesos, tamaños y alturas de línea para títulos, subtítulos, botones y etiquetas

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val InterFontFamily: FontFamily = FontFamily.Default
val ManropeFontFamily: FontFamily = FontFamily.Default

// Catálogo de estilos tipográficos utilizados en las pantallas de la aplicación
object EcoTextStyles {

    // Estilo para títulos principales de pantalla
    val Titulo = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.75).sp,
    )

    // Estilo para subtítulos, párrafos descriptivos y textos de entrada
    val Subtitulo = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    // Estilo para etiquetas de campos de formulario e insignias pequeñas
    val EtiquetaCampo = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    )

    // Estilo para las etiquetas de las pestañas en la barra de navegación inferior
    val EtiquetaNavegacion = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    )

    // Estilo para enlaces de texto e hipervínculos interactivos
    val Enlace = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    // Estilo para el texto de botones principales, secundarios y encabezados de tarjeta
    val Boton = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    // Estilo destacado para cifras numéricas, saldos de puntos y dígitos de verificación
    val Numeros = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 30.sp,
    )

    // Estilo para el nombre de la marca en la pantalla de presentación inicial
    val MarcaSplash = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 60.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.75).sp,
    )
}

// Asignación de los estilos personalizados hacia la tipografía base de Material 3
val Typography = Typography(
    headlineLarge = EcoTextStyles.Titulo,
    bodyLarge = EcoTextStyles.Subtitulo,
    bodyMedium = EcoTextStyles.Subtitulo,
    labelLarge = EcoTextStyles.Boton,
    labelMedium = EcoTextStyles.EtiquetaCampo,
)
