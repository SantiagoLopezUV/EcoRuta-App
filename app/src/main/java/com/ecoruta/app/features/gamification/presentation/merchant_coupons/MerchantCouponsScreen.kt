package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Vista con la lista de cupones del comercio aliado
// Permite editar cada cupón y desactivar los que siguen visibles para los ciudadanos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCoupon
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCouponCostRow
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCouponEditButton
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCouponTitleRow
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.PreviewMerchantCoupon

// Lista de cupones que elige entre sin conexión, error, vacío o las tarjetas de cada cupón
@Composable
fun MerchantCouponsScreen(state: MerchantCouponsUiState, actions: MerchantCouponsActions) {
    when {
        state.isOffline -> OfflineScreen(onRetry = actions.onRetry)
        state.hasError -> EcoSystemStateScreen(
            title = stringResource(R.string.merchant_tab_coupons),
            subtitle = state.storeName,
            illustrationRes = R.drawable.ill_state_offline,
            heading = stringResource(R.string.merchant_coupons_error_heading),
            body = stringResource(R.string.merchant_dashboard_error_body),
            actionText = stringResource(R.string.action_retry),
            onAction = actions.onRetry,
        )
        !state.isLoading && state.coupons.isEmpty() -> EcoSystemStateScreen(
            title = stringResource(R.string.merchant_tab_coupons),
            subtitle = state.storeName,
            illustrationRes = R.drawable.ic_nav_ticket,
            heading = stringResource(R.string.my_coupons_empty_title),
            body = stringResource(R.string.merchant_coupons_empty_body),
            actionText = stringResource(R.string.merchant_tab_create),
            onAction = actions.onCreateCoupon,
        )
        else -> MerchantCouponsContent(state, actions)
    }
}

@Composable
private fun MerchantCouponsContent(state: MerchantCouponsUiState, actions: MerchantCouponsActions) {
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.MY_COUPONS,
        onTabClick = actions.onTabClick,
        contentSpacing = 14.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.merchant_tab_coupons), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                if (state.storeName.isBlank()) {
                    stringResource(R.string.merchant_coupons_description_generic)
                } else {
                    stringResource(R.string.merchant_coupons_description, state.storeName)
                },
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    pluralStringResource(R.plurals.merchant_coupons_count, state.coupons.size, state.coupons.size),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoGreenSecondary,
                )
                state.coupons.forEach { coupon ->
                    CouponCard(
                        coupon = coupon,
                        onEdit = { actions.onEditCoupon(coupon) },
                        onDeactivate = { actions.onDeactivateCoupon(coupon) },
                    )
                }
            }
        }
    }
    if (state.showDeactivateConfirm) {
        MerchantDeactivateCouponDialog(onConfirm = actions.onConfirmDeactivate, onDismiss = actions.onDismissDeactivate)
    }
}

// Tarjeta de un cupón con su estado, costo, vigencia y las acciones según esté activo o no
@Composable
private fun CouponCard(coupon: MerchantCoupon, onEdit: () -> Unit, onDeactivate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MerchantCouponTitleRow(coupon)
        MerchantCouponCostRow(coupon)
        Text(stringResource(R.string.merchant_coupon_valid_until_value, coupon.validUntil), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MerchantCouponEditButton(onClick = onEdit, modifier = Modifier.height(44.dp))
            // Un cupón activo puede desactivarse aquí y uno inactivo solo informa que no se muestra
            if (coupon.isActive) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(EcoDimens.ControlCornerRadius))
                        .background(EcoWhite)
                        .clickable(role = Role.Button, onClick = onDeactivate),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.merchant_coupons_deactivate), style = EcoTextStyles.LinkText, color = EcoPrimary)
                }
            } else {
                Text(
                    stringResource(R.string.merchant_coupons_hidden),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoSubtitle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val PreviewCouponsState = MerchantCouponsUiState(
    storeName = "Café del Parque",
    coupons = listOf(
        PreviewMerchantCoupon.copy(validUntil = "31/12/2026"),
        MerchantCoupon(
            id = "CUP-02",
            title = "15% en bebidas frías",
            description = "Descuento en bebidas frías.",
            pointsCost = 300,
            availableQuantity = 20,
            validUntil = "30/11/2026",
            isActive = false,
        ),
    ),
)

@Preview(name = "Mis cupones del comercio", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponsPreview() {
    EcoRutaTheme { MerchantCouponsScreen(state = PreviewCouponsState, actions = MerchantCouponsActions()) }
}

@Preview(name = "Mis cupones del comercio · Confirmar desactivación", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponsDeactivateConfirmPreview() {
    EcoRutaTheme {
        MerchantCouponsScreen(
            state = PreviewCouponsState.copy(couponToDeactivate = PreviewCouponsState.coupons.first()),
            actions = MerchantCouponsActions(),
        )
    }
}

@Preview(name = "Mis cupones del comercio · Cargando", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponsLoadingPreview() {
    EcoRutaTheme { MerchantCouponsScreen(state = MerchantCouponsUiState(storeName = "Café del Parque", isLoading = true), actions = MerchantCouponsActions()) }
}

@Preview(name = "Mis cupones del comercio · Vacío", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponsEmptyPreview() {
    EcoRutaTheme { MerchantCouponsScreen(state = MerchantCouponsUiState(storeName = "Café del Parque"), actions = MerchantCouponsActions()) }
}

@Preview(name = "Mis cupones del comercio · Error", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponsErrorPreview() {
    EcoRutaTheme { MerchantCouponsScreen(state = MerchantCouponsUiState(storeName = "Café del Parque", hasError = true), actions = MerchantCouponsActions()) }
}

@Preview(name = "Mis cupones del comercio · Sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponsOfflinePreview() {
    EcoRutaTheme { MerchantCouponsScreen(state = MerchantCouponsUiState(isOffline = true), actions = MerchantCouponsActions()) }
}
