package com.ecoruta.app.features.gamification.presentation.merchant_dashboard

// Vista del panel principal del comercio aliado
// Resume los cupones activos, permite activarlos o editarlos y muestra los canjes recientes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantDeactivateCouponDialog

// Acciones disponibles en el panel del comercio
data class MerchantDashboardActions(
    val onEditCoupon: (MerchantCoupon) -> Unit = {},
    // Al apagar el interruptor se pide la confirmación y la desactivación solo se aplica al confirmar
    val onDeactivateCoupon: (MerchantCoupon) -> Unit = {},
    val onConfirmDeactivate: () -> Unit = {},
    val onDismissDeactivate: () -> Unit = {},
    val onCreateCoupon: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (MerchantTab) -> Unit = {},
)

// Panel del comercio que elige entre sin conexión, error, vacío o el contenido con resumen y listas
@Composable
fun MerchantDashboardScreen(state: MerchantDashboardUiState, actions: MerchantDashboardActions) {
    when {
        state.isOffline -> OfflineScreen(onRetry = actions.onRetry)
        state.hasError -> EcoSystemStateScreen(
            title = stringResource(R.string.merchant_dashboard_title),
            subtitle = state.storeName,
            illustrationRes = R.drawable.ill_state_offline,
            heading = stringResource(R.string.merchant_dashboard_error_heading),
            body = stringResource(R.string.merchant_dashboard_error_body),
            actionText = stringResource(R.string.action_retry),
            onAction = actions.onRetry,
        )
        !state.isLoading && state.isEmpty -> MerchantDashboardEmpty(state, actions)
        else -> MerchantDashboardContent(state, actions)
    }
}

// Panel sin cupones ni canjes que conserva la barra del comercio para poder cambiar de pestaña
@Composable
private fun MerchantDashboardEmpty(state: MerchantDashboardUiState, actions: MerchantDashboardActions) {
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.DASHBOARD,
        onTabClick = actions.onTabClick,
        showBrandHeader = false,
        contentSpacing = 18.dp,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.merchant_tab_create), onClick = actions.onCreateCoupon)
        },
    ) {
        MerchantHeaderCard(storeName = state.storeName)
        MerchantEmptyStateCard(
            illustrationRes = R.drawable.ic_nav_ticket,
            heading = stringResource(R.string.merchant_dashboard_empty_heading),
            body = stringResource(R.string.merchant_dashboard_empty_body),
        )
    }
}

// Recuadro con ilustración, título y explicación para las pestañas del comercio que aún no tienen datos
@Composable
internal fun MerchantEmptyStateCard(@DrawableRes illustrationRes: Int, heading: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Image(painterResource(illustrationRes), contentDescription = null, modifier = Modifier.size(104.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(heading, style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(body, style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MerchantDashboardContent(state: MerchantDashboardUiState, actions: MerchantDashboardActions) {
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.DASHBOARD,
        onTabClick = actions.onTabClick,
        showBrandHeader = false,
        contentSpacing = 18.dp,
    ) {
        MerchantHeaderCard(storeName = state.storeName)
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
        } else {
            DashboardSections(state, actions)
        }
    }
    if (state.showDeactivateConfirm) {
        MerchantDeactivateCouponDialog(onConfirm = actions.onConfirmDeactivate, onDismiss = actions.onDismissDeactivate)
    }
}

// Resumen en cifras, cupones activos y canjes recientes del comercio
@Composable
private fun DashboardSections(state: MerchantDashboardUiState, actions: MerchantDashboardActions) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                value = state.activeCoupons.size,
                label = pluralStringResource(R.plurals.merchant_dashboard_active_count, state.activeCoupons.size),
                modifier = Modifier.weight(1f),
            )
            SummaryTile(value = state.availableTotal, label = stringResource(R.string.coupon_available), modifier = Modifier.weight(1f))
            SummaryTile(
                value = state.redemptionsToday,
                label = stringResource(R.string.merchant_dashboard_redemptions_today),
                modifier = Modifier.weight(1f),
            )
        }
        if (state.activeCoupons.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.merchant_dashboard_active_coupons), style = EcoTextStyles.ButtonText, color = EcoTitle)
                state.activeCoupons.forEach { coupon ->
                    ActiveCouponCard(
                        coupon = coupon,
                        // El panel solo lista cupones activos, así que el interruptor solo sirve para desactivar
                        onToggle = { checked -> if (!checked) actions.onDeactivateCoupon(coupon) },
                        onEdit = { actions.onEditCoupon(coupon) },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.merchant_dashboard_recent_redemptions), style = EcoTextStyles.ButtonText, color = EcoTitle)
                Text(state.redemptionsDateLabel, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            }
            if (state.recentRedemptions.isEmpty()) {
                Text(stringResource(R.string.merchant_dashboard_no_redemptions), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
            state.recentRedemptions.forEach { RedemptionRow(it) }
        }
    }
}

// Cabecera verde con el icono del comercio, la marca, el rol y el nombre del establecimiento
@Composable
private fun MerchantHeaderCard(storeName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
            .background(EcoPrimary)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EcoWhite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_store),
                    contentDescription = stringResource(R.string.merchant_store_icon_description),
                    tint = EcoPrimary,
                    modifier = Modifier.size(EcoDimens.InputIconSize),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.app_name), style = EcoTextStyles.ButtonText, color = EcoWhite)
                Text(stringResource(R.string.merchant_location), style = EcoTextStyles.FieldLabel, color = EcoWhite)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.merchant_dashboard_title), style = EcoTextStyles.Title, color = EcoWhite)
            if (storeName.isNotBlank()) Text(storeName, style = EcoTextStyles.Subtitle, color = EcoWhite)
        }
    }
}

// Recuadro del resumen con una cifra destacada y su descripción
@Composable
private fun SummaryTile(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value.toString(), style = EcoTextStyles.Numbers, color = EcoPrimary)
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
    }
}

// Tarjeta de un cupón activo con su costo, disponibilidad, interruptor y botón de edición
@Composable
private fun ActiveCouponCard(coupon: MerchantCoupon, onToggle: (Boolean) -> Unit, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MerchantCouponTitleRow(coupon)
        Text(coupon.description, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        MerchantCouponCostRow(coupon)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            MerchantCouponSwitch(checked = coupon.isActive, onCheckedChange = onToggle)
            MerchantCouponEditButton(onClick = onEdit, modifier = Modifier.height(48.dp))
        }
    }
}

// Botón blanco con el texto editar usado en las tarjetas de cupón
@Composable
internal fun MerchantCouponEditButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(88.dp)
            .clip(RoundedCornerShape(EcoDimens.ControlCornerRadius))
            .background(EcoWhite)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.action_edit), style = EcoTextStyles.LinkText, color = EcoPrimary)
    }
}

// Fila con el icono del cupón, su título y la etiqueta de estado activo o inactivo
@Composable
internal fun MerchantCouponTitleRow(coupon: MerchantCoupon) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.ic_nav_ticket), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(32.dp))
        Text(coupon.title, style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
        Text(
            stringResource(if (coupon.isActive) R.string.merchant_coupon_active else R.string.merchant_coupon_inactive),
            style = EcoTextStyles.FieldLabel,
            color = if (coupon.isActive) EcoGreenSecondary else EcoSubtitle,
        )
    }
}

// Fila con el costo en puntos a la izquierda y la cantidad disponible a la derecha
@Composable
internal fun MerchantCouponCostRow(coupon: MerchantCoupon) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.merchant_coupon_points, coupon.pointsCost), style = EcoTextStyles.LinkText, color = EcoPrimary)
        Text(stringResource(R.string.merchant_coupon_available, coupon.availableQuantity), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

// Interruptor con los colores del tema para activar o desactivar un cupón
@Composable
internal fun MerchantCouponSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val description = stringResource(R.string.merchant_coupon_toggle_description)
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.semantics { contentDescription = description },
        colors = SwitchDefaults.colors(
            checkedThumbColor = EcoWhite,
            checkedTrackColor = EcoAccent,
            uncheckedThumbColor = EcoWhite,
            uncheckedTrackColor = EcoBorder,
            uncheckedBorderColor = EcoBorder,
        ),
    )
}

// Fila de un canje reciente con el código, el descuento aplicado y su hora
@Composable
private fun RedemptionRow(redemption: MerchantRedemption) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EcoDimens.ControlCornerRadius))
            .background(EcoFieldBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(32.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(redemption.code, style = EcoTextStyles.LinkText, color = EcoTitle)
            Text(
                stringResource(R.string.merchant_dashboard_redemption_detail, redemption.discountPercent, redemption.time),
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
            )
        }
        Text(stringResource(R.string.merchant_dashboard_validated), style = EcoTextStyles.FieldLabel, color = EcoGreenSecondary)
    }
}

// Cupón ilustrativo usado solo en las vistas previas del comercio
internal val PreviewMerchantCoupon = MerchantCoupon(
    id = "CUP-01",
    title = "10% en tu compra",
    description = "Descuento en Café del Parque.",
    pointsCost = 200,
    availableQuantity = 50,
    validUntil = "31/12/2026",
    isActive = true,
)

private val PreviewDashboardState = MerchantDashboardUiState(
    storeName = "Café del Parque",
    activeCoupons = listOf(PreviewMerchantCoupon),
    availableTotal = 50,
    redemptionsToday = 2,
    recentRedemptions = listOf(
        MerchantRedemption(code = "ECO-6F8A21C4", discountPercent = 10, time = "10:30 a. m."),
        MerchantRedemption(code = "ECO-5B3D90E7", discountPercent = 10, time = "9:15 a. m."),
    ),
    redemptionsDateLabel = "Hoy, 13 oct.",
)

@Preview(name = "Panel del comercio", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantDashboardPreview() {
    EcoRutaTheme { MerchantDashboardScreen(state = PreviewDashboardState, actions = MerchantDashboardActions()) }
}

@Preview(name = "Panel del comercio · Confirmar desactivación", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantDashboardDeactivateConfirmPreview() {
    EcoRutaTheme {
        MerchantDashboardScreen(
            state = PreviewDashboardState.copy(couponToDeactivate = PreviewMerchantCoupon),
            actions = MerchantDashboardActions(),
        )
    }
}

@Preview(name = "Panel del comercio · Cargando", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantDashboardLoadingPreview() {
    EcoRutaTheme { MerchantDashboardScreen(state = MerchantDashboardUiState(storeName = "Café del Parque", isLoading = true), actions = MerchantDashboardActions()) }
}

@Preview(name = "Panel del comercio · Vacío", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantDashboardEmptyPreview() {
    EcoRutaTheme { MerchantDashboardScreen(state = MerchantDashboardUiState(storeName = "Café del Parque"), actions = MerchantDashboardActions()) }
}

@Preview(name = "Panel del comercio · Error", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantDashboardErrorPreview() {
    EcoRutaTheme { MerchantDashboardScreen(state = MerchantDashboardUiState(storeName = "Café del Parque", hasError = true), actions = MerchantDashboardActions()) }
}

@Preview(name = "Panel del comercio · Sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantDashboardOfflinePreview() {
    EcoRutaTheme { MerchantDashboardScreen(state = MerchantDashboardUiState(isOffline = true), actions = MerchantDashboardActions()) }
}
