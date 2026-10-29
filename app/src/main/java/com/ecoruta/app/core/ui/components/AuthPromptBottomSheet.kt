package com.ecoruta.app.core.ui.components

// Hoja inferior interactiva para invitación de acceso y registro
// Alternativa visual en modal inferior para acciones que requieren autenticación

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Hoja inferior interactiva para invitar al usuario a iniciar sesión o crear cuenta
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPromptBottomSheet(
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.auth_prompt_title),
    subtitle: String = stringResource(R.string.auth_prompt_subtitle),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = EcoWhite,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoFieldBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = EcoPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = EcoTextStyles.Title,
                    color = EcoTitle,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
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
