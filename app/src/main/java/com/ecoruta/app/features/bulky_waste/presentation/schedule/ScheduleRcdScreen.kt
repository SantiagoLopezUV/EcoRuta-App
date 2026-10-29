package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Componentes compartidos del retiro de escombros y primer paso de la solicitud
// Paso 1: Tipo y dirección. La guía, el calendario y el envío están en archivos propios

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTextField
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

internal const val DEBRIS_STEPS = 3

// Funciones de extensión para asociar etiquetas, iconos y formatos de fecha a los tipos de residuo

@StringRes
fun DebrisType.labelRes(): Int = when (this) {
    DebrisType.MUEBLES -> R.string.debris_type_furniture
    DebrisType.COLCHONES -> R.string.debris_type_mattresses
    DebrisType.ESCOMBROS_REMODELACION -> R.string.debris_type_rubble
}

@StringRes
fun DebrisType.descriptionRes(): Int = when (this) {
    DebrisType.MUEBLES -> R.string.debris_type_furniture_description
    DebrisType.COLCHONES -> R.string.debris_type_mattresses_description
    DebrisType.ESCOMBROS_REMODELACION -> R.string.debris_type_rubble_description
}

@DrawableRes
fun DebrisType.iconRes(): Int = when (this) {
    DebrisType.MUEBLES -> R.drawable.ic_sofa
    DebrisType.COLCHONES -> R.drawable.ic_bed
    DebrisType.ESCOMBROS_REMODELACION -> R.drawable.ic_bricks
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

internal val previewBaseDay = DayMillis.startOfDay(1_744_800_000_000L)

internal val previewWeekStart = DayMillis.mondayOf(previewBaseDay)

@Preview(name = "Paso 1 Solicitud", widthDp = 393, heightDp = 900)
@Composable
private fun DebrisRequestPreview() {
    EcoRutaTheme {
        DebrisRequestScreen(
            state = DebrisRequestUiState(
                type = DebrisType.MUEBLES,
                description = "Sofá de tres puestos y mesa de centro de madera.",
                address = "Cra. 22E #6-40 · Torobajo",
            ),
            actions = DebrisRequestActions(),
        )
    }
}
