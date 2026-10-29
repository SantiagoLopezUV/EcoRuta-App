package com.ecoruta.app.core.ui.components

// Diálogo modal reutilizable con ícono de candado
// Invita al usuario visitante a iniciar sesión o registrarse ante acciones protegidas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Diálogo modal de candado que bloquea acciones privadas e invita a ingresar o crear cuenta
@Composable
fun AuthRequiredDialog(
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.auth_required_title),
    message: String = stringResource(R.string.auth_required_message),
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .clip(shape)
                .background(EcoWhite)
                .border(1.dp, EcoBorder, shape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EcoFieldBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = EcoGreenSecondary,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = EcoTextStyles.Title.copy(fontSize = 20.sp),
                    color = EcoTitle,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = EcoTextStyles.Subtitle,
                    color = EcoSubtitle,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EcoPrimaryButton(
                    text = stringResource(R.string.auth_action_login),
                    onClick = onLoginClick,
                    trailingIconRes = R.drawable.ic_arrow_right,
                )
                EcoSecondaryButton(
                    text = stringResource(R.string.auth_action_register),
                    onClick = onRegisterClick,
                )
                EcoTextLink(
                    text = stringResource(R.string.auth_action_continue_browsing),
                    onClick = onDismiss,
                )
            }
        }
    }
}

// Previsualización del diálogo de autenticación requerida
@Preview(name = "Diálogo de candado requerido")
@Composable
private fun AuthRequiredDialogPreview() {
    EcoRutaTheme {
        AuthRequiredDialog(
            onDismiss = {},
            onLoginClick = {},
            onRegisterClick = {},
        )
    }
}
