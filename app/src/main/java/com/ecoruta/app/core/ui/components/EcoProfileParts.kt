package com.ecoruta.app.core.ui.components

// Componentes de cabecera superior y filas de opciones para pantallas del ciudadano
// Incluye barra superior con marca y tarjetas de acceso a opciones de cuenta

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Barra superior con botón opcional para regresar, acción central opcional y marca de la aplicación
@Composable
fun EcoTopBar(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    centerAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            val backLabel = stringResource(R.string.action_back)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EcoFieldBackground)
                    .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = backLabel,
                    tint = EcoTitle,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (centerAction != null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { centerAction() }
        }
        Row(
            modifier = if (centerAction != null) Modifier else Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brand_leaf),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(stringResource(R.string.app_name), style = EcoTextStyles.Boton, color = EcoPrimary)
        }
    }
}

// Fila interactiva para opciones del perfil con icono a la izquierda e indicador de acceso o bloqueo
@Composable
fun EcoAccountOption(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(text, style = EcoTextStyles.Enlace, color = EcoPrimary, modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(if (locked) R.drawable.ic_lock else R.drawable.ic_chevron_right),
            contentDescription = if (locked) stringResource(R.string.profile_requires_login_short) else null,
            tint = EcoSubtitle,
            modifier = Modifier.size(20.dp),
        )
    }
}
