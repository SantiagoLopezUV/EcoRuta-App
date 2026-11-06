package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Diálogo que pide confirmar antes de desactivar un cupón del comercio aliado
// Lo comparten el panel y la lista de cupones para que la acción no se ejecute por un toque accidental

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Explica que el cupón deja de poder canjearse y ofrece cancelar o desactivar
@Composable
fun MerchantDeactivateCouponDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.merchant_coupons_deactivate_title), style = EcoTextStyles.ButtonText, color = EcoTitle) },
        text = { Text(stringResource(R.string.merchant_coupons_deactivate_message), style = EcoTextStyles.Subtitle, color = EcoSubtitle) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(EcoDimens.ControlCornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = EcoError, contentColor = EcoWhite),
            ) { Text(stringResource(R.string.merchant_coupons_deactivate), style = EcoTextStyles.ButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), style = EcoTextStyles.LinkText, color = EcoGreenSecondary)
            }
        },
        containerColor = EcoWhite,
    )
}

@Preview(name = "Desactivar cupón · Confirmación", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantDeactivateCouponDialogPreview() {
    EcoRutaTheme { MerchantDeactivateCouponDialog(onConfirm = {}, onDismiss = {}) }
}
