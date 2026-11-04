package com.ecoruta.app.features.gamification.presentation.cashier_validation

// Vista de confirmación después de validar un cupón en caja
// Muestra el cupón canjeado, el desglose del descuento y el registro del canje

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantSuccessBadge
import java.text.NumberFormat
import java.util.Locale

// Pantalla que confirma el canje o avisa cuando no hay un cupón validado para mostrar
@Composable
fun MerchantCouponValidatedScreen(
    coupon: ValidatedCoupon?,
    onValidateAnother: () -> Unit,
    onTabClick: (MerchantTab) -> Unit,
) {
    if (coupon == null) {
        EcoSystemStateScreen(
            title = stringResource(R.string.merchant_validated_title),
            subtitle = stringResource(R.string.merchant_cashier_description),
            illustrationRes = R.drawable.ic_nav_ticket,
            heading = stringResource(R.string.merchant_validated_missing_heading),
            body = stringResource(R.string.merchant_validated_missing_body),
            actionText = stringResource(R.string.merchant_cashier_title),
            onAction = onValidateAnother,
        )
        return
    }
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.VALIDATE,
        onTabClick = onTabClick,
        contentSpacing = 24.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MerchantSuccessBadge()
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.merchant_validated_title), style = EcoTextStyles.Title, color = EcoTitle)
                Text(stringResource(R.string.merchant_validated_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
        ValidatedSummary(coupon)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(EcoDimens.InputIconSize))
                Text(stringResource(R.string.merchant_validated_registered), style = EcoTextStyles.LinkText, color = EcoPrimary)
            }
            Text(coupon.redeemedAt, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Text(stringResource(R.string.merchant_validated_used), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
    }
}

// Tarjeta con el cupón, su código y el desglose del valor cuando el estado trae los montos
@Composable
private fun ValidatedSummary(coupon: ValidatedCoupon) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(EcoDimens.ControlCornerRadius))
                    .background(EcoWhite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_nav_ticket), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(EcoDimens.InputIconSize))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(coupon.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
                Text(stringResource(R.string.merchant_store_in_city, coupon.storeName), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(coupon.code, style = EcoTextStyles.LinkText, color = EcoPrimary)
            Text(stringResource(R.string.merchant_coupon_points, coupon.pointsCost), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        // El desglose solo aparece cuando el estado incluye el valor de la compra y el descuento
        val purchase = coupon.purchaseAmount
        val discount = coupon.discountAmount
        val total = coupon.totalToPay
        val percent = coupon.discountPercent
        if (purchase != null && discount != null && total != null && percent != null) {
            HorizontalDivider(color = EcoWhite)
            AmountRow(stringResource(R.string.merchant_validated_purchase), stringResource(R.string.merchant_money_value, formatPesos(purchase)))
            AmountRow(stringResource(R.string.merchant_validated_discount, percent), stringResource(R.string.merchant_money_discount, formatPesos(discount)))
            HorizontalDivider(color = EcoWhite)
            AmountRow(
                label = stringResource(R.string.merchant_validated_total),
                value = stringResource(R.string.merchant_money_value, formatPesos(total)),
                labelStyle = EcoTextStyles.ButtonText,
                labelColor = EcoTitle,
                valueStyle = EcoTextStyles.Numbers,
                valueColor = EcoPrimary,
            )
        }
    }
}

// Fila con la etiqueta del monto a la izquierda y su valor a la derecha
@Composable
private fun AmountRow(
    label: String,
    value: String,
    labelStyle: TextStyle = EcoTextStyles.Subtitle,
    labelColor: Color = EcoSubtitle,
    valueStyle: TextStyle = EcoTextStyles.LinkText,
    valueColor: Color = EcoTitle,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = labelStyle, color = labelColor)
        Text(value, style = valueStyle, color = valueColor)
    }
}

// Formatea un valor en pesos colombianos con separador de miles y sin decimales
private fun formatPesos(amount: Long): String = NumberFormat.getIntegerInstance(Locale("es", "CO")).format(amount)

private val PreviewValidatedCoupon = ValidatedCoupon(
    code = "ECO-7K2M90AB",
    title = "10% en tu compra",
    storeName = "Café del Parque",
    pointsCost = 200,
    redeemedAt = "13 de octubre de 2026 · 10:42 a. m.",
    discountPercent = 10,
    purchaseAmount = 40_000,
    discountAmount = 4_000,
    totalToPay = 36_000,
)

@Preview(name = "Cupón validado", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantCouponValidatedPreview() {
    EcoRutaTheme { MerchantCouponValidatedScreen(coupon = PreviewValidatedCoupon, onValidateAnother = {}, onTabClick = {}) }
}

@Preview(name = "Cupón validado · Sin montos", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponValidatedWithoutAmountsPreview() {
    EcoRutaTheme {
        MerchantCouponValidatedScreen(
            coupon = PreviewValidatedCoupon.copy(discountPercent = null, purchaseAmount = null, discountAmount = null, totalToPay = null),
            onValidateAnother = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Cupón validado · Vacío", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCouponValidatedEmptyPreview() {
    EcoRutaTheme { MerchantCouponValidatedScreen(coupon = null, onValidateAnother = {}, onTabClick = {}) }
}
