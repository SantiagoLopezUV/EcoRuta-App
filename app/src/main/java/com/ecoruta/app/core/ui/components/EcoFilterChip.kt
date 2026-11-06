package com.ecoruta.app.core.ui.components

// Chip de filtro compartido con forma de píldora para elegir una opción dentro de una fila
// El chip elegido usa el color de acento y los demás quedan blancos con borde suave

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoWhite

// Chip seleccionable que se comporta como pestaña dentro de un grupo de filtros
@Composable
fun EcoFilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(EcoDimens.PillCornerRadius)
    Box(
        modifier = modifier
            .heightIn(min = EcoDimens.MinTouchTarget)
            .clip(shape)
            .background(if (selected) EcoAccent else EcoWhite)
            .then(if (selected) Modifier else Modifier.border(EcoDimens.BorderWidth, EcoFieldBackground, shape))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = EcoDimens.ChipHorizontalPadding, vertical = EcoDimens.ChipVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = EcoTextStyles.LinkText, color = if (selected) EcoBlack else EcoSubtitle)
    }
}

@Preview(name = "Chip de filtro · Elegido y sin elegir", widthDp = 393)
@Composable
private fun EcoFilterChipPreview() {
    EcoRutaTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(EcoDimens.ChipSpacing), verticalAlignment = Alignment.CenterVertically) {
            EcoFilterChip(text = stringResource(R.string.coupon_category_all), selected = true, onClick = {})
            EcoFilterChip(text = stringResource(R.string.publication_status_available), selected = false, onClick = {})
        }
    }
}
