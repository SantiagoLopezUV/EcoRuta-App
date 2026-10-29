package com.ecoruta.app.features.profile.presentation.components

// Campos y bloques compartidos por los formularios de postulación a comercio y reciclador

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

// Componentes compartidos para los formularios de solicitud de rol
@Composable
internal fun FieldIcon(@DrawableRes iconRes: Int) {
    Icon(painterResource(iconRes), contentDescription = null, tint = EcoSubtitle, modifier = Modifier.size(20.dp))
}

@Composable
internal fun <T> SelectField(
    label: String,
    placeholder: String,
    @DrawableRes iconRes: Int,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    errorMessage: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(EcoDimens.ControlCornerRadius)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoTitle)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(EcoFieldBackground)
                    .then(if (errorMessage != null) Modifier.border(1.5.dp, EcoError, shape) else Modifier)
                    .clickable(role = Role.DropdownList, onClickLabel = label) { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FieldIcon(iconRes)
                Text(
                    selected?.let { optionLabel(it) } ?: placeholder,
                    style = EcoTextStyles.Subtitle,
                    color = if (selected != null) EcoTitle else EcoSubtitle,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = EcoSubtitle,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(90f),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), style = EcoTextStyles.Subtitle, color = EcoTitle) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        },
                    )
                }
            }
        }
        if (errorMessage != null) EcoErrorMessage(text = errorMessage)
    }
}

@Composable
internal fun SupportInfo(@DrawableRes iconRes: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
        Text(text, style = EcoTextStyles.Subtitle, color = EcoSubtitle, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ReadOnlyField(label: String, value: String, @DrawableRes iconRes: Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(EcoDimens.ControlCornerRadius))
                .background(EcoFieldBackground)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FieldIcon(iconRes)
            Text(value, style = EcoTextStyles.Subtitle, color = EcoSubtitle, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = EcoSubtitle, modifier = Modifier.size(20.dp))
        }
    }
}
