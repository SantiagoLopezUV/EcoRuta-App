package com.ecoruta.app.features.routes.presentation.map

// Vista composable del mapa de rutas por barrio con Google Maps Compose
// Renderiza líneas de micro-rutas, ubicación del usuario y estado del camión

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.components.EcoBottomBar
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoServiceCard
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
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
import java.util.Date
import java.util.Locale

private val SpanishColombia: Locale = Locale.forLanguageTag("es-CO")

// Formatea una distancia en metros con separador de miles colombiano
fun formatMeters(meters: Int): String = NumberFormat.getIntegerInstance(SpanishColombia).format(meters) + " m"

// Servicios principales disponibles en la pantalla de inicio ciudadano y público
enum class PublicService(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
) {
    RESIDUOS_ESPECIALES(R.string.service_special_waste_title, R.string.service_special_waste_description, R.drawable.ic_truck),
    BASURA(R.string.service_garbage_title, R.string.service_garbage_description, R.drawable.ic_trash),
    RECICLAJE(R.string.service_recycling_title, R.string.service_recycling_description, R.drawable.ic_recycle),
    REPORTES(R.string.service_reports_title, R.string.service_reports_description, R.drawable.ic_report_alert),
}

// Acciones de interacción de la pantalla de inicio
data class InicioPublicoActions(
    val onHelpClick: () -> Unit = {},
    val onNotificationsClick: () -> Unit = {},
    val onLoginClick: () -> Unit = {},
    val onLogoutClick: () -> Unit = {},
    val onServiceClick: (PublicService) -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun InicioPublicoScreen(
    actions: InicioPublicoActions,
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = false,
    userName: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WelcomeHeader(
                title = when {
                    !isSignedIn -> stringResource(R.string.home_welcome)
                    userName.isNullOrBlank() -> stringResource(R.string.home_greeting_no_name)
                    else -> stringResource(R.string.home_greeting, userName)
                },
                onHelpClick = actions.onHelpClick,
                onNotificationsClick = actions.onNotificationsClick,
                onLogoutClick = if (isSignedIn) actions.onLogoutClick else null,
            )

            if (!isSignedIn) EcoPrimaryButton(
                text = stringResource(R.string.home_login),
                onClick = actions.onLoginClick,
                height = 48.dp,
                cornerRadius = 24.dp,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = EcoAccent,
                        spotColor = EcoAccent,
                    ),
            )

            ServicesSection(onServiceClick = actions.onServiceClick)
        }

        EcoBottomBar(
            items = CitizenTab.entries,
            selectedItem = CitizenTab.INICIO,
            onItemClick = actions.onTabClick,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun WelcomeHeader(
    title: String,
    onHelpClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLogoutClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 185.dp)
            .clip(RoundedCornerShape(topStart = 28.dp))
            .background(EcoPrimary)
            .background(
                Brush.horizontalGradient(
                    0f to EcoPrimary,
                    0.55f to EcoAccent.copy(alpha = 0.35f),
                    1f to EcoPrimary,
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 24.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EcoWhite),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ecoruta_logo_circular),
                    contentDescription = stringResource(R.string.logo_ecoruta_description),
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_name), style = EcoTextStyles.Boton, color = EcoWhite)
            Spacer(Modifier.weight(1f))
            HeaderIconButton(
                iconRes = R.drawable.ic_help,
                contentDescription = stringResource(R.string.home_help),
                onClick = onHelpClick,
            )
            Spacer(Modifier.width(8.dp))
            HeaderIconButton(
                iconRes = R.drawable.ic_bell,
                contentDescription = stringResource(R.string.home_notifications),
                onClick = onNotificationsClick,
            )
        }
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = if (onLogoutClick == null) 20.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = EcoTextStyles.Titulo, color = EcoWhite)
            Text(stringResource(R.string.home_welcome_message), style = EcoTextStyles.Subtitulo, color = EcoWhite)
        }
        if (onLogoutClick != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(topStart = 25.dp, bottomStart = 25.dp))
                        .background(EcoAccent)
                        .clickable(role = Role.Button, onClick = onLogoutClick)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.home_logout), style = EcoTextStyles.Enlace, color = EcoBlack)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(EcoWhite.copy(alpha = 0.1f))
            .border(1.dp, EcoWhite.copy(alpha = 0.2f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ServicesSection(onServiceClick: (PublicService) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_services_title), style = EcoTextStyles.Titulo, color = EcoTitle)
            Text(stringResource(R.string.home_services_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PublicService.entries.chunked(2).forEach { rowServices ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowServices.forEach { service ->
                        EcoServiceCard(
                            title = stringResource(service.titleRes),
                            description = stringResource(service.descriptionRes),
                            iconRes = service.iconRes,
                            iconSize = if (service == PublicService.REPORTES) 32.dp else 20.dp,
                            onClick = { onServiceClick(service) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 170.dp),
                        )
                    }
                }
            }
        }
    }
}

// Pantalla de búsqueda y selección de barrio para consultar rutas de recolección
data class NeighborhoodSelectionActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onNeighborhoodSelected: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun NeighborhoodSelectionScreen(state: NeighborhoodSelectionUiState, actions: NeighborhoodSelectionActions) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.MAPA,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = actions.onContinue,
                enabled = !state.isLoading,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.neighborhood_title), style = EcoTextStyles.Titulo, color = EcoTitle)
            Text(stringResource(R.string.neighborhood_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        EcoTextField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            label = stringResource(R.string.registration_neighborhood_label),
            placeholder = stringResource(R.string.neighborhood_search_placeholder),
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            },
        )
        Text(stringResource(R.string.neighborhood_list_label), style = EcoTextStyles.EtiquetaCampo, color = EcoTitle)
        when {
            state.isLoading -> CenteredLoading()
            state.loadError != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EcoErrorMessage(text = stringResource(state.loadError))
                EcoTextLink(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            else -> {
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.neighborhood_sample_hint))
                val visible = state.visibleNeighborhoods
                if (visible.isEmpty()) {
                    Text(stringResource(R.string.neighborhood_no_results), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
                }
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visible.forEach { neighborhood ->
                        NeighborhoodRow(
                            neighborhood = neighborhood,
                            selected = neighborhood.id == state.selectedId,
                            onClick = { actions.onNeighborhoodSelected(neighborhood.id) },
                        )
                    }
                }
            }
        }
        state.selectionError?.let { EcoErrorMessage(text = stringResource(it)) }
    }
}

@Composable
private fun NeighborhoodRow(neighborhood: Neighborhood, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (selected) EcoFieldBackground else EcoWhite)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) EcoPrimary else EcoBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = if (selected) EcoPrimary else EcoSubtitle, modifier = Modifier.size(20.dp))
        Text(neighborhood.name, style = EcoTextStyles.Enlace, color = EcoTitle, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) EcoPrimary else EcoWhite)
                .border(1.5.dp, if (selected) EcoPrimary else EcoSubtitle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_checkbox_check), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(16.dp))
            }
        }
    }
}

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
    EcoTabScreenLayout(selectedTab = CitizenTab.MAPA, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.route_title), style = EcoTextStyles.Titulo, color = EcoTitle)
            if (route != null) {
                Text(stringResource(R.string.debris_address_in_city, route.neighborhood.name), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
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
            style = EcoTextStyles.EtiquetaCampo,
            color = EcoTitle,
            modifier = Modifier
                .offset(x = pinX + 44.dp, y = pinY + 22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EcoWhite)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Text(
            text = stringResource(route.liveStatus.labelRes()),
            style = EcoTextStyles.EtiquetaCampo,
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
            style = EcoTextStyles.EtiquetaNavegacion,
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
            Text(stringResource(R.string.route_ordinary, route.neighborhood.name), style = EcoTextStyles.Boton, color = EcoTitle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            route.days.forEach { day ->
                Text(
                    text = stringResource(dayNameRes(day)),
                    style = EcoTextStyles.Enlace,
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
            style = EcoTextStyles.Subtitulo,
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
private fun CenteredLoading() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = EcoPrimary) }
}

// Pantalla de listado de notificaciones de proximidad, escombros y reportes
data class NotificationsActions(
    val onBack: () -> Unit = {},
    val onNotificationClick: (AppNotification) -> Unit = {},
    val onGoHome: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun NotificationsScreen(state: NotificationsUiState, actions: NotificationsActions) {
    val isEmpty = state is NotificationsUiState.Loaded && state.notifications.isEmpty()
    val bottomActions: (@Composable ColumnScope.() -> Unit)? = if (isEmpty) {
        { EcoPrimaryButton(text = stringResource(R.string.report_back_home), onClick = actions.onGoHome) }
    } else {
        null
    }
    EcoTabScreenLayout(
        selectedTab = CitizenTab.INICIO,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = bottomActions,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.notifications_title), style = EcoTextStyles.Titulo, color = EcoTitle)
            if (!isEmpty) Text(stringResource(R.string.notifications_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        when (state) {
            NotificationsUiState.Loading -> CenteredLoading()
            NotificationsUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.notifications_load_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            is NotificationsUiState.Loaded ->
                if (state.notifications.isEmpty()) {
                    BellMessage(
                        title = stringResource(R.string.notifications_empty_title),
                        body = stringResource(R.string.notifications_empty_body),
                        highlighted = false,
                    )
                } else {
                    if (state.isSample) EcoInfoHint(text = stringResource(R.string.notifications_sample_hint))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.notifications.forEach { notification ->
                            NotificationCard(notification = notification, onClick = { actions.onNotificationClick(notification) })
                        }
                    }
                }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoFieldBackground)
            .border(1.dp, if (notification.read) EcoFieldBackground else EcoBorder, shape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.notifications_open), onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(notification.kind.iconRes()),
                    contentDescription = null,
                    tint = EcoWhite,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(notification.label, style = EcoTextStyles.EtiquetaCampo, color = EcoTitle, modifier = Modifier.weight(1f))
            if (notification.read) {
                EcoStatusChip(stringResource(R.string.notifications_read), EcoSubtitle, EcoWhite)
            } else {
                EcoStatusChip(stringResource(R.string.notifications_unread), EcoAccent, EcoBlack)
            }
        }
        Text(notification.title, style = EcoTextStyles.Boton, color = EcoTitle)
        Text(notification.body, style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        Text(formatNotificationDate(notification.createdAtMillis), style = EcoTextStyles.EtiquetaCampo, color = EcoSubtitle)
    }
}

private fun NotificationKind.iconRes(): Int = when (this) {
    NotificationKind.PROXIMIDAD -> R.drawable.ic_bell
    NotificationKind.SOLICITUD_ESCOMBROS -> R.drawable.ic_checkbox_check
    NotificationKind.REPORTE -> R.drawable.ic_profile_clipboard
}

private fun formatNotificationDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy '·' h:mm a", SpanishColombia).format(Date(millis))

@Composable
private fun BellMessage(title: String, body: String, highlighted: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(EcoFieldBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (highlighted) EcoPrimary else EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = if (highlighted) EcoWhite else EcoTitle, modifier = Modifier.size(28.dp))
        }
        Text(
            title,
            style = if (highlighted) EcoTextStyles.Titulo else EcoTextStyles.Boton,
            color = EcoTitle,
            textAlign = TextAlign.Center,
        )
        Text(body, style = EcoTextStyles.Subtitulo, color = EcoSubtitle, textAlign = TextAlign.Center)
    }
}

@Composable
fun GuestNotificationsScreen(onBack: () -> Unit, onLogin: () -> Unit, onTabClick: (CitizenTab) -> Unit) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.INICIO,
        onBack = onBack,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.home_login), onClick = onLogin, trailingIconRes = R.drawable.ic_arrow_right)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.notifications_title), style = EcoTextStyles.Titulo, color = EcoTitle)
            Text(stringResource(R.string.notifications_guest_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        BellMessage(
            title = stringResource(R.string.notifications_guest_title),
            body = stringResource(R.string.notifications_guest_body),
            highlighted = true,
        )
    }
}

@Preview(name = "Inicio Público (Visitante)", widthDp = 393, heightDp = 900)
@Composable
private fun InicioPublicoGuestPreview() {
    EcoRutaTheme {
        InicioPublicoScreen(
            actions = InicioPublicoActions(),
            isSignedIn = false,
        )
    }
}

@Preview(name = "Inicio Ciudadano (Con Sesión)", widthDp = 393, heightDp = 900)
@Composable
private fun InicioPublicoSignedInPreview() {
    EcoRutaTheme {
        InicioPublicoScreen(
            actions = InicioPublicoActions(),
            isSignedIn = true,
            userName = "Camila",
        )
    }
}

@Preview(name = "Selección de Barrio", widthDp = 393, heightDp = 852)
@Composable
private fun NeighborhoodSelectionPreview() {
    EcoRutaTheme {
        val list = listOf(
            Neighborhood("la-esperanza", "La Esperanza"),
            Neighborhood("torobajo", "Torobajo"),
            Neighborhood("maridiaz", "Maridíaz"),
        )
        NeighborhoodSelectionScreen(
            state = NeighborhoodSelectionUiState(
                isLoading = false,
                neighborhoods = list,
                selectedId = "torobajo",
            ),
            actions = NeighborhoodSelectionActions(),
        )
    }
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

@Preview(name = "Notificaciones", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsPreview() {
    EcoRutaTheme {
        val notifications = listOf(
            AppNotification(
                id = "not-1",
                kind = NotificationKind.PROXIMIDAD,
                label = "Radar de proximidad",
                title = "El camión recolector se acerca a Torobajo",
                body = "Está a menos de 250 m de tu ubicación configurada.",
                read = false,
                createdAtMillis = 1_744_900_000_000L,
            ),
            AppNotification(
                id = "not-2",
                kind = NotificationKind.REPORTE,
                label = "Reporte ciudadano",
                title = "Tu reporte fue aprobado",
                body = "El equipo operativo programó la atención en tu sector.",
                read = true,
                createdAtMillis = 1_744_800_000_000L,
            ),
        )
        NotificationsScreen(
            state = NotificationsUiState.Loaded(notifications = notifications, isSample = false),
            actions = NotificationsActions(),
        )
    }
}

@Preview(name = "Notificaciones sin sesión", widthDp = 393, heightDp = 852)
@Composable
private fun GuestNotificationsPreview() {
    EcoRutaTheme {
        GuestNotificationsScreen(onBack = {}, onLogin = {}, onTabClick = {})
    }
}
