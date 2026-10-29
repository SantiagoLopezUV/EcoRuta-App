package com.ecoruta.app.core.navigation.components

// Barra de navegación inferior dinámica y reactiva
// Muta de 3 a 6 pestañas según el rol activo o estado visitante (sin sesión)

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Contrato básico para cada pestaña de la barra de navegación inferior
interface EcoBottomBarItem {
    @get:StringRes val labelRes: Int
    @get:DrawableRes val iconRes: Int
}

// Componente visual que muestra las pestañas de navegación en la parte inferior de la pantalla
@Composable
fun <T : EcoBottomBarItem> EcoBottomBar(
    items: List<T>,
    selectedItem: T,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(1.dp, EcoBorder, shape)
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { item ->
            val selected = item == selectedItem
            // Cada pestaña toma el mismo ancho disponible para adaptarse a cualquier tamaño de pantalla
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        // Evita volver a navegar si el usuario toca la pestaña que ya está seleccionada
                        onClick = { if (!selected) onItemClick(item) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (selected) EcoAccent else EcoWhite)
                        .then(if (selected) Modifier else Modifier.border(1.dp, EcoBorder, CircleShape)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        tint = if (selected) EcoBlack else EcoTitle,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = stringResource(item.labelRes),
                    style = EcoTextStyles.NavigationLabel,
                    color = if (selected) EcoAccent else EcoSubtitle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
