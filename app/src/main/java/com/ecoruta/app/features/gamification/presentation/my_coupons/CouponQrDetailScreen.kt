package com.ecoruta.app.features.gamification.presentation.my_coupons

// Vista composable de detalle de cupón redimido
// Exhibe código único alfanumérico (ECO-XXXXX) y código QR para lectura en caja

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSampleQrCode
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.gamification.presentation.benefits.BalanceCard
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsLoading
import com.ecoruta.app.features.gamification.presentation.benefits.Coupon
import com.ecoruta.app.features.gamification.presentation.benefits.CouponCategory
import com.ecoruta.app.features.gamification.presentation.benefits.CouponRedemptionUiState
import com.ecoruta.app.features.gamification.presentation.benefits.RedeemedCoupon
import com.ecoruta.app.features.gamification.presentation.benefits.RedemptionReceipt
import com.ecoruta.app.features.gamification.presentation.benefits.RedemptionStep
import com.ecoruta.app.features.gamification.presentation.benefits.StoreBadge
import com.ecoruta.app.features.gamification.presentation.benefits.SummaryBox
import com.ecoruta.app.features.gamification.presentation.benefits.SummaryRow
import com.ecoruta.app.features.gamification.presentation.benefits.formatCouponDate
import com.ecoruta.app.features.gamification.presentation.benefits.formatPoints
import com.ecoruta.app.features.gamification.presentation.benefits.formatSignedPoints

data class CouponRedemptionActions(
    val onBack: () -> Unit = {},
    val onRedeem: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onBackToDetail: () -> Unit = {},
    val onMyCoupons: () -> Unit = {},
    val onBackToBenefits: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Flujo de canje de cupón con pasos de detalle, confirmación, cupón redimido o saldo insuficiente
@Composable
fun CouponRedemptionScreen(state: CouponRedemptionUiState, actions: CouponRedemptionActions) {
    val coupon = state.coupon
    when {
        state.isLoading -> RedemptionLayout(actions) { BenefitsLoading() }
        state.error != null || coupon == null -> RedemptionLayout(actions) {
            EcoErrorMessage(text = stringResource(state.error ?: R.string.coupon_not_found))
            EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
        }
        else -> when (state.step) {
            RedemptionStep.DETAIL -> CouponDetailStep(coupon, state, actions)
            RedemptionStep.CONFIRM -> ConfirmStep(coupon, state, actions)
            RedemptionStep.INSUFFICIENT -> InsufficientStep(coupon, state, actions)
            RedemptionStep.REDEEMED -> RedeemedStep(state, actions)
        }
    }
}

@Composable
private fun RedemptionLayout(
    actions: CouponRedemptionActions,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.BENEFITS,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = bottomActions,
        content = content,
    )
}

@Composable
private fun StepHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = EcoTextStyles.Title, color = EcoTitle)
        Text(description, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

// Tarjeta de encabezado con comercio, título y descripción del beneficio
@Composable
private fun CouponHeaderCard(coupon: Coupon) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoreBadge(size = 52)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(coupon.businessName, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            Text(coupon.title, style = EcoTextStyles.Title, color = EcoTitle)
            Text(coupon.subtitle, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
    }
}

@Composable
private fun CouponDetailStep(coupon: Coupon, state: CouponRedemptionUiState, actions: CouponRedemptionActions) {
    RedemptionLayout(
        actions,
        bottomActions = { EcoPrimaryButton(text = stringResource(R.string.coupon_redeem), onClick = actions.onRedeem) },
    ) {
        StepHeader(stringResource(R.string.coupon_detail_title), stringResource(R.string.coupon_detail_description))
        if (state.isSample) EcoInfoHint(text = stringResource(R.string.benefits_sample_hint))
        CouponHeaderCard(coupon)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile(stringResource(R.string.coupon_cost), stringResource(R.string.benefits_points_short, formatPoints(coupon.pointsCost)), Modifier.weight(1f))
            InfoTile(stringResource(R.string.coupon_available), formatPoints(coupon.availableQuantity), Modifier.weight(1f))
        }
        Text(stringResource(R.string.coupon_valid_until, formatCouponDate(coupon.validUntilMillis)), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        SummaryBox(title = stringResource(R.string.coupon_conditions)) {
            coupon.conditions.forEach { condition ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = EcoTextStyles.Subtitle, color = EcoPrimary)
                    Text(condition, style = EcoTextStyles.Subtitle, color = EcoTitle)
                }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(EcoFieldBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Text(value, style = EcoTextStyles.ButtonText, color = EcoTitle)
    }
}

@Composable
private fun ConfirmStep(coupon: Coupon, state: CouponRedemptionUiState, actions: CouponRedemptionActions) {
    val balance = state.balance ?: 0
    RedemptionLayout(
        actions,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.coupon_confirm),
                onClick = actions.onConfirm,
                isLoading = state.isRedeeming,
                trailingIconRes = R.drawable.ic_checkbox_check,
            )
            EcoSecondaryButton(text = stringResource(R.string.coupon_back_to_detail), onClick = actions.onBackToDetail, enabled = !state.isRedeeming)
        },
    ) {
        StepHeader(stringResource(R.string.coupon_confirm_title), stringResource(R.string.coupon_confirm_description))
        CouponHeaderCard(coupon)
        SummaryBox(title = stringResource(R.string.coupon_your_points)) {
            SummaryRow(stringResource(R.string.coupon_current_balance), stringResource(R.string.benefits_points_short, formatPoints(balance)))
            SummaryRow(stringResource(R.string.coupon_points_to_deduct), stringResource(R.string.benefits_points_short, formatSignedPoints(-coupon.pointsCost)))
            HorizontalDivider(color = EcoBorder)
            SummaryRow(
                stringResource(R.string.coupon_remaining_balance),
                stringResource(R.string.benefits_points_short, formatPoints(state.balanceAfterPreview ?: 0)),
                emphasized = true,
                valueColor = EcoPrimary,
            )
        }
        EcoInfoHint(text = stringResource(R.string.coupon_confirm_hint, formatPoints(coupon.pointsCost)))
    }
}

@Composable
private fun InsufficientStep(coupon: Coupon, state: CouponRedemptionUiState, actions: CouponRedemptionActions) {
    RedemptionLayout(
        actions,
        bottomActions = { EcoPrimaryButton(text = stringResource(R.string.coupon_back_to_benefits), onClick = actions.onBackToBenefits) },
    ) {
        StatusIcon(iconRes = R.drawable.ic_alert_circle, background = EcoFieldBackground)
        StepHeader(stringResource(R.string.coupon_insufficient_title), stringResource(R.string.coupon_insufficient_description))
        CouponHeaderCard(coupon)
        SummaryBox {
            SummaryRow(stringResource(R.string.coupon_your_balance), stringResource(R.string.benefits_points_short, formatPoints(state.balance ?: 0)))
            SummaryRow(stringResource(R.string.coupon_cost), stringResource(R.string.benefits_points_short, formatPoints(coupon.pointsCost)))
            HorizontalDivider(color = EcoBorder)
            SummaryRow(
                stringResource(R.string.coupon_missing),
                stringResource(R.string.benefits_points_short, formatPoints(state.missingPoints)),
                emphasized = true,
                valueColor = EcoError,
            )
        }
        Text(stringResource(R.string.coupon_no_points_deducted), style = EcoTextStyles.ButtonText, color = EcoTitle)
        EcoInfoHint(text = stringResource(R.string.coupon_keep_recycling, formatPoints(coupon.pointsCost)))
    }
}

@Composable
private fun RedeemedStep(state: CouponRedemptionUiState, actions: CouponRedemptionActions) {
    val receipt = state.receipt ?: return
    RedemptionLayout(
        actions,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.coupon_view_my_coupons), onClick = actions.onMyCoupons)
            EcoSecondaryButton(text = stringResource(R.string.coupon_back_to_benefits), onClick = actions.onBackToBenefits)
        },
    ) {
        StatusIcon(iconRes = R.drawable.ic_check_large, background = EcoAccent)
        StepHeader(stringResource(R.string.coupon_redeemed_title), stringResource(R.string.coupon_redeemed_description))
        if (state.redeemedWithoutBackend) EcoInfoHint(text = stringResource(R.string.coupon_design_mode_not_redeemed))
        CouponCodeCard(redeemed = receipt.redeemedCoupon, isSample = state.redeemedWithoutBackend || state.isSample)
        BalanceCard(label = stringResource(R.string.coupon_balance_available), balance = receipt.balanceAfter)
    }
}

@Composable
private fun StatusIcon(iconRes: Int, background: Color) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = if (background == EcoAccent) EcoWhite else EcoTitle, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun CouponCodeCard(redeemed: RedeemedCoupon, isSample: Boolean) {
    val qrDescription = stringResource(R.string.coupon_qr_sample_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EcoFieldBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(redeemed.coupon.businessName, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Text(redeemed.coupon.title, style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center)
        if (isSample) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoWhite)
                    .padding(14.dp)
                    .semantics { contentDescription = qrDescription },
                contentAlignment = Alignment.Center,
            ) { EcoSampleQrCode(seed = redeemed.code) }
            EcoStatusChip(stringResource(R.string.recycling_qr_sample_label), EcoTitle, EcoWhite)
        }
        Text(stringResource(R.string.coupon_code_label), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Text(
            redeemed.code,
            style = EcoTextStyles.Numbers.copy(fontFamily = FontFamily.Monospace),
            color = EcoTitle,
        )
        Text(stringResource(R.string.coupon_code_hint), style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.coupon_valid_until, formatCouponDate(redeemed.coupon.validUntilMillis)),
            style = EcoTextStyles.FieldLabel,
            color = EcoPrimary,
        )
    }
}

private val samplePreviewCoupon = Coupon(
    id = "CUP-01",
    businessName = "Café del Río",
    title = "20 % de descuento en bebidas calientes",
    subtitle = "Válido de lunes a viernes",
    category = CouponCategory.FOOD,
    pointsCost = 450,
    availableQuantity = 18,
    validUntilMillis = 1_795_996_800_000L,
    conditions = listOf("Un cupón por compra"),
)

@Preview(name = "Detalle del cupón", widthDp = 393, heightDp = 900)
@Composable
private fun CouponDetailPreview() {
    EcoRutaTheme {
        CouponRedemptionScreen(
            CouponRedemptionUiState(
                isLoading = false,
                coupon = samplePreviewCoupon,
                balance = 1_250,
                isSample = true,
            ),
            CouponRedemptionActions(),
        )
    }
}

@Preview(name = "Confirmar canje", widthDp = 393, heightDp = 900)
@Composable
private fun CouponConfirmPreview() {
    EcoRutaTheme {
        CouponRedemptionScreen(
            CouponRedemptionUiState(
                isLoading = false,
                coupon = samplePreviewCoupon,
                balance = 1_250,
                step = RedemptionStep.CONFIRM,
                isSample = false,
            ),
            CouponRedemptionActions(),
        )
    }
}

@Preview(name = "Puntos insuficientes", widthDp = 393, heightDp = 900)
@Composable
private fun CouponInsufficientPreview() {
    EcoRutaTheme {
        CouponRedemptionScreen(
            CouponRedemptionUiState(
                isLoading = false,
                coupon = samplePreviewCoupon,
                balance = 200,
                step = RedemptionStep.INSUFFICIENT,
                isSample = false,
            ),
            CouponRedemptionActions(),
        )
    }
}

@Preview(name = "Cupón redimido", widthDp = 393, heightDp = 950)
@Composable
private fun CouponRedeemedPreview() {
    EcoRutaTheme {
        CouponRedemptionScreen(
            CouponRedemptionUiState(
                isLoading = false,
                coupon = samplePreviewCoupon,
                balance = 800,
                step = RedemptionStep.REDEEMED,
                receipt = RedemptionReceipt(
                    redeemedCoupon = RedeemedCoupon(
                        id = "RED-01",
                        coupon = samplePreviewCoupon,
                        code = "ECO-CAFE-8492",
                        redeemedAtMillis = 1_760_100_000_000L,
                        pointsSpent = 450,
                    ),
                    balanceAfter = 800,
                ),
                isSample = true,
            ),
            CouponRedemptionActions(),
        )
    }
}
