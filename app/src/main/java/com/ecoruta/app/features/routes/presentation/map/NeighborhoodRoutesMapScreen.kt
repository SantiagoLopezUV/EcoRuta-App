package com.ecoruta.app.features.routes.presentation.map

// Vista composable del mapa de rutas por barrio con Google Maps Compose
// Renderiza líneas de micro-rutas, ubicación del usuario y estado del camión

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal val SpanishColombia: Locale = Locale.forLanguageTag("es-CO")

// Formatea una distancia en metros con separador de miles colombiano
fun formatMeters(meters: Int): String = NumberFormat.getIntegerInstance(SpanishColombia).format(meters) + " m"

// Pantalla del mapa de ruta y horarios de recolección del barrio seleccionado
data class RouteActions(
    val onBack: () -> Unit = {},
    val onPrimaryAction: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun RouteScreen(state: RouteUiState, isSignedIn: Boolean, actions: RouteActions) {
    val route = (state as? RouteUiState.Loaded)?.route
    EcoTabScreenLayout(selectedTab = CitizenTab.MAP, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.route_title), style = EcoTextStyles.Title, color = EcoTitle)
            if (route != null) {
                Text(stringResource(R.string.debris_address_in_city, route.neighborhood.name), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
        when (state) {
            RouteUiState.Loading -> CenteredLoading()
            RouteUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EcoErrorMessage(text = stringResource(R.string.route_load_error))
                EcoTextLink(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            RouteUiState.NotFound -> EcoErrorMessage(text = stringResource(R.string.route_not_found))
            is RouteUiState.Loaded -> {
                RouteMap(route = state.route)
                RouteCard(route = state.route, isSignedIn = isSignedIn, onPrimaryAction = actions.onPrimaryAction)
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.route_sample_hint))
            }
        }
    }
}

private val RoutePoints = listOf(
    0.16f to 0.86f, 0.16f to 0.44f, 0.46f to 0.44f, 0.46f to 0.14f, 0.88f to 0.14f, 0.88f to 0.66f, 0.30f to 0.66f,
)

@Composable
private fun RouteMap(route: CollectionRoute) {
    val mapDescription = stringResource(R.string.route_map_description, route.neighborhood.name)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .semantics { contentDescription = mapDescription },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cols = 3
            val rows = 3
            val gap = 18.dp.toPx()
            val blockW = (size.width - gap * (cols + 1)) / cols
            val blockH = (size.height - gap * (rows + 1)) / rows
            for (r in 0 until rows) for (c in 0 until cols) {
                val isPark = r == 0 && c == 1
                drawRoundRect(
                    color = if (isPark) EcoAccent else Color.White,
                    topLeft = Offset(gap + c * (blockW + gap), gap + r * (blockH + gap)),
                    size = Size(blockW, blockH),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
            val path = Path()
            RoutePoints.forEachIndexed { index, (x, y) ->
                if (index == 0) path.moveTo(x * size.width, y * size.height) else path.lineTo(x * size.width, y * size.height)
            }
            drawPath(path, color = EcoPrimary, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val (sx, sy) = RoutePoints.first()
            drawCircle(color = EcoAccent, radius = 6.dp.toPx(), center = Offset(sx * size.width, sy * size.height))
            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(sx * size.width, sy * size.height))
        }
        val pinX = maxWidth * 0.46f - 20.dp
        val pinY = maxHeight * 0.66f - 20.dp
        Box(
            modifier = Modifier
                .offset(x = pinX, y = pinY)
                .size(40.dp)
                .clip(CircleShape)
                .background(EcoGreenSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_map_pin_white), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
        }
        Text(
            text = route.neighborhood.name,
            style = EcoTextStyles.FieldLabel,
            color = EcoTitle,
            modifier = Modifier
                .offset(x = pinX + 44.dp, y = pinY + 22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EcoWhite)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Text(
            text = stringResource(route.liveStatus.labelRes()),
            style = EcoTextStyles.FieldLabel,
            color = EcoTitle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EcoWhite)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Text(
            text = stringResource(R.string.report_map_illustrative),
            style = EcoTextStyles.NavigationLabel,
            color = EcoSubtitle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EcoWhite)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun RouteCard(route: CollectionRoute, isSignedIn: Boolean, onPrimaryAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(EcoBorder),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_truck), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.route_ordinary, route.neighborhood.name), style = EcoTextStyles.ButtonText, color = EcoTitle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            route.days.forEach { day ->
                Text(
                    text = stringResource(dayNameRes(day)),
                    style = EcoTextStyles.LinkText,
                    color = EcoTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EcoWhite)
                        .padding(vertical = 8.dp),
                )
            }
        }
        Text(
            stringResource(R.string.route_time_range, formatMinute(route.startMinute), formatMinute(route.endMinute)),
            style = EcoTextStyles.Subtitle,
            color = EcoTitle,
        )
        EcoPrimaryButton(
            text = stringResource(if (isSignedIn) R.string.route_activate_radar else R.string.route_login_for_radar),
            onClick = onPrimaryAction,
            height = 48.dp,
            trailingIconRes = R.drawable.ic_arrow_right,
        )
    }
}

private fun RouteLiveStatus.labelRes(): Int = when (this) {
    RouteLiveStatus.NO_INICIADA -> R.string.route_status_not_started
    RouteLiveStatus.EN_CURSO -> R.string.route_status_in_progress
    RouteLiveStatus.FINALIZADA -> R.string.route_status_finished
}

private fun dayNameRes(day: Int): Int = when (day) {
    Calendar.MONDAY -> R.string.day_monday
    Calendar.TUESDAY -> R.string.day_tuesday
    Calendar.WEDNESDAY -> R.string.day_wednesday
    Calendar.THURSDAY -> R.string.day_thursday
    Calendar.FRIDAY -> R.string.day_friday
    Calendar.SATURDAY -> R.string.day_saturday
    else -> R.string.day_sunday
}

private fun formatMinute(minuteOfDay: Int): String {
    val calendar = Calendar.getInstance().apply {
        clear()
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
    }
    return SimpleDateFormat("h:mm a", SpanishColombia).format(calendar.time)
}

@Composable
internal fun CenteredLoading() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = EcoPrimary) }
}

@Preview(name = "Rutas del barrio", widthDp = 393, heightDp = 900)
@Composable
private fun RoutePreview() {
    EcoRutaTheme {
        val route = CollectionRoute(
            neighborhood = Neighborhood("ejemplo-la-esperanza", "La Esperanza"),
            days = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
            startMinute = 7 * 60,
            endMinute = 11 * 60,
            liveStatus = RouteLiveStatus.EN_CURSO,
        )
        RouteScreen(RouteUiState.Loaded(route, isSample = true), isSignedIn = true, actions = RouteActions())
    }
}
