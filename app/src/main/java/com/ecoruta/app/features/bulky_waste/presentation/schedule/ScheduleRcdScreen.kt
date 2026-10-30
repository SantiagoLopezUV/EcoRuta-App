package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Vista composable de agendamiento de retiro de escombros en 3 pasos
// Paso 1: Tipo y dirección. Paso 2: Guía ambiental. Paso 3: Calendario semanal

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.EcoWizardLayout
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
import java.util.Date
import java.util.Locale

private val SpanishColombia: Locale = Locale.forLanguageTag("es-CO")
private const val DEBRIS_STEPS = 3

// Funciones de extensión para asociar etiquetas, iconos y formatos de fecha a los tipos de residuo

@StringRes
fun DebrisType.labelRes(): Int = when (this) {
    DebrisType.FURNITURE -> R.string.debris_type_furniture
    DebrisType.MATTRESSES -> R.string.debris_type_mattresses
    DebrisType.RENOVATION_DEBRIS -> R.string.debris_type_rubble
}

@StringRes
fun DebrisType.descriptionRes(): Int = when (this) {
    DebrisType.FURNITURE -> R.string.debris_type_furniture_description
    DebrisType.MATTRESSES -> R.string.debris_type_mattresses_description
    DebrisType.RENOVATION_DEBRIS -> R.string.debris_type_rubble_description
}

@DrawableRes
fun DebrisType.iconRes(): Int = when (this) {
    DebrisType.FURNITURE -> R.drawable.ic_sofa
    DebrisType.MATTRESSES -> R.drawable.ic_bed
    DebrisType.RENOVATION_DEBRIS -> R.drawable.ic_bricks
}

@StringRes
fun DebrisStatus.labelRes(): Int = when (this) {
    DebrisStatus.PENDIENTE -> R.string.debris_status_pending
    DebrisStatus.COTIZADO -> R.string.debris_status_quoted
    DebrisStatus.PROGRAMADO -> R.string.debris_status_scheduled
    DebrisStatus.RECOLECTADO -> R.string.debris_status_collected
    DebrisStatus.CANCELADO -> R.string.debris_status_cancelled
}

@Composable
fun DebrisStatusChip(status: DebrisStatus, modifier: Modifier = Modifier) {
    val (background, content) = when (status) {
        DebrisStatus.PENDIENTE -> EcoSubtitle to EcoWhite
        DebrisStatus.COTIZADO -> EcoAccent to EcoBlack
        DebrisStatus.PROGRAMADO -> EcoPrimary to EcoWhite
        DebrisStatus.RECOLECTADO -> EcoGreenSecondary to EcoWhite
        DebrisStatus.CANCELADO -> EcoTitle to EcoWhite
    }
    EcoStatusChip(stringResource(status.labelRes()), background, content, modifier)
}

fun formatLongDate(millis: Long): String =
    SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", SpanishColombia).format(Date(millis)).capitalizeFirst()

fun formatShortDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", SpanishColombia).format(Date(millis))

fun formatMonth(millis: Long): String =
    SimpleDateFormat("MMMM 'de' yyyy", SpanishColombia).format(Date(millis)).capitalizeFirst()

fun formatDayNumber(millis: Long): String = SimpleDateFormat("d", SpanishColombia).format(Date(millis))

fun formatWeekday(millis: Long): String =
    SimpleDateFormat("EEE", SpanishColombia).format(Date(millis)).trimEnd('.').capitalizeFirst()

fun formatDayAndMonth(millis: Long): String = SimpleDateFormat("d 'de' MMMM", SpanishColombia).format(Date(millis))

fun formatCop(value: Long): String = "$" + NumberFormat.getIntegerInstance(SpanishColombia).format(value) + " COP"

private fun String.capitalizeFirst(): String = replaceFirstChar { it.titlecase(SpanishColombia) }

// Primer paso del flujo para seleccionar el tipo de residuo, la descripción y la dirección de recogida

data class DebrisRequestActions(
    val onTypeSelected: (DebrisType) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun DebrisRequestScreen(state: DebrisRequestUiState, actions: DebrisRequestActions) {
    EcoWizardLayout(
        step = 1,
        totalSteps = DEBRIS_STEPS,
        stageLabel = stringResource(R.string.debris_stage_request),
        title = stringResource(R.string.debris_request_title),
        description = stringResource(R.string.debris_request_description),
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = actions.onContinue,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.debris_type_label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DebrisType.entries.forEach { type ->
                    DebrisTypeOption(type = type, selected = state.type == type, onClick = { actions.onTypeSelected(type) })
                }
            }
            state.typeError?.let { EcoErrorMessage(text = stringResource(it)) }
        }
        EcoTextField(
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            label = stringResource(R.string.debris_description_label),
            placeholder = stringResource(R.string.debris_description_placeholder),
            errorMessage = state.descriptionError?.let { stringResource(it) },
            supportingText = stringResource(
                R.string.report_description_counter,
                state.description.length,
                DebrisRequestUiState.MAX_DESCRIPTION,
            ),
            singleLine = false,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        EcoTextField(
            value = state.address,
            onValueChange = actions.onAddressChange,
            label = stringResource(R.string.debris_address_label),
            placeholder = stringResource(R.string.debris_address_placeholder),
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            },
            errorMessage = state.addressError?.let { stringResource(it) },
            supportingText = stringResource(R.string.report_default_area),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        EcoInfoHint(text = stringResource(R.string.debris_request_hint))
    }
}

@Composable
private fun DebrisTypeOption(type: DebrisType, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) EcoFieldBackground else EcoWhite)
            .border(if (selected) 2.dp else 1.dp, if (selected) EcoAccent else EcoBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) EcoAccent else EcoFieldBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(type.iconRes()),
                contentDescription = null,
                tint = if (selected) EcoBlack else EcoPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(type.labelRes()), style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(stringResource(type.descriptionRes()), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
        RadioMark(selected)
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) EcoAccent else EcoSubtitle, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(EcoAccent),
            )
        }
    }
}

// Segundo paso del flujo que muestra las recomendaciones de disposición según el residuo seleccionado

@Composable
fun DebrisGuideScreen(selectedType: DebrisType?, onContinue: () -> Unit, onBack: () -> Unit) {
    EcoWizardLayout(
        step = 2,
        totalSteps = DEBRIS_STEPS,
        stageLabel = stringResource(R.string.debris_stage_guide),
        title = stringResource(R.string.debris_guide_title),
        description = stringResource(R.string.debris_guide_description),
        onBack = onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.debris_choose_date),
                onClick = onContinue,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        val bulkySelected = selectedType == DebrisType.FURNITURE || selectedType == DebrisType.MATTRESSES
        GuideCard(
            iconRes = R.drawable.ic_sofa,
            title = stringResource(R.string.debris_guide_bulky_title),
            category = stringResource(R.string.debris_guide_bulky_category),
            body = stringResource(R.string.debris_guide_bulky_body),
            highlighted = bulkySelected,
        )
        GuideCard(
            iconRes = R.drawable.ic_bricks,
            title = stringResource(R.string.debris_type_rubble),
            category = stringResource(R.string.debris_guide_rcd_category),
            body = stringResource(R.string.debris_guide_rcd_body),
            highlighted = selectedType == DebrisType.RENOVATION_DEBRIS,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoPrimary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_shield_check), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.debris_guide_care_title), style = EcoTextStyles.ButtonText, color = EcoWhite)
            }
            Text(stringResource(R.string.debris_guide_care_body), style = EcoTextStyles.Subtitle, color = EcoWhite)
        }
    }
}

@Composable
private fun GuideCard(iconRes: Int, title: String, category: String, body: String, highlighted: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) EcoWhite else EcoFieldBackground)
            .border(if (highlighted) 2.dp else 0.dp, if (highlighted) EcoAccent else Color.Transparent, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(iconRes), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
            Text(title, style = EcoTextStyles.ButtonText, color = EcoTitle)
        }
        Text(
            text = if (highlighted) stringResource(R.string.debris_guide_your_selection, category) else category,
            style = EcoTextStyles.FieldLabel,
            color = if (highlighted) EcoPrimary else EcoSubtitle,
        )
        Text(body, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

// Tercer paso del flujo para elegir la fecha preferida dentro del calendario semanal disponible

data class DebrisScheduleActions(
    val onPreviousWeek: () -> Unit = {},
    val onNextWeek: () -> Unit = {},
    val onDaySelected: (Long) -> Unit = {},
    val onRetryDays: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun DebrisScheduleScreen(state: DebrisRequestUiState, actions: DebrisScheduleActions) {
    EcoWizardLayout(
        step = 3,
        totalSteps = DEBRIS_STEPS,
        stageLabel = stringResource(R.string.debris_stage_schedule),
        title = stringResource(R.string.debris_schedule_title),
        description = stringResource(R.string.debris_schedule_description),
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(if (state.isSubmitting) R.string.debris_sending else R.string.debris_send),
                onClick = actions.onSubmit,
                isLoading = state.isSubmitting,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        val weekEnd = DayMillis.addDays(state.weekStart, 6)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_calendar), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                Text(
                    formatMonth(DayMillis.addDays(state.weekStart, 3)),
                    style = EcoTextStyles.ButtonText,
                    color = EcoTitle,
                    modifier = Modifier.weight(1f),
                )
                WeekArrow(R.drawable.ic_chevron_left, R.string.debris_previous_week, state.canGoToPreviousWeek, actions.onPreviousWeek)
                WeekArrow(R.drawable.ic_chevron_right, R.string.debris_next_week, state.canGoToNextWeek, actions.onNextWeek)
            }
            Text(weekLabel(state.weekStart, weekEnd), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            when {
                state.isLoadingDays -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = EcoPrimary) }

                state.daysError != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EcoErrorMessage(text = stringResource(state.daysError))
                    EcoTextLink(text = stringResource(R.string.action_retry), onClick = actions.onRetryDays)
                }

                else -> Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(7) { offset ->
                        val day = DayMillis.addDays(state.weekStart, offset)
                        val inRange = day >= state.firstSelectableDay && day <= state.lastSelectableDay
                        DayCell(
                            day = day,
                            inRange = inRange,
                            available = inRange && day in state.availableDays,
                            selected = state.selectedDay == day,
                            onClick = { actions.onDaySelected(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Text(stringResource(R.string.debris_availability_legend), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            if (state.daysAreSample) EcoInfoHint(text = stringResource(R.string.debris_sample_dates_hint))
            state.dateError?.let { EcoErrorMessage(text = stringResource(it)) }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EcoDataLabel(stringResource(R.string.debris_preferred_date))
                Text(
                    state.selectedDay?.let(::formatLongDate) ?: stringResource(R.string.debris_no_date_selected),
                    style = EcoTextStyles.Subtitle,
                    color = EcoTitle,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EcoDataLabel(stringResource(R.string.debris_waste_and_location))
                state.type?.let { Text(stringResource(it.labelRes()), style = EcoTextStyles.Subtitle, color = EcoTitle) }
                Text(stringResource(R.string.debris_address_in_city, state.address.trim()), style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
        }
    }
}

@Composable
private fun WeekArrow(iconRes: Int, labelRes: Int, enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(labelRes)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(EcoFieldBackground)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = label, tint = EcoPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DayCell(
    day: Long,
    inRange: Boolean,
    available: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = if (selected) EcoWhite else if (available) EcoTitle else EcoSubtitle
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) EcoPrimary else EcoFieldBackground)
            .selectable(selected = selected, enabled = available, role = Role.RadioButton, onClick = onClick)
            .alpha(if (inRange) 1f else 0.4f)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(formatWeekday(day), style = EcoTextStyles.NavigationLabel, color = content)
        Text(formatDayNumber(day), style = EcoTextStyles.ButtonText.copy(fontSize = 18.sp), color = content)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (available) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(EcoAccent),
                )
            }
            Text(
                stringResource(
                    when {
                        !inRange -> R.string.debris_day_out_of_range
                        available -> R.string.debris_day_available
                        else -> R.string.debris_day_unavailable
                    },
                ),
                style = EcoTextStyles.NavigationLabel,
                color = content,
            )
        }
    }
}

@Composable
private fun weekLabel(start: Long, end: Long): String =
    if (formatMonth(start) == formatMonth(end)) {
        stringResource(R.string.debris_week_same_month, formatDayNumber(start), formatDayAndMonth(end))
    } else {
        stringResource(R.string.debris_week_two_months, formatDayAndMonth(start), formatDayAndMonth(end))
    }

// Pantalla de confirmación mostrada tras registrar exitosamente la solicitud de retiro

@Composable
fun DebrisSentScreen(
    request: DebrisRequest,
    sentWithoutBackend: Boolean,
    onViewTracking: () -> Unit,
    onGoHome: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoHome)
    }
    EcoTabScreenLayout(
        selectedTab = CitizenTab.HOME,
        onBack = onGoHome,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.debris_view_tracking),
                onClick = onViewTracking,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.debris_sent_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.debris_sent_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EcoFieldBackground)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(EcoAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(40.dp))
            }
            Text(stringResource(R.string.debris_sent_motto), style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center)
            DebrisStatusChip(DebrisStatus.PENDIENTE)
        }
        DebrisSummaryCard(request)
        EcoInfoHint(text = stringResource(R.string.debris_cost_pending_hint))
        if (sentWithoutBackend) EcoInfoHint(text = stringResource(R.string.debris_design_mode_sent_hint))
    }
}

@Composable
internal fun DebrisSummaryCard(request: DebrisRequest) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryItem(stringResource(R.string.debris_type_label), stringResource(request.type.labelRes()))
        SummaryItem(stringResource(R.string.debris_location_label), stringResource(R.string.debris_address_in_city, request.address))
        SummaryItem(stringResource(R.string.debris_preferred_not_confirmed), formatLongDate(request.preferredDateMillis))
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EcoDataLabel(label)
        Text(value, style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

private val previewBaseDay = DayMillis.startOfDay(1_744_800_000_000L)
private val previewWeekStart = DayMillis.mondayOf(previewBaseDay)

@Preview(name = "Paso 1 Solicitud", widthDp = 393, heightDp = 900)
@Composable
private fun DebrisRequestPreview() {
    EcoRutaTheme {
        DebrisRequestScreen(
            state = DebrisRequestUiState(
                type = DebrisType.FURNITURE,
                description = "Sofá de tres puestos y mesa de centro de madera.",
                address = "Cra. 22E #6-40 · Torobajo",
            ),
            actions = DebrisRequestActions(),
        )
    }
}

@Preview(name = "Paso 2 Guía", widthDp = 393, heightDp = 900)
@Composable
private fun DebrisGuidePreview() {
    EcoRutaTheme {
        DebrisGuideScreen(
            selectedType = DebrisType.FURNITURE,
            onContinue = {},
            onBack = {},
        )
    }
}

@Preview(name = "Paso 3 Agendar", widthDp = 393, heightDp = 900)
@Composable
private fun DebrisSchedulePreview() {
    val available = setOf(
        DayMillis.addDays(previewWeekStart, 1),
        DayMillis.addDays(previewWeekStart, 3),
        DayMillis.addDays(previewWeekStart, 5),
    )
    EcoRutaTheme {
        DebrisScheduleScreen(
            state = DebrisRequestUiState(
                type = DebrisType.FURNITURE,
                address = "Cra. 22E #6-40 · Torobajo",
                firstSelectableDay = previewWeekStart,
                lastSelectableDay = DayMillis.addDays(previewWeekStart, 21),
                weekStart = previewWeekStart,
                availableDays = available,
                selectedDay = DayMillis.addDays(previewWeekStart, 3),
            ),
            actions = DebrisScheduleActions(),
        )
    }
}

@Preview(name = "Solicitud Enviada", widthDp = 393, heightDp = 852)
@Composable
private fun DebrisSentPreview() {
    EcoRutaTheme {
        DebrisSentScreen(
            request = DebrisRequest(
                id = "rcd-1",
                type = DebrisType.FURNITURE,
                description = "Sofá de tres puestos",
                address = "Cra. 22E #6-40 · Torobajo",
                preferredDateMillis = DayMillis.addDays(previewWeekStart, 3),
                status = DebrisStatus.PENDIENTE,
                createdAtMillis = previewBaseDay,
            ),
            sentWithoutBackend = false,
            onViewTracking = {},
            onGoHome = {},
            onTabClick = {},
        )
    }
}
