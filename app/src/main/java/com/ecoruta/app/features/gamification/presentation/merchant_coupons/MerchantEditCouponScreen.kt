package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Vista para que el comercio aliado edite un cupón ya publicado
// Reutiliza los campos de creación y agrega la activación del cupón antes de guardar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCouponSwitch

// Pantalla de edición que elige entre sin conexión, cupón inexistente o el formulario con activación
@Composable
fun MerchantEditCouponScreen(
    state: MerchantCouponFormUiState,
    actions: MerchantCouponFormActions,
    onBackToMyCoupons: () -> Unit,
    onTabClick: (MerchantTab) -> Unit,
) {
    when {
        state.isOffline -> OfflineScreen(onRetry = actions.onRetry)
        state.isCouponMissing -> EcoSystemStateScreen(
            title = stringResource(R.string.merchant_tab_coupons),
            subtitle = state.storeName,
            illustrationRes = R.drawable.ic_nav_ticket,
            heading = stringResource(R.string.merchant_edit_missing_heading),
            body = stringResource(R.string.merchant_edit_missing_body),
            actionText = stringResource(R.string.coupon_back_to_my_coupons),
            onAction = onBackToMyCoupons,
        )
        else -> EditCouponContent(state, actions, onTabClick)
    }
}

@Composable
private fun EditCouponContent(
    state: MerchantCouponFormUiState,
    actions: MerchantCouponFormActions,
    onTabClick: (MerchantTab) -> Unit,
) {
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.MY_COUPONS,
        onTabClick = onTabClick,
        contentSpacing = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.merchant_tab_coupons), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.merchant_edit_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Text(stringResource(R.string.merchant_edit_selected), style = EcoTextStyles.FieldLabel, color = EcoGreenSecondary)
        MerchantCouponFormFields(
            state = state,
            actions = actions,
            pointsHint = stringResource(R.string.merchant_coupon_points_hint),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.merchant_edit_activation), style = EcoTextStyles.FieldLabel, color = EcoTitle)
                Text(
                    stringResource(if (state.isActive) R.string.merchant_coupon_active else R.string.merchant_coupon_inactive),
                    style = EcoTextStyles.LinkText,
                    color = if (state.isActive) EcoGreenSecondary else EcoSubtitle,
                )
            }
            MerchantCouponSwitch(checked = state.isActive, onCheckedChange = actions.onActiveChange)
        }
        if (state.hasError) EcoErrorMessage(text = stringResource(R.string.merchant_edit_error))
        EcoPrimaryButton(
            text = stringResource(R.string.merchant_edit_save),
            onClick = actions.onSubmit,
            isLoading = state.isSaving,
        )
    }
}

@Preview(name = "Editar cupón", widthDp = 393, heightDp = 1000)
@Composable
private fun MerchantEditCouponPreview() {
    EcoRutaTheme {
        MerchantEditCouponScreen(
            state = PreviewCouponForm.copy(description = "Obtén un 10% de descuento en tu compra. Válido una vez por cupón."),
            actions = MerchantCouponFormActions(),
            onBackToMyCoupons = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Editar cupón · Validación", widthDp = 393, heightDp = 1150)
@Composable
private fun MerchantEditCouponValidationPreview() {
    EcoRutaTheme {
        MerchantEditCouponScreen(
            state = PreviewCouponForm.copy(title = "", pointsCost = "", invalidFields = setOf(MerchantCouponField.TITLE, MerchantCouponField.POINTS)),
            actions = MerchantCouponFormActions(),
            onBackToMyCoupons = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Editar cupón · Error", widthDp = 393, heightDp = 1050)
@Composable
private fun MerchantEditCouponErrorPreview() {
    EcoRutaTheme {
        MerchantEditCouponScreen(
            state = PreviewCouponForm.copy(isActive = false, hasError = true),
            actions = MerchantCouponFormActions(),
            onBackToMyCoupons = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Editar cupón · Vacío", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantEditCouponMissingPreview() {
    EcoRutaTheme {
        MerchantEditCouponScreen(
            state = MerchantCouponFormUiState(isCouponMissing = true),
            actions = MerchantCouponFormActions(),
            onBackToMyCoupons = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Editar cupón · Sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantEditCouponOfflinePreview() {
    EcoRutaTheme {
        MerchantEditCouponScreen(
            state = MerchantCouponFormUiState(isOffline = true),
            actions = MerchantCouponFormActions(),
            onBackToMyCoupons = {},
            onTabClick = {},
        )
    }
}
