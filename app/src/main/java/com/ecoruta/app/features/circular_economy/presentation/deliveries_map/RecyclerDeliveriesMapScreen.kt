package com.ecoruta.app.features.circular_economy.presentation.deliveries_map

// Vista composable de mapa con ofertas de reciclaje para recicladores
// Renderiza solicitudes cercanas con filtros por material y botón Reclamar 1:1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.core.navigation.components.recyclerTabs
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.circular_economy.presentation.publish.MaterialType
import com.ecoruta.app.features.circular_economy.presentation.publish.PublicationStatus
import com.ecoruta.app.features.circular_economy.presentation.publish.labelRes

// Acciones disponibles en el mapa de entregas del reciclador
data class RecyclerDeliveriesActions(
    val onMaterialFilterChange: (MaterialType?) -> Unit = {},
    val onStatusFilterChange: (PublicationStatus) -> Unit = {},
    val onDeliverySelected: (String) -> Unit = {},
    val onExpandList: () -> Unit = {},
    val onCollapseList: () -> Unit = {},
    val onTakeDelivery: (RecyclerDelivery) -> Unit = {},
    val onMyLocationClick: () -> Unit = {},
    val onMyPickupsClick: () -> Unit = {},
    val onTabClick: (RecyclerTab) -> Unit = {},
)

// Materiales que se pueden filtrar en el mapa, además de la opción «Todos»
private val FilterMaterials = listOf(MaterialType.PET, MaterialType.CARDBOARD, MaterialType.GLASS)

// Estados de publicación que el reciclador puede consultar en el mapa
private val FilterStatuses = listOf(PublicationStatus.DISPONIBLE, PublicationStatus.EN_CAMINO)

// Pantalla principal del reciclador con el mapa ilustrativo, filtros y detalle de la entrega elegida
@Composable
fun RecyclerDeliveriesMapScreen(
    state: RecyclerDeliveriesUiState,
    tabs: List<RecyclerTab>,
    actions: RecyclerDeliveriesActions,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedDelivery
    EcoRoleTabScreenLayout(
        items = tabs,
        selectedTab = RecyclerTab.MAP,
        onTabClick = actions.onTabClick,
        onBack = if (state.isListExpanded) actions.onCollapseList else null,
        contentSpacing = if (state.isListExpanded) 8.dp else 16.dp,
        headerAction = { MyPickupsButton(onClick = actions.onMyPickupsClick) },
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (state.isListExpanded) 4.dp else 8.dp)) {
            Text(stringResource(R.string.recycler_map_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                stringResource(if (state.isListExpanded) R.string.recycler_map_subtitle_list else R.string.recycler_map_subtitle),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterRow {
                FilterChip(stringResource(R.string.coupon_category_all), state.materialFilter == null) { actions.onMaterialFilterChange(null) }
                FilterMaterials.forEach { material ->
                    FilterChip(stringResource(material.labelRes()), state.materialFilter == material) { actions.onMaterialFilterChange(material) }
                }
            }
            if (!state.isListExpanded) {
                FilterRow {
                    FilterStatuses.forEach { status ->
                        FilterChip(stringResource(status.filterLabelRes()), state.statusFilter == status) { actions.onStatusFilterChange(status) }
                    }
                }
            }
        }
        DeliveriesMap(
            deliveries = state.visibleDeliveries,
            selectedId = selected?.id,
            compact = state.isListExpanded,
            onDeliverySelected = actions.onDeliverySelected,
            onMyLocationClick = actions.onMyLocationClick,
        )
        when {
            selected == null -> Text(
                stringResource(R.string.recycler_map_empty),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            state.isListExpanded -> {
                ExpandedDeliveryCard(delivery = selected, onCollapse = actions.onCollapseList, onTake = { actions.onTakeDelivery(selected) })
                state.visibleDeliveries.filter { it.id != selected.id }.forEach { delivery ->
                    DeliveryRow(delivery = delivery, onClick = { actions.onDeliverySelected(delivery.id) })
                }
            }
            else -> SelectedDeliveryCard(delivery = selected, onViewDetail = actions.onExpandList)
        }
    }
}

// Pastilla de la cabecera que abre la lista de recogidas reservadas por el reciclador
@Composable
private fun MyPickupsButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(EcoDimens.MinTouchTarget)
            .clip(RoundedCornerShape(EcoDimens.HeaderActionCornerRadius))
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = EcoDimens.ChipHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EcoDimens.ChipSpacing),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_list),
            contentDescription = null,
            tint = EcoTitle,
            modifier = Modifier.size(EcoDimens.InputIconSize),
        )
        Text(stringResource(R.string.recycler_pickups_title), style = EcoTextStyles.LinkText, color = EcoTitle)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

// Chip de filtro en forma de píldora; el seleccionado usa el color de acento
@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) EcoAccent else EcoWhite)
            .then(if (selected) Modifier else Modifier.border(1.dp, EcoFieldBackground, shape))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = EcoTextStyles.LinkText, color = if (selected) EcoBlack else EcoSubtitle)
    }
}

// Mapa esquemático de Tuluá con calles, río, parque y marcadores de las entregas
@Composable
private fun DeliveriesMap(
    deliveries: List<RecyclerDelivery>,
    selectedId: String?,
    compact: Boolean,
    onDeliverySelected: (String) -> Unit,
    onMyLocationClick: () -> Unit,
) {
    val mapDescription = stringResource(R.string.recycler_map_description)
    val mapHeight = if (compact) 94.dp else 266.dp
    // En la vista compacta el mapa se desplaza hacia arriba para mostrar la franja de los marcadores
    val verticalShift = if (compact) 0.26f else 0f
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(mapHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .semantics { contentDescription = mapDescription },
    ) {
        val fullHeight = maxWidth * (266f / 350f)
        val shiftPx = fullHeight * verticalShift
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = fullHeight.toPx()
            val dy = -shiftPx.toPx()
            // Manzanas de la red de calles y carreras
            val left = w * 0.1314f
            val top = h * 0.1617f + dy
            val gridW = w * (1f - 0.1314f - 0.0971f)
            val gridH = h * (1f - 0.1617f * 2)
            val cols = 4
            val rows = 3
            val gap = 10.dp.toPx()
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
            // Río Tuluá atravesando la ciudad de norte a sur
            val riverX = w * (227f / 350f)
            val river = Path().apply {
                moveTo(riverX + w * 0.03f, dy)
                cubicTo(riverX - w * 0.08f, h * 0.25f + dy, riverX + w * 0.08f, h * 0.45f + dy, riverX - w * 0.01f, h * 0.6f + dy)
                cubicTo(riverX - w * 0.07f, h * 0.75f + dy, riverX + w * 0.06f, h * 0.88f + dy, riverX + w * 0.02f, h + dy)
            }
            drawPath(river, color = EcoSubtitle, style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
            // Parque Céspedes
            drawRoundRect(
                color = EcoGreenSecondary,
                topLeft = Offset(w * (55f / 350f), h * (112f / 266f) + dy),
                size = Size(w * (45f / 350f), h * (37f / 266f)),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
            // Ubicación de ejemplo del reciclador
            val user = Offset(w * (166f / 350f), h * (205f / 266f) + dy)
            drawCircle(color = EcoWhite, radius = 6.dp.toPx(), center = user)
            drawCircle(color = EcoAccent, radius = 4.dp.toPx(), center = user)
        }
        if (!compact) {
            MapLabel(R.string.recycler_map_label_carrera_26, 108f, 5f, EcoSubtitle, maxWidth)
            MapLabel(R.string.recycler_map_label_calle_25, 112f, 44f, EcoSubtitle, maxWidth)
            MapLabel(R.string.recycler_map_label_boyaca, 248f, 53f, EcoPrimary, maxWidth)
            MapLabel(R.string.recycler_map_label_cespedes, 14f, 164f, EcoPrimary, maxWidth)
            MapLabel(R.string.recycler_map_label_river, 245f, 140f, EcoSubtitle, maxWidth)
            MapLabel(R.string.recycler_map_label_calle_27, 20f, 224f, EcoSubtitle, maxWidth)
        }
        MapLabel(R.string.recycler_map_label_center, 132f, if (compact) 72f else 166f, EcoPrimary, maxWidth)
        deliveries.forEach { delivery ->
            val isSelected = delivery.id == selectedId
            val markerWidth: Dp = if (isSelected) 54.dp else if (compact) 44.dp else 36.dp
            val markerHeight: Dp = if (isSelected) 48.dp else markerWidth
            val centerY = if (compact) 42.dp else fullHeight * delivery.mapY
            DeliveryMarker(
                selected = isSelected,
                width = markerWidth,
                height = markerHeight,
                onClick = { onDeliverySelected(delivery.id) },
                modifier = Modifier.offset(
                    x = maxWidth * delivery.mapX - markerWidth / 2,
                    y = centerY - markerHeight / 2,
                ),
            )
        }
        val locationLabel = stringResource(R.string.recycler_map_my_location)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(48.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(EcoWhite)
                .clickable(role = Role.Button, onClickLabel = locationLabel, onClick = onMyLocationClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_map_pin), contentDescription = locationLabel, tint = EcoTitle, modifier = Modifier.size(20.dp))
        }
    }
}

// Etiqueta de texto ubicada en coordenadas del diseño de 350 × 266 escaladas al ancho disponible
@Composable
private fun MapLabel(labelRes: Int, x: Float, y: Float, color: androidx.compose.ui.graphics.Color, mapWidth: Dp) {
    val scale = mapWidth / 350.dp
    Text(
        text = stringResource(labelRes),
        style = EcoTextStyles.FieldLabel,
        color = color,
        modifier = Modifier.offset(x = (x * scale).dp, y = (y * scale).dp),
    )
}

// Marcador de una publicación disponible; el seleccionado se resalta con el color primario
@Composable
private fun DeliveryMarker(
    selected: Boolean,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(if (selected) RoundedCornerShape(12.dp) else CircleShape)
            .background(if (selected) EcoPrimary else EcoGreenSecondary)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_package),
            contentDescription = null,
            tint = EcoWhite,
            modifier = Modifier.size(if (selected) 26.dp else 20.dp),
        )
    }
}

// Tarjeta con la entrega elegida en el mapa y el acceso a su detalle
@Composable
private fun SelectedDeliveryCard(delivery: RecyclerDelivery, onViewDetail: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(delivery.status.filterLabelRes()), style = EcoTextStyles.FieldLabel, color = EcoGreenSecondary)
        Text(delivery.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
        Text(
            stringResource(R.string.recycler_delivery_summary, delivery.area, delivery.volume, delivery.distance),
            style = EcoTextStyles.Subtitle,
            color = EcoSubtitle,
        )
        EcoPrimaryButton(text = stringResource(R.string.recycler_delivery_view_detail), onClick = onViewDetail, height = 56.dp)
    }
}

// Tarjeta desplegada con volumen, distancia y la acción para tomar la entrega
@Composable
private fun ExpandedDeliveryCard(delivery: RecyclerDelivery, onCollapse: () -> Unit, onTake: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoFieldBackground)
            .border(1.5.dp, EcoPrimary, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(delivery.status.filterLabelRes()), style = EcoTextStyles.FieldLabel, color = EcoGreenSecondary)
                Text(delivery.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
            }
            val collapseLabel = stringResource(R.string.recycler_delivery_collapse)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EcoWhite)
                    .clickable(role = Role.Button, onClickLabel = collapseLabel, onClick = onCollapse),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_arrow_up), contentDescription = collapseLabel, tint = EcoTitle, modifier = Modifier.size(24.dp))
            }
        }
        Text(delivery.area, style = EcoTextStyles.Subtitle, color = EcoTitle)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DeliveryMetric(label = stringResource(R.string.recycler_delivery_volume), value = delivery.volume, modifier = Modifier.weight(1f))
            DeliveryMetric(label = stringResource(R.string.recycler_delivery_distance), value = delivery.distance, modifier = Modifier.weight(1f))
        }
        Text(
            stringResource(R.string.recycler_delivery_illustrative),
            style = EcoTextStyles.FieldLabel,
            color = EcoSubtitle,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoWhite),
        )
        EcoPrimaryButton(
            text = stringResource(R.string.recycler_delivery_take),
            onClick = onTake,
            height = 48.dp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DeliveryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(EcoWhite),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Text(value, style = EcoTextStyles.Numbers, color = EcoPrimary)
    }
}

// Fila compacta de otra entrega cercana dentro de la lista desplegada
@Composable
private fun DeliveryRow(delivery: RecyclerDelivery, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoFieldBackground)
            .border(1.dp, EcoFieldBackground, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(delivery.title, style = EcoTextStyles.LinkText, color = EcoTitle)
            Text(delivery.zone, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
        Text(delivery.distance, style = EcoTextStyles.LinkText, color = EcoPrimary)
    }
}

// Etiqueta corta del estado usada en los filtros y en las tarjetas del mapa
private fun PublicationStatus.filterLabelRes(): Int = when (this) {
    PublicationStatus.DISPONIBLE -> R.string.publication_status_available
    PublicationStatus.EN_CAMINO -> R.string.publication_status_on_the_way
    PublicationStatus.ENTREGADA -> R.string.publication_status_delivered
    PublicationStatus.CANCELADA -> R.string.publication_status_cancelled
}

// Entregas de ejemplo en Tuluá usadas en las previsualizaciones y en el modo diseño
val SampleRecyclerDeliveries = listOf(
    RecyclerDelivery(
        id = "ENT-01",
        title = "Cartón seco y plegado",
        area = "Centro, Tuluá",
        zone = "Zona Centro",
        volume = "0,3 m³",
        distance = "0,8 km",
        material = MaterialType.CARDBOARD,
        status = PublicationStatus.DISPONIBLE,
        mapX = 0.449f,
        mapY = 0.44f,
    ),
    RecyclerDelivery(
        id = "ENT-02",
        title = "Botellas PET limpias",
        area = "Barrio Boyacá, Tuluá",
        zone = "Zona Boyacá",
        volume = "0,2 m³",
        distance = "1,2 km",
        material = MaterialType.PET,
        status = PublicationStatus.DISPONIBLE,
        mapX = 0.837f,
        mapY = 0.402f,
    ),
    RecyclerDelivery(
        id = "ENT-03",
        title = "Envases de vidrio",
        area = "Parque Céspedes, Tuluá",
        zone = "Zona Céspedes",
        volume = "0,1 m³",
        distance = "1,6 km",
        material = MaterialType.GLASS,
        status = PublicationStatus.DISPONIBLE,
        mapX = 0.237f,
        mapY = 0.282f,
    ),
    RecyclerDelivery(
        id = "ENT-04",
        title = "Cajas de cartón",
        area = "La Campiña, Tuluá",
        zone = "Zona La Campiña",
        volume = "0,4 m³",
        distance = "2,1 km",
        material = MaterialType.CARDBOARD,
        status = PublicationStatus.EN_CAMINO,
        mapX = 0.6f,
        mapY = 0.68f,
    ),
)

@Preview(name = "Mapa de entregas", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerDeliveriesMapPreview() {
    EcoRutaTheme {
        RecyclerDeliveriesMapScreen(
            state = RecyclerDeliveriesUiState(deliveries = SampleRecyclerDeliveries),
            tabs = recyclerTabs(isCompanyLinked = false),
            actions = RecyclerDeliveriesActions(),
        )
    }
}

@Preview(name = "Mapa de entregas · Lista desplegada", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerDeliveriesListPreview() {
    EcoRutaTheme {
        RecyclerDeliveriesMapScreen(
            state = RecyclerDeliveriesUiState(deliveries = SampleRecyclerDeliveries, isListExpanded = true),
            tabs = recyclerTabs(isCompanyLinked = false),
            actions = RecyclerDeliveriesActions(),
        )
    }
}
