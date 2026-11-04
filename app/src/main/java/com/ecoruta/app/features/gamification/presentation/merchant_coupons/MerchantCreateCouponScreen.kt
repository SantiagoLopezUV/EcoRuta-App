package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Vista para que el comercio aliado cree y publique un cupón de descuento
// Incluye los campos del cupón que también reutiliza la pantalla de edición

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

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
        EcoTextField(
            value = state.validUntil,
            onValueChange = actions.onValidUntilChange,
            label = stringResource(R.string.merchant_coupon_valid_until_label),
            placeholder = stringResource(R.string.merchant_coupon_valid_until_placeholder),
            errorMessage = errorFor(MerchantCouponField.VALID_UNTIL, R.string.merchant_coupon_error_valid_until),
        )
    }
}

// Formulario ilustrativo usado solo en las vistas previas del comercio
internal val PreviewCouponForm = MerchantCouponFormUiState(
    storeName = "Café del Parque",
    title = "10% en tu compra",
    description = "Obtén un 10% de descuento en tu compra en Café del Parque. Válido una vez por cupón.",
    pointsCost = "200",
    quantity = "50",
    validUntil = "31/12/2026",
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

@Preview(name = "Crear cupón · Sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCreateCouponOfflinePreview() {
    EcoRutaTheme { MerchantCreateCouponScreen(state = MerchantCouponFormUiState(isOffline = true), actions = MerchantCouponFormActions(), onTabClick = {}) }
}
