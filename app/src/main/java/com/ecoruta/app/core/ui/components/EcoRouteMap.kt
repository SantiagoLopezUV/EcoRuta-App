package com.ecoruta.app.core.ui.components

// Mapa ilustrativo de Tuluá compartido por los módulos que muestran recorridos con paradas
// Dibuja cuadras, el río, el trazado ortogonal de la ruta y las paradas numeradas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoWhite

// Posición relativa de una parada dentro del mapa, con valores entre cero y uno
data class EcoRouteMapPoint(
    val x: Float,
    val y: Float,
)

// Mapa ilustrativo de Tuluá con el trazado de la ruta y sus paradas numeradas
@Composable
fun EcoRouteMap(
    points: List<EcoRouteMapPoint>,
    modifier: Modifier = Modifier,
    description: String = stringResource(R.string.recycler_map_description),
    mapHeight: Dp = 140.dp,
    // Marca el vehículo con un punto verde cuando se comparte la ubicación en vivo
    showLiveLocation: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(mapHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val left = w * 0.1314f
            val top = h * 0.1617f
            val gridW = w * (1f - 0.1314f - 0.0971f)
            val gridH = h * (1f - 0.1617f * 2)
            val cols = 4
            val rows = 3
            val gap = 8.dp.toPx()
            val blockW = (gridW - gap * (cols - 1)) / cols
            val blockH = (gridH - gap * (rows - 1)) / rows
            for (r in 0 until rows) for (c in 0 until cols) {
                drawRoundRect(
                    color = EcoWhite,
                    topLeft = Offset(left + c * (blockW + gap), top + r * (blockH + gap)),
                    size = Size(blockW, blockH),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
            val riverX = w * 0.65f
            val river = Path().apply {
                moveTo(riverX, 0f)
                cubicTo(riverX - w * 0.07f, h * 0.3f, riverX + w * 0.07f, h * 0.55f, riverX - w * 0.01f, h * 0.7f)
                cubicTo(riverX - w * 0.05f, h * 0.82f, riverX + w * 0.03f, h * 0.92f, riverX + w * 0.02f, h)
            }
            drawPath(river, color = EcoSubtitle, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
            drawRoundRect(
                color = EcoGreenSecondary,
                topLeft = Offset(w * 0.1571f, h * 0.42f),
                size = Size(w * 0.1286f, h * 0.14f),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            if (showLiveLocation) {
                val dot = Offset(w * (0.1571f + 0.1286f), h * 0.42f)
                drawCircle(color = EcoWhite, radius = 6.dp.toPx(), center = dot)
                drawCircle(color = EcoAccent, radius = 4.dp.toPx(), center = dot)
            }
            // Trazado ortogonal que une las paradas en el orden asignado
            if (points.size > 1) {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val position = Offset(point.x * w, point.y * h)
                    if (index == 0) {
                        path.moveTo(position.x, position.y)
                    } else {
                        val previous = points[index - 1]
                        path.lineTo(point.x * w, previous.y * h)
                        path.lineTo(position.x, position.y)
                    }
                }
                drawPath(path, color = EcoPrimary, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
            }
        }
        RouteMapLabel(R.string.recycler_map_label_carrera_26, 108f, 5f, maxWidth.value, maxHeight.value)
        RouteMapLabel(R.string.recycler_map_label_calle_25, 112f, 22f, maxWidth.value, maxHeight.value)
        RouteMapLabel(R.string.recycler_map_label_river, 250f, 74f, maxWidth.value, maxHeight.value)
        RouteMapLabel(R.string.recycler_map_label_calle_27, 20f, 118f, maxWidth.value, maxHeight.value)
        points.forEachIndexed { index, point ->
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * point.x - 16.dp, y = maxHeight * point.y - 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text((index + 1).toString(), style = EcoTextStyles.FieldLabel, color = EcoWhite)
            }
        }
        overlay()
    }
}

// Etiqueta del mapa ubicada en coordenadas de referencia de 350 dp de ancho por 140 dp de alto que se escalan al tamaño real del mapa
@Composable
private fun RouteMapLabel(labelRes: Int, x: Float, y: Float, mapWidth: Float, mapHeight: Float) {
    val scale = mapWidth / 350f
    val verticalScale = mapHeight / 140f
    Text(
        stringResource(labelRes),
        style = EcoTextStyles.FieldLabel,
        color = EcoSubtitle,
        modifier = Modifier.offset(x = (x * scale).dp, y = (y * verticalScale).dp),
    )
}
