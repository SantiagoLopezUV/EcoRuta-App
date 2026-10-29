package com.ecoruta.app.features.profile.presentation.components

// Pantalla para seleccionar y cambiar el perfil activo
// Muestra los perfiles habilitados y la opción de postular a roles no activos

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.profile.presentation.ProfileUiState
import com.ecoruta.app.features.profile.presentation.UserProfile
import com.ecoruta.app.features.profile.presentation.UserRole

// Acciones y componentes para cambiar el perfil activo del usuario
data class ChangeProfileActions(
    val onBack: () -> Unit = {},
    val onSwitchRole: (UserRole) -> Unit = {},
    val onRequestRole: (UserRole) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

private val requestableRoles = listOf(UserRole.COMERCIO, UserRole.RECICLADOR)

@Composable
fun ChangeProfileScreen(state: ProfileUiState, actions: ChangeProfileActions) {
    EcoTabScreenLayout(selectedTab = CitizenTab.PROFILE, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.change_profile_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.change_profile_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when (state) {
            ProfileUiState.Loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
            ProfileUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.profile_load_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            is ProfileUiState.Loaded -> ChangeProfileContent(state.profile, actions)
        }
    }
}

@Composable
private fun ChangeProfileContent(profile: UserProfile, actions: ChangeProfileActions) {
    val roles = listOf(profile.role) + UserRole.entries.filter { it != profile.role && it in profile.approvedRoles }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        roles.forEach { role ->
            RoleCard(role = role, isCurrent = role == profile.role, onClick = { actions.onSwitchRole(role) })
        }
    }
    val available = requestableRoles.filter { it !in profile.approvedRoles }
    if (available.isNotEmpty()) {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.change_profile_request_title), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            available.forEach { role ->
                RequestRow(role = role, pending = role in profile.pendingRoles, onClick = { actions.onRequestRole(role) })
            }
        }
    }
}

@Composable
private fun RoleCard(role: UserRole, isCurrent: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val info = role.info()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(1.5.dp, if (isCurrent) EcoGreenSecondary else EcoFieldBackground, shape)
            .clickable(enabled = !isCurrent, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCurrent) EcoGreenSecondary else EcoFieldBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(info.iconRes),
                    contentDescription = null,
                    tint = if (isCurrent) EcoWhite else EcoGreenSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(info.nameRes), style = EcoTextStyles.ButtonText, color = if (isCurrent) EcoTitle else EcoSubtitle)
                Text(
                    stringResource(if (isCurrent) R.string.change_profile_current else R.string.change_profile_tap_to_switch),
                    style = EcoTextStyles.FieldLabel,
                    color = if (isCurrent) EcoPrimary else EcoSubtitle,
                )
            }
            Icon(
                painterResource(if (isCurrent) R.drawable.ic_circle_check else R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = if (isCurrent) EcoGreenSecondary else EcoSubtitle,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(stringResource(info.descriptionRes), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

@Composable
private fun RequestRow(role: UserRole, pending: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(role.info().iconRes), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
        Text(
            stringResource(if (role == UserRole.COMERCIO) R.string.change_profile_register_store else R.string.change_profile_apply_recycler),
            style = EcoTextStyles.LinkText,
            color = EcoPrimary,
            modifier = Modifier.weight(1f),
        )
        if (pending) Text(stringResource(R.string.change_profile_in_review), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
    }
}

private data class RoleInfo(@StringRes val nameRes: Int, @StringRes val descriptionRes: Int, @DrawableRes val iconRes: Int)

private fun UserRole.info(): RoleInfo = when (this) {
    UserRole.CIUDADANO -> RoleInfo(R.string.role_citizen, R.string.change_profile_citizen_description, R.drawable.ic_leaf)
    UserRole.COMERCIO -> RoleInfo(R.string.change_profile_store_name, R.string.change_profile_store_description, R.drawable.ic_store)
    UserRole.RECICLADOR -> RoleInfo(R.string.role_recycler, R.string.change_profile_recycler_description, R.drawable.ic_recycle)
    UserRole.CHOFER -> RoleInfo(R.string.role_driver, R.string.change_profile_driver_description, R.drawable.ic_truck)
}

@Preview(name = "Cambiar Perfil", widthDp = 393, heightDp = 852)
@Composable
private fun ChangeProfilePreview() {
    EcoRutaTheme {
        ChangeProfileScreen(
            state = ProfileUiState.Loaded(
                profile = UserProfile(
                    fullName = "Camila Andrea Gómez",
                    email = "camila.gomez@ecoruta.co",
                    city = "Pasto",
                    role = UserRole.CIUDADANO,
                    approvedRoles = setOf(UserRole.CIUDADANO),
                ),
            ),
            actions = ChangeProfileActions(),
        )
    }
}
