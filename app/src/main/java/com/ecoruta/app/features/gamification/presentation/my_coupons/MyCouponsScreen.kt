package com.ecoruta.app.features.gamification.presentation.my_coupons

// Vista composable de la billetera de cupones canjeados del ciudadano
// Lista cupones activos listos para mostrar en caja y clasificados por categoría

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.gamification.presentation.benefits.AvailableMark
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsLoading
import com.ecoruta.app.features.gamification.presentation.benefits.CategoryFilterRow
import com.ecoruta.app.features.gamification.presentation.benefits.Coupon
import com.ecoruta.app.features.gamification.presentation.benefits.CouponCategory
import com.ecoruta.app.features.gamification.presentation.benefits.MyCouponUiState
import com.ecoruta.app.features.gamification.presentation.benefits.MyCouponsUiState
import com.ecoruta.app.features.gamification.presentation.benefits.RedeemedCoupon
import com.ecoruta.app.features.gamification.presentation.benefits.StoreBadge
import com.ecoruta.app.features.gamification.presentation.benefits.formatPoints
import com.ecoruta.app.features.gamification.presentation.benefits.formatShortCouponDate

data class MyCouponsActions(
    val onBack: () -> Unit = {},
    val onCategorySelected: (CouponCategory?) -> Unit = {},
    val onToggleExpanded: (String) -> Unit = {},
    val onShowQr: (RedeemedCoupon) -> Unit = {},
    val onGoToBenefits: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Pantalla de cupones canjeados del ciudadano con soporte para estado vacío y filtro por categoría
@Composable
fun MyCouponsScreen(state: MyCouponsUiState, actions: MyCouponsActions, selectedTab: CitizenTab = CitizenTab.COUPONS) {
    val isEmpty = !state.isLoading && state.error == null && state.coupons.isEmpty()
    val bottomActions: (@Composable ColumnScope.() -> Unit)? = if (isEmpty) {
        { EcoPrimaryButton(text = stringResource(R.string.my_coupons_go_to_benefits), onClick = actions.onGoToBenefits) }
    } else {
        null
    }
    EcoTabScreenLayout(
        selectedTab = selectedTab,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = bottomActions,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.my_coupons_title), style = EcoTextStyles.Title, color = EcoTitle)
            if (!isEmpty) Text(stringResource(R.string.my_coupons_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when {
            state.isLoading -> BenefitsLoading()
            state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(state.error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            isEmpty -> EmptyCoupons()
            else -> {
                AvailableCounter(count = state.coupons.size)
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.my_coupons_sample_hint))
                CategoryFilterRow(selected = state.category, onSelected = actions.onCategorySelected)
                if (state.visibleCoupons.isEmpty()) {
                    Text(stringResource(R.string.my_coupons_empty_category), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.visibleCoupons.forEach { redeemed ->
                        RedeemedCouponCard(
                            redeemed = redeemed,
                            expanded = state.expandedId == redeemed.id,
                            onToggle = { actions.onToggleExpanded(redeemed.id) },
                            onShowQr = { actions.onShowQr(redeemed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailableCounter(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoPrimary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_nav_ticket), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.my_coupons_available_label), style = EcoTextStyles.FieldLabel, color = EcoWhite.copy(alpha = 0.85f))
            Text(pluralStringResource(R.plurals.my_coupons_count, count, count), style = EcoTextStyles.Title, color = EcoWhite)
        }
    }
}

@Composable
private fun RedeemedCouponCard(redeemed: RedeemedCoupon, expanded: Boolean, onToggle: () -> Unit, onShowQr: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(1.dp, if (expanded) EcoPrimary else EcoBorder, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(if (expanded) R.string.my_coupons_collapse else R.string.my_coupons_expand),
                    onClick = onToggle,
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoreBadge()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(redeemed.coupon.businessName, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                Text(redeemed.coupon.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
                AvailableMark()
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = EcoSubtitle,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 90f else 0f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(color = EcoBorder)
                Text(stringResource(R.string.my_coupons_how_to_redeem), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EcoFieldBackground)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(redeemed.code, style = EcoTextStyles.Numbers.copy(fontFamily = FontFamily.Monospace), color = EcoTitle)
                }
                Text(stringResource(R.string.coupon_code_hint_short), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.my_coupons_redeemed_for, formatPoints(redeemed.pointsSpent)),
                        style = EcoTextStyles.FieldLabel,
                        color = EcoSubtitle,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.coupon_valid_until_short, formatShortCouponDate(redeemed.coupon.validUntilMillis)),
                        style = EcoTextStyles.FieldLabel,
                        color = EcoSubtitle,
                    )
                }
                EcoSecondaryButton(text = stringResource(R.string.my_coupons_show_qr), onClick = onShowQr)
            }
        }
    }
}

@Composable
private fun EmptyCoupons() {
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
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_nav_ticket), contentDescription = null, tint = EcoTitle, modifier = Modifier.size(28.dp))
        }
        Text(stringResource(R.string.my_coupons_empty_title), style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center)
        Text(stringResource(R.string.my_coupons_empty_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center)
    }
}

// Vista detallada del cupón canjeado con su código alfanumérico y código QR
@Composable
fun MyCouponDetailScreen(
    state: MyCouponUiState,
    selectedTab: CitizenTab,
    onBack: () -> Unit,
    onBackToMyCoupons: () -> Unit,
    onBackToBenefits: () -> Unit,
    onRetry: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    EcoTabScreenLayout(
        selectedTab = selectedTab,
        onBack = onBack,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.coupon_back_to_my_coupons), onClick = onBackToMyCoupons)
            EcoSecondaryButton(text = stringResource(R.string.coupon_back_to_benefits), onClick = onBackToBenefits)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.my_coupon_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.my_coupon_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when (state) {
            MyCouponUiState.Loading -> BenefitsLoading()
            MyCouponUiState.NotFound -> EcoErrorMessage(text = stringResource(R.string.coupon_not_found))
            MyCouponUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.my_coupons_load_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = onRetry)
            }
            is MyCouponUiState.Loaded -> {
                AvailableMark()
                CouponCodeCard(redeemed = state.coupon, isSample = state.isSample)
                Text(
                    stringResource(R.string.my_coupons_redeemed_on, formatShortCouponDate(state.coupon.redeemedAtMillis), formatPoints(state.coupon.pointsSpent)),
                    style = EcoTextStyles.Subtitle,
                    color = EcoSubtitle,
                )
            }
        }
    }
}

private val sampleRedeemedCoupon = RedeemedCoupon(
    id = "RED-01",
    coupon = Coupon(
        id = "CUP-01",
        businessName = "Café del Río",
        title = "20 % de descuento en bebidas calientes",
        subtitle = "Válido de lunes a viernes",
        category = CouponCategory.FOOD,
        pointsCost = 450,
        availableQuantity = 18,
        validUntilMillis = 1_795_996_800_000L,
        conditions = listOf("Un cupón por compra"),
    ),
    code = "ECO-CAFE-8492",
    redeemedAtMillis = 1_760_100_000_000L,
    pointsSpent = 450,
)

@Preview(name = "Mis cupones", widthDp = 393, heightDp = 1000)
@Composable
private fun MyCouponsPreview() {
    EcoRutaTheme {
        MyCouponsScreen(
            MyCouponsUiState(
                isLoading = false,
                coupons = listOf(sampleRedeemedCoupon),
                isSample = true,
                expandedId = sampleRedeemedCoupon.id,
            ),
            MyCouponsActions(),
        )
    }
}

@Preview(name = "Detalle de mi cupón QR", widthDp = 393, heightDp = 900)
@Composable
private fun MyCouponDetailPreview() {
    EcoRutaTheme {
        MyCouponDetailScreen(
            state = MyCouponUiState.Loaded(coupon = sampleRedeemedCoupon, isSample = false),
            selectedTab = CitizenTab.COUPONS,
            onBack = {},
            onBackToMyCoupons = {},
            onBackToBenefits = {},
            onRetry = {},
            onTabClick = {},
        )
    }
}
