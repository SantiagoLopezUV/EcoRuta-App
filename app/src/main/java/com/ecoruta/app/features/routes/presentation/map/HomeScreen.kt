package com.ecoruta.app.features.routes.presentation.map

// Pantalla de inicio para el ciudadano con o sin sesión iniciada
// Muestra el saludo y los accesos a los servicios principales

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.components.EcoBottomBar
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoServiceCard
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Servicios principales disponibles en la pantalla de inicio ciudadano y público
enum class PublicService(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
) {
    SPECIAL_WASTE(R.string.service_special_waste_title, R.string.service_special_waste_description, R.drawable.ic_truck),
    GARBAGE(R.string.service_garbage_title, R.string.service_garbage_description, R.drawable.ic_trash),
    RECYCLING(R.string.service_recycling_title, R.string.service_recycling_description, R.drawable.ic_recycle),
    REPORTS(R.string.service_reports_title, R.string.service_reports_description, R.drawable.ic_report_alert),
}

// Acciones de interacción de la pantalla de inicio
data class HomeActions(
    val onHelpClick: () -> Unit = {},
    val onNotificationsClick: () -> Unit = {},
    val onLoginClick: () -> Unit = {},
    val onLogoutClick: () -> Unit = {},
    val onServiceClick: (PublicService) -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun HomeScreen(
    actions: HomeActions,
    modifier: Modifier = Modifier,
    isSignedIn: Boolean = false,
    userName: String? = null,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WelcomeHeader(
                title = when {
                    !isSignedIn -> stringResource(R.string.home_welcome)
                    userName.isNullOrBlank() -> stringResource(R.string.home_greeting_no_name)
                    else -> stringResource(R.string.home_greeting, userName)
                },
                onHelpClick = actions.onHelpClick,
                onNotificationsClick = actions.onNotificationsClick,
                onLogoutClick = if (isSignedIn) actions.onLogoutClick else null,
            )

            if (!isSignedIn) EcoPrimaryButton(
                text = stringResource(R.string.home_login),
                onClick = actions.onLoginClick,
                height = 48.dp,
                cornerRadius = 24.dp,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = EcoAccent,
                        spotColor = EcoAccent,
                    ),
            )

            ServicesSection(onServiceClick = actions.onServiceClick)
        }

        EcoBottomBar(
            items = CitizenTab.entries,
            selectedItem = CitizenTab.HOME,
            onItemClick = actions.onTabClick,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun WelcomeHeader(
    title: String,
    onHelpClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLogoutClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 185.dp)
            .clip(RoundedCornerShape(topStart = 28.dp))
            .background(EcoPrimary)
            .background(
                Brush.horizontalGradient(
                    0f to EcoPrimary,
                    0.55f to EcoAccent.copy(alpha = 0.35f),
                    1f to EcoPrimary,
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 24.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EcoWhite),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ecoruta_logo_circular),
                    contentDescription = stringResource(R.string.logo_ecoruta_description),
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_name), style = EcoTextStyles.ButtonText, color = EcoWhite)
            Spacer(Modifier.weight(1f))
            HeaderIconButton(
                iconRes = R.drawable.ic_help,
                contentDescription = stringResource(R.string.home_help),
                onClick = onHelpClick,
            )
            Spacer(Modifier.width(8.dp))
            HeaderIconButton(
                iconRes = R.drawable.ic_bell,
                contentDescription = stringResource(R.string.home_notifications),
                onClick = onNotificationsClick,
            )
        }
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = if (onLogoutClick == null) 20.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = EcoTextStyles.Title, color = EcoWhite)
            Text(stringResource(R.string.home_welcome_message), style = EcoTextStyles.Subtitle, color = EcoWhite)
        }
        if (onLogoutClick != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(topStart = 25.dp, bottomStart = 25.dp))
                        .background(EcoAccent)
                        .clickable(role = Role.Button, onClick = onLogoutClick)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.home_logout), style = EcoTextStyles.LinkText, color = EcoBlack)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(EcoWhite.copy(alpha = 0.1f))
            .border(1.dp, EcoWhite.copy(alpha = 0.2f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ServicesSection(onServiceClick: (PublicService) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.home_services_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.home_services_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PublicService.entries.chunked(2).forEach { rowServices ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowServices.forEach { service ->
                        EcoServiceCard(
                            title = stringResource(service.titleRes),
                            description = stringResource(service.descriptionRes),
                            iconRes = service.iconRes,
                            iconSize = if (service == PublicService.REPORTS) 32.dp else 20.dp,
                            onClick = { onServiceClick(service) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 170.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Inicio Público (Visitante)", widthDp = 393, heightDp = 900)
@Composable
private fun HomeGuestPreview() {
    EcoRutaTheme {
        HomeScreen(
            actions = HomeActions(),
            isSignedIn = false,
        )
    }
}

@Preview(name = "Inicio Ciudadano (Con Sesión)", widthDp = 393, heightDp = 900)
@Composable
private fun HomeSignedInPreview() {
    EcoRutaTheme {
        HomeScreen(
            actions = HomeActions(),
            isSignedIn = true,
            userName = "Camila",
        )
    }
}
