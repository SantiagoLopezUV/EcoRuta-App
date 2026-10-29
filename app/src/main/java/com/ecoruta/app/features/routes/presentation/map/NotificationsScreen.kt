package com.ecoruta.app.features.routes.presentation.map

// Pantallas de notificaciones del ciudadano con y sin sesión iniciada

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import java.text.SimpleDateFormat
import java.util.Date

// Pantalla de listado de notificaciones de proximidad, escombros y reportes
data class NotificationsActions(
    val onBack: () -> Unit = {},
    val onNotificationClick: (AppNotification) -> Unit = {},
    val onGoHome: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun NotificationsScreen(state: NotificationsUiState, actions: NotificationsActions) {
    val isEmpty = state is NotificationsUiState.Loaded && state.notifications.isEmpty()
    val bottomActions: (@Composable ColumnScope.() -> Unit)? = if (isEmpty) {
        { EcoPrimaryButton(text = stringResource(R.string.report_back_home), onClick = actions.onGoHome) }
    } else {
        null
    }
    EcoTabScreenLayout(
        selectedTab = CitizenTab.HOME,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = bottomActions,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.notifications_title), style = EcoTextStyles.Title, color = EcoTitle)
            if (!isEmpty) Text(stringResource(R.string.notifications_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when (state) {
            NotificationsUiState.Loading -> CenteredLoading()
            NotificationsUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.notifications_load_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            is NotificationsUiState.Loaded ->
                if (state.notifications.isEmpty()) {
                    BellMessage(
                        title = stringResource(R.string.notifications_empty_title),
                        body = stringResource(R.string.notifications_empty_body),
                        highlighted = false,
                    )
                } else {
                    if (state.isSample) EcoInfoHint(text = stringResource(R.string.notifications_sample_hint))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.notifications.forEach { notification ->
                            NotificationCard(notification = notification, onClick = { actions.onNotificationClick(notification) })
                        }
                    }
                }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoFieldBackground)
            .border(1.dp, if (notification.read) EcoFieldBackground else EcoBorder, shape)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.notifications_open), onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(notification.kind.iconRes()),
                    contentDescription = null,
                    tint = EcoWhite,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(notification.label, style = EcoTextStyles.FieldLabel, color = EcoTitle, modifier = Modifier.weight(1f))
            if (notification.read) {
                EcoStatusChip(stringResource(R.string.notifications_read), EcoSubtitle, EcoWhite)
            } else {
                EcoStatusChip(stringResource(R.string.notifications_unread), EcoAccent, EcoBlack)
            }
        }
        Text(notification.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
        Text(notification.body, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        Text(formatNotificationDate(notification.createdAtMillis), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
    }
}

private fun NotificationKind.iconRes(): Int = when (this) {
    NotificationKind.PROXIMIDAD -> R.drawable.ic_bell
    NotificationKind.SOLICITUD_ESCOMBROS -> R.drawable.ic_checkbox_check
    NotificationKind.REPORTE -> R.drawable.ic_profile_clipboard
}

private fun formatNotificationDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy '·' h:mm a", SpanishColombia).format(Date(millis))

@Composable
private fun BellMessage(title: String, body: String, highlighted: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(EcoFieldBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (highlighted) EcoPrimary else EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = if (highlighted) EcoWhite else EcoTitle, modifier = Modifier.size(28.dp))
        }
        Text(
            title,
            style = if (highlighted) EcoTextStyles.Title else EcoTextStyles.ButtonText,
            color = EcoTitle,
            textAlign = TextAlign.Center,
        )
        Text(body, style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center)
    }
}

@Composable
fun GuestNotificationsScreen(onBack: () -> Unit, onLogin: () -> Unit, onTabClick: (CitizenTab) -> Unit) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.HOME,
        onBack = onBack,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.home_login), onClick = onLogin, trailingIconRes = R.drawable.ic_arrow_right)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.notifications_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.notifications_guest_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        BellMessage(
            title = stringResource(R.string.notifications_guest_title),
            body = stringResource(R.string.notifications_guest_body),
            highlighted = true,
        )
    }
}

@Preview(name = "Notificaciones", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsPreview() {
    EcoRutaTheme {
        val notifications = listOf(
            AppNotification(
                id = "not-1",
                kind = NotificationKind.PROXIMIDAD,
                label = "Radar de proximidad",
                title = "El camión recolector se acerca a Torobajo",
                body = "Está a menos de 250 m de tu ubicación configurada.",
                read = false,
                createdAtMillis = 1_744_900_000_000L,
            ),
            AppNotification(
                id = "not-2",
                kind = NotificationKind.REPORTE,
                label = "Reporte ciudadano",
                title = "Tu reporte fue aprobado",
                body = "El equipo operativo programó la atención en tu sector.",
                read = true,
                createdAtMillis = 1_744_800_000_000L,
            ),
        )
        NotificationsScreen(
            state = NotificationsUiState.Loaded(notifications = notifications, isSample = false),
            actions = NotificationsActions(),
        )
    }
}

@Preview(name = "Notificaciones sin sesión", widthDp = 393, heightDp = 852)
@Composable
private fun GuestNotificationsPreview() {
    EcoRutaTheme {
        GuestNotificationsScreen(onBack = {}, onLogin = {}, onTabClick = {})
    }
}
