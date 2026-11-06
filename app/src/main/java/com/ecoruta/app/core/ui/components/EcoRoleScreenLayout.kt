package com.ecoruta.app.core.ui.components

// Contenedor de pantallas para los roles reciclador, chofer y comercio con barra inferior propia
// Muestra la marca EcoRuta o el botón de regreso, el contenido desplazable y las pestañas del rol

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.EcoBottomBar
import com.ecoruta.app.core.navigation.components.EcoBottomBarItem
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Pantalla con cabecera de marca, contenido desplazable y barra inferior con las pestañas del rol activo
@Composable
fun <T : EcoBottomBarItem> EcoRoleTabScreenLayout(
    items: List<T>,
    selectedTab: T,
    onTabClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    compactHeader: Boolean = false,
    // Permite ocultar la fila de marca cuando el contenido ya incluye su propia cabecera
    showBrandHeader: Boolean = true,
    contentSpacing: Dp = 16.dp,
    // Acción opcional que se ubica a la derecha de la marca en la cabecera
    headerAction: (@Composable () -> Unit)? = null,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            if (compactHeader) {
                EcoTopBar(onBackClick = onBack)
            } else if (showBrandHeader) {
                EcoRoleBrandHeader(onBack = onBack, action = headerAction)
            }
            content()
        }
        if (bottomActions != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = bottomActions,
            )
        }
        EcoBottomBar(
            items = items,
            selectedItem = selectedTab,
            onItemClick = onTabClick,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

// Cabecera con el logo de EcoRuta o, si hay acción de regreso, con el botón para volver
@Composable
fun EcoRoleBrandHeader(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            val backLabel = stringResource(R.string.action_back)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EcoFieldBackground)
                    .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = backLabel,
                    tint = EcoTitle,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brand_leaf),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(stringResource(R.string.app_name), style = EcoTextStyles.Title, color = EcoPrimary)
        // La acción opcional queda empujada al borde derecho de la cabecera
        if (action != null) {
            Spacer(Modifier.weight(1f))
            action()
        }
    }
}

@Preview(name = "Plantilla con pestañas del comercio", widthDp = 393, heightDp = 852)
@Composable
private fun EcoRoleTabScreenLayoutPreview() {
    EcoRutaTheme {
        EcoRoleTabScreenLayout(
            items = MerchantTab.entries,
            selectedTab = MerchantTab.DASHBOARD,
            onTabClick = {},
            onBack = {},
            bottomActions = { EcoPrimaryButton(text = stringResource(R.string.merchant_tab_create), onClick = {}) },
        ) {
            EcoEmptyStateCard(
                illustrationRes = R.drawable.ic_nav_ticket,
                heading = stringResource(R.string.merchant_dashboard_empty_heading),
                body = stringResource(R.string.merchant_dashboard_empty_body),
            )
        }
    }
}

@Preview(name = "Cabecera de marca por rol", widthDp = 393)
@Composable
private fun EcoRoleBrandHeaderPreview() {
    EcoRutaTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EcoRoleBrandHeader(onBack = {})
            EcoRoleBrandHeader(onBack = null)
        }
    }
}
