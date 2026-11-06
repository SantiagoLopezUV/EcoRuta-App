package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Vista para que el comercio aliado cree y publique un cupón de descuento
// Incluye los campos del cupón que también reutiliza la pantalla de edición

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Pantalla de creación con el formulario del cupón, el botón de publicar y su aclaración
@Composable
fun MerchantCreateCouponScreen(
    state: MerchantCouponFormUiState,
    actions: MerchantCouponFormActions,
    onTabClick: (MerchantTab) -> Unit,
) {
    if (state.isOffline) {
        OfflineScreen(onRetry = actions.onRetry)
        return
    }
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.CREATE_COUPON,
        onTabClick = onTabClick,
        contentSpacing = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.merchant_tab_create), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                if (state.storeName.isBlank()) {
                    stringResource(R.string.merchant_create_description_generic)
                } else {
                    stringResource(R.string.merchant_create_description, state.storeName)
                },
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        MerchantCouponFormFields(state = state, actions = actions)
        if (state.hasError) EcoErrorMessage(text = stringResource(R.string.merchant_create_error))
        EcoPrimaryButton(
            text = stringResource(R.string.merchant_create_publish),
            onClick = actions.onSubmit,
            isLoading = state.isSaving,
        )
        Text(
            if (state.pointsCost.isBlank()) {
                stringResource(R.string.merchant_create_publish_hint_generic)
            } else {
                stringResource(R.string.merchant_create_publish_hint, state.pointsCost)
            },
            style = EcoTextStyles.Subtitle,
            color = EcoSubtitle,
        )
    }
}

// Campos del cupón con sus mensajes de error, compartidos por la creación y la edición
@Composable
internal fun MerchantCouponFormFields(
    state: MerchantCouponFormUiState,
    actions: MerchantCouponFormActions,
    pointsHint: String? = null,
) {
    // Devuelve el texto de error solo cuando el campo fue marcado como inválido
    @Composable
    fun errorFor(field: MerchantCouponField, message: Int): String? =
        if (field in state.invalidFields) stringResource(message) else null

    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EcoTextField(
            value = state.title,
            onValueChange = actions.onTitleChange,
            label = stringResource(R.string.merchant_coupon_title_label),
            placeholder = stringResource(R.string.merchant_coupon_title_placeholder),
            errorMessage = errorFor(MerchantCouponField.TITLE, R.string.merchant_coupon_error_title),
        )
        EcoTextField(
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            label = stringResource(R.string.debris_description_label),
            placeholder = stringResource(R.string.merchant_coupon_description_placeholder),
            errorMessage = errorFor(MerchantCouponField.DESCRIPTION, R.string.merchant_coupon_error_description),
            singleLine = false,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EcoTextField(
                    value = state.pointsCost,
                    onValueChange = { actions.onPointsChange(it.filter(Char::isDigit)) },
                    label = stringResource(R.string.merchant_coupon_points_label),
                    errorMessage = errorFor(MerchantCouponField.POINTS, R.string.merchant_coupon_error_points),
                    keyboardOptions = numberKeyboard,
                    modifier = Modifier.weight(1f),
                )
                EcoTextField(
                    value = state.quantity,
                    onValueChange = { actions.onQuantityChange(it.filter(Char::isDigit)) },
                    label = stringResource(R.string.merchant_coupon_quantity_label),
                    errorMessage = errorFor(MerchantCouponField.QUANTITY, R.string.merchant_coupon_error_quantity),
                    keyboardOptions = numberKeyboard,
                    modifier = Modifier.weight(1f),
                )
            }
            // La edición explica debajo del costo quién paga los puntos del cupón
            if (pointsHint != null) Text(pointsHint, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
        // La vigencia no se escribe, se elige en el calendario que abre el campo al tocarlo
        val openPickerLabel = stringResource(R.string.merchant_coupon_date_picker_title)
        Box {
            EcoTextField(
                value = state.validUntilMillis?.let(::formatCouponDate).orEmpty(),
                onValueChange = {},
                label = stringResource(R.string.merchant_coupon_valid_until_label),
                placeholder = stringResource(R.string.merchant_coupon_valid_until_placeholder),
                errorMessage = errorFor(MerchantCouponField.VALID_UNTIL, R.string.merchant_coupon_error_valid_until),
                readOnly = true,
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(EcoDimens.InputIconSize),
                    )
                },
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(role = Role.Button, onClickLabel = openPickerLabel) { actions.onDatePickerToggle(true) },
            )
        }
    }
    if (state.isDatePickerOpen) {
        CouponDatePickerDialog(
            selectedMillis = state.validUntilMillis,
            onConfirm = { millis ->
                actions.onDateChange(millis)
                actions.onDatePickerToggle(false)
            },
            onDismiss = { actions.onDatePickerToggle(false) },
        )
    }
}

// Calendario para elegir el vencimiento del cupón desde el día de hoy en adelante
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponDatePickerDialog(selectedMillis: Long?, onConfirm: (Long?) -> Unit, onDismiss: () -> Unit) {
    val todayMillis = remember { startOfTodayUtcMillis() }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        selectableDates = remember(todayMillis) { FromDaySelectableDates(todayMillis) },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = EcoWhite,
        titleContentColor = EcoSubtitle,
        headlineContentColor = EcoTitle,
        weekdayContentColor = EcoSubtitle,
        subheadContentColor = EcoTitle,
        navigationContentColor = EcoTitle,
        dayContentColor = EcoTitle,
        selectedDayContainerColor = EcoGreenSecondary,
        selectedDayContentColor = EcoWhite,
        todayContentColor = EcoGreenSecondary,
        todayDateBorderColor = EcoGreenSecondary,
        selectedYearContainerColor = EcoGreenSecondary,
        selectedYearContentColor = EcoWhite,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onConfirm(pickerState.selectedDateMillis) },
                enabled = pickerState.selectedDateMillis != null,
                shape = RoundedCornerShape(EcoDimens.ControlCornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreenSecondary, contentColor = EcoWhite),
            ) { Text(stringResource(R.string.action_accept), style = EcoTextStyles.ButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), style = EcoTextStyles.LinkText, color = EcoGreenSecondary)
            }
        },
        colors = colors,
    ) {
        DatePicker(
            state = pickerState,
            colors = colors,
            showModeToggle = false,
            title = {
                Text(
                    stringResource(R.string.merchant_coupon_date_picker_title).uppercase(),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoSubtitle,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}

// Solo deja elegir días desde la fecha indicada en adelante
@OptIn(ExperimentalMaterial3Api::class)
private class FromDaySelectableDates(private val firstDayMillis: Long) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= firstDayMillis

    override fun isSelectableYear(year: Int): Boolean = year >= utcCalendar().apply { timeInMillis = firstDayMillis }.get(Calendar.YEAR)
}

// El calendario trabaja con días a medianoche UTC, así que hoy se convierte a ese mismo formato
private fun startOfTodayUtcMillis(): Long {
    val local = Calendar.getInstance()
    return utcCalendar().apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

// Muestra la fecha elegida como día, mes y año sin correrla por la zona horaria del teléfono
internal fun formatCouponDate(utcMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(utcMillis))

// Formulario ilustrativo usado solo en las vistas previas del comercio
internal val PreviewCouponForm = MerchantCouponFormUiState(
    storeName = "Café del Parque",
    title = "10% en tu compra",
    description = "Obtén un 10% de descuento en tu compra en Café del Parque. Válido una vez por cupón.",
    pointsCost = "200",
    quantity = "50",
    // Corresponde al 31 de diciembre de 2026 a medianoche UTC
    validUntilMillis = 1_798_675_200_000L,
)

@Preview(name = "Crear cupón", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCreateCouponPreview() {
    EcoRutaTheme { MerchantCreateCouponScreen(state = PreviewCouponForm, actions = MerchantCouponFormActions(), onTabClick = {}) }
}

@Preview(name = "Crear cupón · Vacío", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCreateCouponEmptyPreview() {
    EcoRutaTheme { MerchantCreateCouponScreen(state = MerchantCouponFormUiState(), actions = MerchantCouponFormActions(), onTabClick = {}) }
}

@Preview(name = "Crear cupón · Validación", widthDp = 393, heightDp = 1100)
@Composable
private fun MerchantCreateCouponValidationPreview() {
    EcoRutaTheme {
        MerchantCreateCouponScreen(
            state = MerchantCouponFormUiState(invalidFields = MerchantCouponField.entries.toSet()),
            actions = MerchantCouponFormActions(),
            onTabClick = {},
        )
    }
}

@Preview(name = "Crear cupón · Error", widthDp = 393, heightDp = 1000)
@Composable
private fun MerchantCreateCouponErrorPreview() {
    EcoRutaTheme { MerchantCreateCouponScreen(state = PreviewCouponForm.copy(hasError = true), actions = MerchantCouponFormActions(), onTabClick = {}) }
}

@Preview(name = "Crear cupón · Publicando", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCreateCouponSavingPreview() {
    EcoRutaTheme { MerchantCreateCouponScreen(state = PreviewCouponForm.copy(isSaving = true), actions = MerchantCouponFormActions(), onTabClick = {}) }
}

@Preview(name = "Crear cupón · Selector de fecha", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCreateCouponDatePickerPreview() {
    EcoRutaTheme {
        MerchantCreateCouponScreen(state = PreviewCouponForm.copy(isDatePickerOpen = true), actions = MerchantCouponFormActions(), onTabClick = {})
    }
}

@Preview(name = "Crear cupón · Sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCreateCouponOfflinePreview() {
    EcoRutaTheme { MerchantCreateCouponScreen(state = MerchantCouponFormUiState(isOffline = true), actions = MerchantCouponFormActions(), onTabClick = {}) }
}
