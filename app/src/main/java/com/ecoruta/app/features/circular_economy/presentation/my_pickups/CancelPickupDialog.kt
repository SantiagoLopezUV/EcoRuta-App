package com.ecoruta.app.features.circular_economy.presentation.my_pickups

// Diálogo que pide confirmar antes de cancelar una recogida pendiente
// Avisa que la entrega vuelve a quedar disponible y que el ciudadano recibirá un aviso

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

// Ofrece volver sin cambios o cancelar la recogida; mientras se cancela el botón queda inactivo
@Composable
fun CancelPickupDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, isCancelling: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recycler_pickups_cancel_dialog_title), style = EcoTextStyles.ButtonText, color = EcoTitle) },
        text = { Text(stringResource(R.string.recycler_pickups_cancel_dialog_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCancelling,
                shape = RoundedCornerShape(EcoDimens.ControlCornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = EcoError, contentColor = EcoWhite),
            ) { Text(stringResource(R.string.recycler_pickups_cancel), style = EcoTextStyles.ButtonText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_back), style = EcoTextStyles.LinkText, color = EcoGreenSecondary)
            }
        },
        containerColor = EcoWhite,
    )
}

@Preview(name = "Cancelar recogida · Confirmación", widthDp = 393, heightDp = 852)
@Composable
private fun CancelPickupDialogPreview() {
    EcoRutaTheme { CancelPickupDialog(onConfirm = {}, onDismiss = {}) }
}
