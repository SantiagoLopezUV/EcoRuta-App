package com.ecoruta.app.core.ui.components

// Componentes visuales compartidos para las pantallas de autenticación
// Incluye cabecera de marca, insignias de icono, selector de visibilidad de clave y enlaces

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoTextStyles

// Cabecera de identidad para pantallas de acceso con opción de mostrar un botón para regresar
@Composable
fun EcoBrandHeader(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val backLabel = stringResource(R.string.action_back)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EcoPrimary)
                .then(
                    if (onBackClick != null) {
                        Modifier.clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBackClick)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (onBackClick != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_brand_back),
                    contentDescription = backLabel,
                    tint = Color.Unspecified,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_brand_leaf),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(stringResource(R.string.app_name), style = EcoTextStyles.Titulo, color = EcoPrimary)
    }
}

// Recuadro decorativo con fondo de color primario e icono central
@Composable
fun EcoIconBadge(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(EcoPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(iconSize),
        )
    }
}

// Icono decorativo ubicado al inicio de un campo de formulario
@Composable
fun EcoInputIcon(@DrawableRes iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(EcoDimens.InputIconSize),
    )
}

// Botón de icono para alternar entre mostrar y ocultar el texto de una contraseña
@Composable
fun EcoPasswordVisibilityToggle(isVisible: Boolean, onToggle: () -> Unit) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier
            .offset(x = 10.dp)
            .size(40.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_eye),
            contentDescription = stringResource(
                if (isVisible) R.string.login_hide_password else R.string.login_show_password,
            ),
            // Cuando la contraseña está visible se resalta el icono con el color primario
            tint = if (isVisible) EcoPrimary else Color.Unspecified,
            modifier = Modifier.size(EcoDimens.InputIconSize),
        )
    }
}

// Enlace de texto interactivo con soporte opcional para mostrar un icono al inicio
@Composable
fun EcoTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leadingIconRes != null) {
            Icon(
                painter = painterResource(leadingIconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(text = text, style = EcoTextStyles.Enlace, color = EcoPrimary)
    }
}
