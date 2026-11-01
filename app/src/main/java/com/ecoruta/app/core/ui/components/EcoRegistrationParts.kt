package com.ecoruta.app.core.ui.components

// Componentes visuales para flujos de registro y formularios por etapas
// Incluye cabecera de progreso, barra de acciones inferior, tarjetas de selección y casillas

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Cabecera de formularios guiados que muestra la marca, el paso actual y una barra de progreso
@Composable
fun EcoStepHeader(
    stepLabel: String,
    stageLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
    showBrand: Boolean = true,
    onBackClick: (() -> Unit)? = null,
) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), label = "progreso")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = EcoDimens.ScreenHorizontalPadding, end = EcoDimens.ScreenHorizontalPadding, top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showBrand || onBackClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.action_back),
                            tint = EcoTitle,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Image(
                    painter = painterResource(R.drawable.ecoruta_logo_circular),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = EcoTextStyles.Title.copy(fontSize = 18.sp),
                    color = EcoPrimary,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stepLabel, style = EcoTextStyles.LinkText, color = EcoTitle)
            Text(stageLabel, style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(EcoBorder),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(EcoPrimary),
            )
        }
    }
}

// Contenedor inferior fijo con línea divisoria superior para agrupar botones de acción
@Composable
fun EcoBottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 1.dp, color = EcoBorder)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = EcoDimens.ScreenHorizontalPadding, end = EcoDimens.ScreenHorizontalPadding, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

// Tarjeta seleccionable con icono, título, descripción e indicador circular de selección
@Composable
fun EcoSelectionCard(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(if (selected) 2.dp else 1.dp, if (selected) EcoAccent else EcoBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) EcoAccent else EcoGreenSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = EcoWhite,
                    modifier = Modifier.size(iconSize),
                )
            }
            Text(title, style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(if (selected) R.drawable.ic_circle_check else R.drawable.ic_circle_empty),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(description, style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

// Fila con casilla de verificación, etiqueta descriptiva y enlace opcional a la derecha
@Composable
fun EcoCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    linkText: String? = null,
    onLinkClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                val boxShape = RoundedCornerShape(4.dp)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(boxShape)
                        .background(if (checked) EcoAccent else EcoWhite)
                        .border(1.dp, if (checked) EcoAccent else EcoSubtitle, boxShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) {
                        Icon(
                            painter = painterResource(R.drawable.ic_checkbox_check),
                            contentDescription = null,
                            tint = EcoWhite,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Text(text, style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        if (linkText != null) {
            EcoTextLink(text = linkText, onClick = onLinkClick)
        }
    }
}
