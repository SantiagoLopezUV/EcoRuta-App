package com.ecoruta.app.core.ui.components

// Generador visual de patrón con apariencia de código QR para previsualización
// Dibuja marcas de posicionamiento y módulos deterministas a partir de una semilla

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTitle

// Dibuja un patrón visual de ejemplo con forma de código QR basado en el texto recibido
@Composable
fun EcoSampleQrCode(seed: String, modifier: Modifier = Modifier, faded: Boolean = false) {
    val modules = 25
    val hash = seed.hashCode()
    val color = if (faded) EcoSubtitle.copy(alpha = 0.4f) else EcoTitle
    Canvas(modifier.fillMaxSize()) {
        val cell = size.minDimension / modules
        fun module(row: Int, col: Int) = drawRect(color, Offset(col * cell, row * cell), Size(cell, cell))
        fun finder(row: Int, col: Int) {
            for (r in 0 until 7) for (c in 0 until 7) {
                val ring = r == 0 || r == 6 || c == 0 || c == 6
                val core = r in 2..4 && c in 2..4
                if (ring || core) module(row + r, col + c)
            }
        }
        fun inFinderZone(r: Int, c: Int) = (r < 8 && c < 8) || (r < 8 && c >= modules - 8) || (r >= modules - 8 && c < 8)
        for (r in 0 until modules) for (c in 0 until modules) {
            if (inFinderZone(r, c)) continue
            if (((r * 31 + c * 17 + r * c + hash) and 0x7fffffff) % 5 < 2) module(r, c)
        }
        finder(0, 0)
        finder(0, modules - 7)
        finder(modules - 7, 0)
    }
}
