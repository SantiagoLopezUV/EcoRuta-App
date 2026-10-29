package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Paso 3 del retiro de escombros: selección de fecha en el calendario semanal

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.EcoWizardLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

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
                type = DebrisType.MUEBLES,
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
