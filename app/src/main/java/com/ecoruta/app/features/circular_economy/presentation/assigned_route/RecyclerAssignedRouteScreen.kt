package com.ecoruta.app.features.circular_economy.presentation.assigned_route

// Vista composable de la ruta de reciclaje asignada al reciclador vinculado a una empresa
// Muestra el recorrido en el mapa ilustrativo, las paradas ordenadas y la autorización de ubicación

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.core.ui.components.EcoCheckboxRow
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Parada de la ruta con su barrio, dirección y posición relativa dentro del mapa ilustrativo
data class RouteStop(
    val neighborhood: String,
    val address: String,
    val mapX: Float,
    val mapY: Float,
)

// Ruta de reciclaje asignada por la empresa al reciclador vinculado
data class AssignedRecyclingRoute(
    val code: String,
    val name: String,
    val date: String,
    val company: String,
    val stops: List<RouteStop>,
)

// Acciones disponibles en la pantalla de ruta asignada
data class AssignedRouteActions(
    val onBack: () -> Unit = {},
    val onShareLocationChange: (Boolean) -> Unit = {},
    val onViewOnMap: () -> Unit = {},
    val onTabClick: (RecyclerTab) -> Unit = {},
)

// Pantalla con la ruta del día, sus paradas y el consentimiento para compartir la ubicación
@Composable
fun RecyclerAssignedRouteScreen(
    route: AssignedRecyclingRoute,
    shareLocation: Boolean,
    actions: AssignedRouteActions,
) {
    EcoRoleTabScreenLayout(
        items = RecyclerTab.entries,
        selectedTab = RecyclerTab.ROUTES,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
        contentSpacing = 12.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycler_route_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.recycler_route_subtitle, route.code, route.name), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.recycler_route_summary, route.date, route.stops.size, route.company),
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.recycler_route_assigned), style = EcoTextStyles.FieldLabel, color = EcoGreenSecondary)
        }
        RouteMap(stops = route.stops)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            route.stops.forEachIndexed { index, stop ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EcoWhite),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = EcoTextStyles.FieldLabel, color = EcoPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stop.neighborhood, style = EcoTextStyles.LinkText, color = EcoTitle)
                        Text(stop.address, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoFieldBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.recycler_route_location_title), style = EcoTextStyles.FieldLabel, color = EcoPrimary)
            Text(stringResource(R.string.recycler_route_location_body), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            EcoCheckboxRow(
                checked = shareLocation,
                onCheckedChange = actions.onShareLocationChange,
                text = stringResource(R.string.recycler_route_location_consent),
            )
        }
        EcoPrimaryButton(text = stringResource(R.string.recycler_route_view_on_map), onClick = actions.onViewOnMap, height = 56.dp)
    }
}

// Mapa ilustrativo de Tuluá con el trazado de la ruta y sus paradas numeradas
@Composable
private fun RouteMap(stops: List<RouteStop>) {
    val description = stringResource(R.string.recycler_map_description)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
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
            // Trazado ortogonal que une las paradas en el orden asignado
            if (stops.size > 1) {
                val path = Path()
                stops.forEachIndexed { index, stop ->
                    val point = Offset(stop.mapX * w, stop.mapY * h)
                    if (index == 0) {
                        path.moveTo(point.x, point.y)
                    } else {
                        val previous = stops[index - 1]
                        path.lineTo(stop.mapX * w, previous.mapY * h)
                        path.lineTo(point.x, point.y)
                    }
                }
                drawPath(path, color = EcoPrimary, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))
            }
        }
        RouteMapLabel(R.string.recycler_map_label_carrera_26, 108f, 5f, maxWidth.value)
        RouteMapLabel(R.string.recycler_map_label_calle_25, 112f, 22f, maxWidth.value)
        RouteMapLabel(R.string.recycler_map_label_river, 250f, 74f, maxWidth.value)
        RouteMapLabel(R.string.recycler_map_label_calle_27, 20f, 118f, maxWidth.value)
        stops.forEachIndexed { index, stop ->
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * stop.mapX - 16.dp, y = maxHeight * stop.mapY - 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", style = EcoTextStyles.FieldLabel, color = EcoWhite)
            }
        }
    }
}

// Etiqueta del mapa ubicada en coordenadas del diseño de 350 dp de ancho
@Composable
private fun RouteMapLabel(labelRes: Int, x: Float, y: Float, mapWidth: Float) {
    val scale = mapWidth / 350f
    Text(
        stringResource(labelRes),
        style = EcoTextStyles.FieldLabel,
        color = EcoSubtitle,
        modifier = Modifier.offset(x = (x * scale).dp, y = y.dp),
    )
}

// Ruta ilustrativa de reciclaje en Tuluá para el modo diseño y las previsualizaciones
val SampleAssignedRoute = AssignedRecyclingRoute(
    code = "RT-031",
    name = "Ruta Sajonia",
    date = "17 oct. 2026",
    company = "Aseo Tuluá S. A. S.",
    stops = listOf(
        RouteStop("Sajonia", "Carrera 27 # 32-14", 0.237f, 0.294f),
        RouteStop("La Campiña", "Calle 25 # 28-09", 0.477f, 0.494f),
        RouteStop("Centro", "Calle 27 # 26-35", 0.237f, 0.814f),
    ),
)

@Preview(name = "Ruta asignada · Reciclador vinculado", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerAssignedRoutePreview() {
    EcoRutaTheme {
        RecyclerAssignedRouteScreen(route = SampleAssignedRoute, shareLocation = false, actions = AssignedRouteActions())
    }
}
