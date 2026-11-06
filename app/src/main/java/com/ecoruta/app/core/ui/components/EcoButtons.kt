package com.ecoruta.app.core.ui.components

// Botones reutilizables de acción principal y secundaria de la aplicación
// Soportan estados de carga, deshabilitado e iconos complementarios

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Botón de acción principal con soporte de indicador de carga para evitar envíos duplicados
@Composable
fun EcoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = EcoDimens.ControlHeight,
    cornerRadius: Dp = EcoDimens.ControlCornerRadius,
    @DrawableRes trailingIconRes: Int? = null,
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = EcoAccent,
            contentColor = EcoBlack,
            // Colores aplicados cuando el botón se encuentra deshabilitado
            disabledContainerColor = EcoFieldBackground,
            disabledContentColor = EcoSubtitle,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = EcoBlack,
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Mantiene el texto centrado incluso cuando ocupa más de una línea
                Text(text = text, style = EcoTextStyles.ButtonText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false))
                if (trailingIconRes != null) {
                    // El icono adopta automáticamente el color del contenido del botón
                    Icon(
                        painter = painterResource(trailingIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// Botón secundario con borde y fondo claro para acciones complementarias
@Composable
fun EcoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(EcoDimens.SecondaryControlHeight),
        shape = RoundedCornerShape(EcoDimens.ControlCornerRadius),
        border = BorderStroke(1.dp, EcoBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = EcoWhite,
            contentColor = EcoTitle,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leadingIcon?.invoke()
            Text(text = text, style = EcoTextStyles.ButtonText)
        }
    }
}

@Preview(name = "Botones principales", widthDp = 354)
@Composable
private fun EcoPrimaryButtonPreview() {
    EcoRutaTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EcoPrimaryButton(text = stringResource(R.string.registration_create_account), onClick = {})
            EcoPrimaryButton(text = stringResource(R.string.recycler_application_select_company), onClick = {}, trailingIconRes = R.drawable.ic_arrow_right)
            EcoPrimaryButton(text = stringResource(R.string.registration_create_account), onClick = {}, isLoading = true)
            EcoPrimaryButton(text = stringResource(R.string.registration_create_account), onClick = {}, enabled = false)
        }
    }
}

@Preview(name = "Botones secundarios", widthDp = 354)
@Composable
private fun EcoSecondaryButtonPreview() {
    EcoRutaTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EcoSecondaryButton(
                text = stringResource(R.string.login_google),
                onClick = {},
                leadingIcon = { Icon(painterResource(R.drawable.ic_google), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp)) },
            )
            EcoSecondaryButton(text = stringResource(R.string.login_google), onClick = {}, enabled = false)
        }
    }
}
