package com.ecoruta.app.features.gamification.presentation.benefits

// Vista composable del catálogo de beneficios comerciales
// Muestra saldo acumulado de EcoPuntos y filtros por categoría (Cafés, Mercado, etc.)

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
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

@StringRes
fun CouponCategory.labelRes(): Int = when (this) {
    CouponCategory.GASTRONOMIA -> R.string.coupon_category_gastronomia
    CouponCategory.MERCADO -> R.string.coupon_category_mercado
    CouponCategory.PAPELERIA -> R.string.coupon_category_papeleria
}

// Formatea el puntaje con separador de miles colombiano
fun formatPoints(points: Int): String = NumberFormat.getIntegerInstance(SpanishColombia).format(points)

// Formatea el movimiento de puntos con signo positivo o negativo para el historial
fun formatSignedPoints(points: Int): String = if (points >= 0) "+${formatPoints(points)}" else "−${formatPoints(-points)}"

// Formatea la fecha de vigencia del cupón en texto completo
fun formatCouponDate(millis: Long): String = SimpleDateFormat("d 'de' MMMM 'de' yyyy", SpanishColombia).format(Date(millis))

// Formatea la fecha del cupón o movimiento en versión abreviada
fun formatShortCouponDate(millis: Long): String = SimpleDateFormat("d MMM yyyy", SpanishColombia).format(Date(millis))

// Indicador de carga centrado para las vistas de beneficios
@Composable
fun BenefitsLoading() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = EcoPrimary) }
}

// Barra horizontal de filtros por categoría de beneficio
@Composable
fun CategoryFilterRow(selected: CouponCategory?, onSelected: (CouponCategory?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(stringResource(R.string.coupon_category_all), selected == null) { onSelected(null) }
        CouponCategory.entries.forEach { category ->
            FilterPill(stringResource(category.labelRes()), selected == category) { onSelected(category) }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        text = text,
        style = EcoTextStyles.FieldLabel,
        color = if (selected) EcoWhite else EcoTitle,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) EcoPrimary else EcoFieldBackground)
            .border(1.dp, if (selected) EcoPrimary else EcoBorder, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

// Tarjeta destacada con el saldo disponible de EcoPuntos
@Composable
fun BalanceCard(
    label: String,
    balance: Int?,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EcoPrimary)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoWhite.copy(alpha = 0.85f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_ecopoints_leaf), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            Text(
                if (balance != null) stringResource(R.string.benefits_points_value, formatPoints(balance)) else stringResource(R.string.benefits_balance_unknown),
                style = EcoTextStyles.Title,
                color = EcoWhite,
            )
        }
        footer?.invoke()
    }
}

// Fila de etiqueta y valor para los cuadros de resumen de puntos y canjes
@Composable
fun SummaryRow(label: String, value: String, emphasized: Boolean = false, valueColor: Color = EcoTitle) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (emphasized) EcoTextStyles.ButtonText else EcoTextStyles.Subtitle, color = if (emphasized) EcoTitle else EcoSubtitle, modifier = Modifier.weight(1f))
        Text(value, style = EcoTextStyles.ButtonText, color = valueColor, textAlign = TextAlign.End)
    }
}

// Contenedor con borde para agrupar filas de resumen
@Composable
fun SummaryBox(title: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) Text(title, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        content()
    }
}

// Insignia con icono de comercio para las tarjetas de cupón
@Composable
fun StoreBadge(size: Int = 44) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EcoPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_store), contentDescription = null, tint = EcoWhite, modifier = Modifier.size((size / 2).dp))
    }
}

// Tarjeta de cupón dentro del catálogo de beneficios
@Composable
fun CouponCard(coupon: Coupon, onClick: () -> Unit, affordable: Boolean?) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(1.dp, EcoBorder, shape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.coupon_open), onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoreBadge()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(coupon.businessName, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            Text(coupon.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(stringResource(coupon.category.labelRes()), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.benefits_points_short, formatPoints(coupon.pointsCost)), style = EcoTextStyles.ButtonText, color = EcoPrimary)
            if (affordable == false) {
                Text(stringResource(R.string.coupon_not_enough_short), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = EcoSubtitle, modifier = Modifier.size(20.dp))
    }
}

// Indicador visual de cupón disponible para usar
@Composable
fun AvailableMark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(EcoAccent),
        )
        Text(stringResource(R.string.my_coupons_available_to_use), style = EcoTextStyles.FieldLabel, color = EcoPrimary)
    }
}

data class BenefitsActions(
    val onBack: () -> Unit = {},
    val onCouponClick: (Coupon) -> Unit = {},
    val onCategorySelected: (CouponCategory?) -> Unit = {},
    val onMyCoupons: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
private fun BenefitsHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.benefits_title), style = EcoTextStyles.Title, color = EcoTitle)
        Text(stringResource(R.string.benefits_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

// Pantalla principal del catálogo de beneficios con saldo, acceso a cupones e historial
@Composable
fun BenefitsScreen(state: BenefitsUiState, actions: BenefitsActions) {
    EcoTabScreenLayout(selectedTab = CitizenTab.BENEFITS, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        BenefitsHeader()
        when {
            state.isLoading -> BenefitsLoading()
            state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(state.error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            else -> {
                BalanceCard(label = stringResource(R.string.benefits_balance_label), balance = state.balance) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.benefits_tagline), style = EcoTextStyles.Subtitle, color = EcoWhite.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                        Text(
                            stringResource(R.string.benefits_view_history),
                            style = EcoTextStyles.LinkText,
                            color = EcoWhite,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button, onClick = actions.onHistory)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }
                EcoPrimaryButton(
                    text = stringResource(R.string.benefits_my_coupons),
                    onClick = actions.onMyCoupons,
                    trailingIconRes = R.drawable.ic_arrow_right,
                )
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.benefits_sample_hint))
                CategoryFilterRow(selected = state.category, onSelected = actions.onCategorySelected)
                if (state.visibleCoupons.isEmpty()) {
                    Text(stringResource(R.string.benefits_empty_category), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.visibleCoupons.forEach { coupon ->
                            CouponCard(
                                coupon = coupon,
                                onClick = { actions.onCouponClick(coupon) },
                                affordable = state.balance?.let { it >= coupon.pointsCost },
                            )
                        }
                    }
                }
            }
        }
    }
}

// Vista del catálogo de beneficios para usuarios sin sesión activa
@Composable
fun GuestBenefitsScreen(
    state: BenefitsUiState,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onCategorySelected: (CouponCategory?) -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.BENEFITS,
        onBack = onBack,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.home_login), onClick = onLogin, trailingIconRes = R.drawable.ic_arrow_right)
        },
    ) {
        BenefitsHeader()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EcoPrimary)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.benefits_balance_label), style = EcoTextStyles.FieldLabel, color = EcoWhite.copy(alpha = 0.85f))
            Text(stringResource(R.string.benefits_guest_balance), style = EcoTextStyles.ButtonText, color = EcoWhite)
        }
        GuestLockedRow(text = stringResource(R.string.benefits_guest_my_coupons))
        if (state.isSample) EcoInfoHint(text = stringResource(R.string.benefits_sample_hint))
        if (state.isLoading) {
            BenefitsLoading()
        } else {
            CategoryFilterRow(selected = state.category, onSelected = onCategorySelected)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.visibleCoupons.forEach { coupon -> CouponCard(coupon = coupon, onClick = onLogin, affordable = null) }
            }
        }
    }
}

@Composable
private fun GuestLockedRow(text: String) {
    Text(
        text,
        style = EcoTextStyles.Subtitle,
        color = EcoSubtitle,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EcoFieldBackground)
            .border(1.dp, EcoBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
    )
}

// Pantalla con el historial detallado de movimientos de EcoPuntos
@Composable
fun PointsHistoryScreen(
    state: PointsHistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    EcoTabScreenLayout(selectedTab = CitizenTab.BENEFITS, onBack = onBack, onTabClick = onTabClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.points_history_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.points_history_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when (state) {
            PointsHistoryUiState.Loading -> BenefitsLoading()
            PointsHistoryUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.points_history_load_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = onRetry)
            }
            is PointsHistoryUiState.Loaded -> {
                BalanceCard(label = stringResource(R.string.benefits_balance_label), balance = state.balance)
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.points_history_sample_hint))
                if (state.movements.isEmpty()) {
                    Text(stringResource(R.string.points_history_empty), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, EcoBorder, RoundedCornerShape(16.dp)),
                    ) {
                        state.movements.forEachIndexed { index, movement ->
                            if (index > 0) HorizontalDivider(color = EcoBorder)
                            MovementRow(movement)
                        }
                    }
                    SummaryBox {
                        state.initialBalance?.let {
                            SummaryRow(stringResource(R.string.points_history_initial), stringResource(R.string.benefits_points_short, formatPoints(it)))
                        }
                        SummaryRow(stringResource(R.string.points_history_net), stringResource(R.string.benefits_points_short, formatSignedPoints(state.netMovement)))
                        state.balance?.let {
                            SummaryRow(stringResource(R.string.points_history_final), stringResource(R.string.benefits_points_short, formatPoints(it)), emphasized = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovementRow(movement: PointsMovement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(movement.description, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(formatShortCouponDate(movement.dateMillis), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
        Text(
            formatSignedPoints(movement.points),
            style = EcoTextStyles.ButtonText,
            color = if (movement.points >= 0) EcoPrimary else EcoTitle,
        )
    }
}

private val previewCoupon = Coupon(
    id = "CUP-01",
    businessName = "Café del Río",
    title = "20 % de descuento en bebidas calientes",
    subtitle = "Válido de lunes a viernes",
    category = CouponCategory.GASTRONOMIA,
    pointsCost = 450,
    availableQuantity = 18,
    validUntilMillis = 1_795_996_800_000L,
    conditions = listOf("Un cupón por compra"),
)

@Preview(name = "Beneficios", widthDp = 393, heightDp = 1000)
@Composable
private fun BenefitsPreview() {
    EcoRutaTheme {
        BenefitsScreen(
            BenefitsUiState(
                isLoading = false,
                balance = 1_250,
                coupons = listOf(previewCoupon),
                isSample = true,
            ),
            BenefitsActions(),
        )
    }
}

@Preview(name = "Beneficios sin sesión", widthDp = 393, heightDp = 900)
@Composable
private fun GuestBenefitsPreview() {
    EcoRutaTheme {
        GuestBenefitsScreen(
            state = BenefitsUiState(
                isLoading = false,
                coupons = listOf(previewCoupon),
                isSample = false,
            ),
            onBack = {},
            onLogin = {},
            onCategorySelected = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Historial de EcoPuntos", widthDp = 393, heightDp = 900)
@Composable
private fun PointsHistoryPreview() {
    EcoRutaTheme {
        PointsHistoryScreen(
            state = PointsHistoryUiState.Loaded(
                balance = 1_250,
                movements = listOf(
                    PointsMovement("mov-1", "Entrega de material reciclable (PET)", 1_744_850_000_000L, 80),
                    PointsMovement("mov-2", "Canje de cupón · Café del Río", 1_744_700_000_000L, -450),
                    PointsMovement("mov-3", "Guía completada · Separación en casa", 1_744_500_000_000L, 50),
                ),
                isSample = false,
            ),
            onBack = {},
            onRetry = {},
            onTabClick = {},
        )
    }
}
