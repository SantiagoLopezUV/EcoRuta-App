package com.ecoruta.app.core.ui.components

// Plantilla para servicios que el invitado consulta sin sesión iniciada
// Muestra los beneficios del servicio e invita a iniciar sesión

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Textos descriptivos para presentar un servicio que requiere iniciar sesión
data class GuestServiceContent(
    @StringRes val banner: Int,
    @StringRes val lockedSection: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val benefits: List<Int>,
)

data class GuestServiceActions(
    val onBack: () -> Unit = {},
    val onLogin: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Plantilla compartida para pantallas de servicios consultados sin sesión iniciada
@Composable
fun GuestServiceScreen(
    content: GuestServiceContent,
    actions: GuestServiceActions,
    selectedTab: CitizenTab = CitizenTab.HOME,
) {
    EcoTabScreenLayout(selectedTab = selectedTab, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                .background(EcoPrimary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(content.banner), style = EcoTextStyles.Subtitle, color = EcoWhite)
            EcoPrimaryButton(text = stringResource(R.string.home_login), onClick = actions.onLogin, height = 48.dp)
            Text(stringResource(content.lockedSection), style = EcoTextStyles.FieldLabel, color = EcoWhite)
        }
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(content.title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(content.description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.guest_can_title), style = EcoTextStyles.ButtonText, color = EcoTitle)
            content.benefits.forEach { res ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(EcoPrimary),
                    )
                    Text(stringResource(res), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
            }
        }
    }
}
