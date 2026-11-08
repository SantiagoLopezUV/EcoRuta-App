package com.ecoruta.app.core.ui.theme

// Dimensiones y medidas constantes compartidas en las pantallas de la aplicación
// Centraliza márgenes laterales, alturas de controles, radios de esquina y tamaños de icono

import androidx.compose.ui.unit.dp

// Objeto con las medidas estándar para controles y contenedores visuales
object EcoDimens {
    // Margen horizontal estándar aplicado a las pantallas principales
    val ScreenHorizontalPadding = 24.dp

    // Altura estándar para botones principales y campos de texto
    val ControlHeight = 56.dp

    // Altura estándar para botones secundarios con borde
    val SecondaryControlHeight = 52.dp

    // Radio de redondeo para botones y campos de formulario
    val ControlCornerRadius = 12.dp

    // Tamaño estándar para iconos ubicados dentro de campos y botones
    val InputIconSize = 20.dp

    // Grosor del borde en tarjetas y contenedores delineados
    val BorderWidth = 1.dp

    // Grosor del borde que resalta una tarjeta activa
    val HighlightBorderWidth = 1.5.dp

    // Alto mínimo de chips y botones de cabecera para que sean fáciles de tocar
    val MinTouchTarget = 44.dp

    // Radio de los botones de la cabecera de marca
    val HeaderActionCornerRadius = 14.dp

    // Radio que deja los chips con forma de píldora
    val PillCornerRadius = 999.dp

    // Relleno interno de los chips de filtro
    val ChipHorizontalPadding = 12.dp
    val ChipVerticalPadding = 8.dp

    // Separación entre chips de una misma fila
    val ChipSpacing = 8.dp

    // Radio y relleno interno de las tarjetas de lista
    val CardCornerRadius = 16.dp
    val CardPadding = 16.dp

    // Separación entre los bloques dentro de una tarjeta y entre tarjetas de una lista
    val CardContentSpacing = 12.dp

    // Separación corta entre una etiqueta y su valor
    val LabelValueSpacing = 4.dp
}
