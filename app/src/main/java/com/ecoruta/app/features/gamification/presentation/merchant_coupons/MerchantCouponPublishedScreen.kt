package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Vista de confirmación que el comercio ve después de publicar un cupón
// Resume el cupón publicado y ofrece ir a sus cupones o volver al panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCoupon
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.PreviewMerchantCoupon

// Estado de la confirmación con el cupón recién publicado, nulo mientras no exista
data class MerchantCouponPublishedUiState(
    val storeName: String = "",
    val coupon: MerchantCoupon? = null,
)

// Acciones de la confirmación de publicación
data class MerchantCouponPublishedActions(
    val onViewMyCoupons: () -> Unit = {},
    val onBackToDashboard: () -> Unit = {},
    val onTabClick: (MerchantTab) -> Unit = {},
)

// Pantalla que confirma la publicación o avisa cuando no hay cupón para mostrar
@Composable
fun MerchantCouponPublishedScreen(state: MerchantCouponPublishedUiState, actions: MerchantCouponPublishedActions) {
    val coupon = state.coupon
    if (coupon == null) {
        EcoSystemStateScreen(
            title = stringResource(R.string.merchant_published_title),
            subtitle = state.storeName,
            illustrationRes = R.drawable.ic_nav_ticket,
            heading = stringResource(R.string.merchant_published_missing_heading),
            body = stringResource(R.string.merchant_published_missing_body),
            actionText = stringResource(R.string.coupon_view_my_coupons),
            onAction = actions.onViewMyCoupons,
        )
        return
    }
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.CREATE_COUPON,
        onTabClick = actions.onTabClick,
        contentSpacing = 24.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MerchantSuccessBadge()
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.merchant_published_title), style = EcoTextStyles.Title, color = EcoTitle)
                Text(stringResource(R.string.merchant_published_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
        PublishedCouponSummary(coupon = coupon, storeName = state.storeName)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EcoPrimaryButton(text = stringResource(R.string.coupon_view_my_coupons), onClick = actions.onViewMyCoupons)
            EcoSecondaryButton(text = stringResource(R.string.merchant_back_to_dashboard), onClick = actions.onBackToDashboard)
        }
    }
}

// Círculo verde con el visto de confirmación usado en publicación y validación
@Composable
internal fun MerchantSuccessBadge() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(EcoGreenSecondary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(40.dp))
    }
}

// Tarjeta con el título, la descripción, el costo, la disponibilidad y la vigencia del cupón
@Composable
private fun PublishedCouponSummary(coupon: MerchantCoupon, storeName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_nav_ticket), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(coupon.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
                if (storeName.isNotBlank()) {
                    Text(stringResource(R.string.merchant_store_in_city, storeName), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
            }
        }
        Text(coupon.description, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        HorizontalDivider(color = EcoWhite)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryValue(stringResource(R.string.merchant_coupon_points_label), coupon.pointsCost.toString(), Modifier.weight(1f))
            SummaryValue(stringResource(R.string.coupon_available), coupon.availableQuantity.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.merchant_coupon_valid_until_value, coupon.validUntil), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Text(
                stringResource(if (coupon.isActive) R.string.merchant_coupon_active else R.string.merchant_coupon_inactive),
                style = EcoTextStyles.FieldLabel,
                color = if (coupon.isActive) EcoGreenSecondary else EcoSubtitle,
            )
        }
    }
}

// Etiqueta con una cifra destacada debajo
@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Text(value, style = EcoTextStyles.Numbers, color = EcoPrimary)
    }
}

@Preview(name = "Cupón publicado", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponPublishedPreview() {
    EcoRutaTheme {
        MerchantCouponPublishedScreen(
            state = MerchantCouponPublishedUiState(storeName = "Café del Parque", coupon = PreviewMerchantCoupon),
            actions = MerchantCouponPublishedActions(),
        )
    }
}

@Preview(name = "Cupón publicado · Sin datos", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponPublishedEmptyPreview() {
    EcoRutaTheme { MerchantCouponPublishedScreen(state = MerchantCouponPublishedUiState(), actions = MerchantCouponPublishedActions()) }
}
