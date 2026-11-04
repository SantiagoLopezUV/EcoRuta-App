package com.ecoruta.app.features.gamification.presentation.cashier_validation

// Vista composable para el rol Comercio Aliado en caja
// Permite ingresar o escanear código de cupón y aplica descuento automático

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoCodeInput
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.circular_economy.presentation.scanner.QrCameraPreview

// Pantalla de caja con el escáner del QR, el código manual y el botón para verificar
@Composable
fun MerchantCashierScreen(state: MerchantCashierUiState, actions: MerchantCashierActions) {
    if (state.isOffline) {
        OfflineScreen(onRetry = actions.onRetry)
        return
    }
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.VALIDATE,
        onTabClick = actions.onTabClick,
        contentSpacing = 24.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.merchant_cashier_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.merchant_cashier_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        ScannerCard(isScanning = state.isScanning, actions = actions)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.merchant_cashier_manual),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            ManualCodeField(state = state, actions = actions)
            if (state.hasError) EcoErrorMessage(text = stringResource(R.string.merchant_cashier_error_server))
            EcoPrimaryButton(
                text = stringResource(R.string.merchant_cashier_verify),
                onClick = actions.onVerify,
                enabled = state.isCodeComplete,
                isLoading = state.isVerifying,
            )
            Text(
                if (state.storeName.isBlank()) {
                    stringResource(R.string.merchant_cashier_hint_generic)
                } else {
                    stringResource(R.string.merchant_cashier_hint, state.storeName)
                },
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
    }
}

// Recuadro del escáner que muestra la cámara mientras se escanea o la invitación a escanear
@Composable
private fun ScannerCard(isScanning: Boolean, actions: MerchantCashierActions) {
    val shape = RoundedCornerShape(16.dp)
    if (isScanning) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(shape),
            ) {
                QrCameraPreview(onQrDetected = actions.onQrDetected, modifier = Modifier.fillMaxSize())
            }
            EcoSecondaryButton(text = stringResource(R.string.action_cancel), onClick = actions.onStopScan)
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(shape)
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClick = actions.onStartScan)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(shape)
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_qr_code), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(40.dp))
        }
        Text(stringResource(R.string.merchant_cashier_scan_title), style = EcoTextStyles.LinkText, color = EcoPrimary)
        Text(stringResource(R.string.merchant_cashier_scan_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

// Campo del código con el prefijo ECO fijo y las ocho casillas que escribe el comercio
@Composable
private fun ManualCodeField(state: MerchantCashierUiState, actions: MerchantCashierActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.merchant_cashier_code_label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.merchant_cashier_code_prefix), style = EcoTextStyles.Numbers, color = EcoTitle)
            EcoCodeInput(
                value = state.code,
                onValueChange = actions.onCodeChange,
                length = COUPON_CODE_LENGTH,
                accessibilityLabel = stringResource(R.string.merchant_cashier_code_accessibility),
                isError = state.validationError != null,
                onDone = { if (state.isCodeComplete) actions.onVerify() },
                allowLetters = true,
                modifier = Modifier.weight(1f),
            )
        }
        state.validationError?.let { EcoErrorMessage(text = stringResource(it.messageRes())) }
    }
}

// Texto que explica al comercio por qué no se pudo validar el código
private fun CashierValidationError.messageRes(): Int = when (this) {
    CashierValidationError.INCOMPLETE_CODE -> R.string.merchant_cashier_error_incomplete
    CashierValidationError.NOT_FOUND -> R.string.merchant_cashier_error_not_found
    CashierValidationError.ALREADY_REDEEMED -> R.string.merchant_cashier_error_redeemed
    CashierValidationError.EXPIRED -> R.string.merchant_cashier_error_expired
    CashierValidationError.OTHER_STORE -> R.string.merchant_cashier_error_other_store
}

@Preview(name = "Validar cupón en caja", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantCashierPreview() {
    EcoRutaTheme {
        MerchantCashierScreen(state = MerchantCashierUiState(storeName = "Café del Parque", code = "7K2M90AB"), actions = MerchantCashierActions())
    }
}

@Preview(name = "Validar cupón en caja · Vacío", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantCashierEmptyPreview() {
    EcoRutaTheme { MerchantCashierScreen(state = MerchantCashierUiState(), actions = MerchantCashierActions()) }
}

@Preview(name = "Validar cupón en caja · Validación", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantCashierValidationPreview() {
    EcoRutaTheme {
        MerchantCashierScreen(
            state = MerchantCashierUiState(
                storeName = "Café del Parque",
                code = "7K2M90AB",
                validationError = CashierValidationError.ALREADY_REDEEMED,
            ),
            actions = MerchantCashierActions(),
        )
    }
}

@Preview(name = "Validar cupón en caja · Error", widthDp = 393, heightDp = 950)
@Composable
private fun MerchantCashierErrorPreview() {
    EcoRutaTheme {
        MerchantCashierScreen(
            state = MerchantCashierUiState(storeName = "Café del Parque", code = "7K2M90AB", hasError = true),
            actions = MerchantCashierActions(),
        )
    }
}

@Preview(name = "Validar cupón en caja · Sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantCashierOfflinePreview() {
    EcoRutaTheme { MerchantCashierScreen(state = MerchantCashierUiState(isOffline = true), actions = MerchantCashierActions()) }
}
