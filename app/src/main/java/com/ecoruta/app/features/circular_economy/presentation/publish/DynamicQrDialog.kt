package com.ecoruta.app.features.circular_economy.presentation.publish

// Diálogo composable con código QR dinámico de un solo uso
// Muestra contador regresivo de expiración a 5 minutos para validación presencial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSampleQrCode
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import kotlinx.coroutines.delay

// Pantalla que muestra el código QR temporal de un solo uso para confirmar la entrega del material
@Composable
fun DeliveryCodeScreen(state: DeliveryCodeUiState, onRenew: () -> Unit, onBack: () -> Unit) {
    val recyclerName = state.publication?.recycler?.name
    FocusLayout(onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycling_qr_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                if (recyclerName != null) stringResource(R.string.recycling_qr_show_to, recyclerName) else stringResource(R.string.recycling_qr_show_generic),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        val code = state.code
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
            state.error != null || code == null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EcoErrorMessage(text = stringResource(state.error ?: R.string.recycling_code_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = onRenew)
            }
            else -> CodeCard(code = code, isSample = state.isSample, onRenew = onRenew)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.recycling_qr_single_use), style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(stringResource(R.string.recycling_qr_do_not_share), style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center)
        }
        state.publication?.let { PublicationSummary(it) }
        Text(
            stringResource(R.string.recycling_qr_confirmation),
            style = EcoTextStyles.Subtitle,
            color = EcoSubtitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CodeCard(code: DeliveryCode, isSample: Boolean, onRenew: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(code) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= code.expiresAtMillis) break
            delay(1_000)
        }
    }
    val remainingSeconds = ((code.expiresAtMillis - now) / 1_000).coerceAtLeast(0)
    val expired = remainingSeconds == 0L
    val qrDescription = stringResource(if (isSample) R.string.recycling_qr_sample_description else R.string.recycling_qr_title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EcoFieldBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EcoWhite)
                .padding(16.dp)
                .semantics { contentDescription = qrDescription },
            contentAlignment = Alignment.Center,
        ) {
            if (isSample) {
                EcoSampleQrCode(seed = code.token, faded = expired)
            } else {
                Text(
                    code.token,
                    style = EcoTextStyles.Numbers.copy(fontFamily = FontFamily.Monospace, fontSize = 22.sp),
                    color = if (expired) EcoSubtitle else EcoTitle,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (isSample) EcoStatusChip(stringResource(R.string.recycling_qr_sample_label), EcoTitle, EcoWhite)
        if (expired) {
            Text(stringResource(R.string.recycling_qr_expired), style = EcoTextStyles.ButtonText, color = EcoError)
            EcoTextLink(text = stringResource(R.string.recycling_qr_renew), onClick = onRenew)
        } else {
            Text(stringResource(R.string.recycling_qr_expires_in), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            Text(
                "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                style = EcoTextStyles.Numbers,
                color = EcoTitle,
            )
        }
        if (isSample) EcoInfoHint(text = stringResource(R.string.recycling_qr_library_pending))
    }
}

@Preview(name = "Código QR de entrega", widthDp = 393, heightDp = 900)
@Composable
private fun DeliveryCodePreview() {
    EcoRutaTheme {
        DeliveryCodeScreen(
            DeliveryCodeUiState(
                isLoading = false,
                code = DeliveryCode(token = "ECO-2026-0412", expiresAtMillis = System.currentTimeMillis() + DeliveryCode.VALIDITY_MS),
                isSample = true,
            ),
            onRenew = {},
            onBack = {},
        )
    }
}
