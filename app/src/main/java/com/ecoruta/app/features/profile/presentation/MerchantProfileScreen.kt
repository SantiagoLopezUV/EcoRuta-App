package com.ecoruta.app.features.profile.presentation

// Pantallas de perfil del comercio aliado y de cambio de perfil desde el módulo comercio
// Muestran los datos del establecimiento, el estado del perfil y las acciones de la cuenta

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.profile.presentation.components.ChangeProfileActions
import com.ecoruta.app.features.profile.presentation.components.ChangeProfileContent
import com.ecoruta.app.features.profile.presentation.components.ProfilePhotoSheet

// Datos del establecimiento que se muestran en el perfil del comercio
data class MerchantProfileDetails(
    val category: String,
    val maskedAddress: String,
    val maskedNit: String,
    val isApproved: Boolean = true,
)

// Estado del perfil del comercio, vacío por defecto mientras no haya datos del servidor
data class MerchantProfileUiState(
    val profile: UserProfile? = null,
    val details: MerchantProfileDetails? = null,
    val localPhoto: Uri? = null,
    val isLoading: Boolean = false,
    val isSigningOut: Boolean = false,
    val hasError: Boolean = false,
    val isOffline: Boolean = false,
)

// Acciones disponibles en el perfil del comercio
data class MerchantProfileActions(
    val onPhotoSelected: (Uri) -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onCameraUnavailable: () -> Unit = {},
    val onChangeProfile: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (MerchantTab) -> Unit = {},
)

// Perfil del comercio que elige entre sin conexión, error, vacío o la tarjeta con los datos y acciones
@Composable
fun MerchantProfileScreen(state: MerchantProfileUiState, actions: MerchantProfileActions) {
    when {
        state.isOffline -> OfflineScreen(onRetry = actions.onRetry)
        state.hasError -> EcoSystemStateScreen(
            title = stringResource(R.string.merchant_profile_title),
            subtitle = stringResource(R.string.role_store),
            illustrationRes = R.drawable.ill_state_offline,
            heading = stringResource(R.string.merchant_profile_error_heading),
            body = stringResource(R.string.profile_load_error),
            actionText = stringResource(R.string.action_retry),
            onAction = actions.onRetry,
        )
        !state.isLoading && state.profile == null -> EcoSystemStateScreen(
            title = stringResource(R.string.merchant_profile_title),
            subtitle = stringResource(R.string.role_store),
            illustrationRes = R.drawable.ic_nav_ticket,
            heading = stringResource(R.string.merchant_profile_empty_heading),
            body = stringResource(R.string.merchant_profile_empty_body),
            actionText = stringResource(R.string.action_retry),
            onAction = actions.onRetry,
        )
        else -> MerchantProfileContent(state, actions)
    }
}

@Composable
private fun MerchantProfileContent(state: MerchantProfileUiState, actions: MerchantProfileActions) {
    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile
    if (showPhotoSheet && profile != null) {
        ProfilePhotoSheet(
            hasPhoto = state.localPhoto != null || profile.photoUrl != null,
            onPhotoSelected = actions.onPhotoSelected,
            onRemovePhoto = actions.onRemovePhoto,
            onCameraUnavailable = actions.onCameraUnavailable,
            onDismiss = { showPhotoSheet = false },
        )
    }
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.PROFILE,
        onTabClick = actions.onTabClick,
        showBrandHeader = false,
        bottomActions = {
            AccountButton(
                text = stringResource(R.string.profile_change_profile),
                iconRes = R.drawable.ic_nav_user,
                background = EcoAccent,
                content = EcoBlack,
                onClick = actions.onChangeProfile,
            )
            AccountButton(
                text = stringResource(if (state.isSigningOut) R.string.profile_signing_out else R.string.home_logout),
                iconRes = R.drawable.ic_profile_logout,
                background = EcoFieldBackground,
                content = EcoPrimary,
                onClick = { if (!state.isSigningOut) actions.onSignOut() },
            )
        },
    ) {
        Text(stringResource(R.string.merchant_profile_title), style = EcoTextStyles.Title, color = EcoTitle)
        if (profile == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
        } else {
            // La marca va dentro de la tarjeta y el nombre comercial queda al pie
            UserCard(
                profile = profile,
                localPhoto = state.localPhoto,
                isUpdatingPhoto = false,
                onChangePhoto = { showPhotoSheet = true },
                footer = profile.fullName,
                headline = stringResource(R.string.app_name),
            )
            state.details?.let { MerchantDetails(it) }
        }
    }
}

// Datos del comercio y el estado de aprobación del perfil
@Composable
private fun MerchantDetails(details: MerchantProfileDetails) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileDetailRow(R.drawable.ic_tag, stringResource(R.string.merchant_profile_category), details.category)
        ProfileDetailRow(R.drawable.ic_map_pin, stringResource(R.string.merchant_profile_address), details.maskedAddress)
        ProfileDetailRow(R.drawable.ic_id_card, stringResource(R.string.store_id_nit), details.maskedNit)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(EcoDimens.InputIconSize))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.recycler_profile_status), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            Text(
                stringResource(if (details.isApproved) R.string.recycler_profile_approved else R.string.store_review_status),
                style = EcoTextStyles.ButtonText,
                color = if (details.isApproved) EcoGreenSecondary else EcoSubtitle,
            )
        }
    }
}

// Cambio de perfil dentro del módulo comercio, con la barra inferior del comercio
@Composable
fun MerchantChangeProfileScreen(
    profile: UserProfile,
    actions: ChangeProfileActions,
    onTabClick: (MerchantTab) -> Unit,
) {
    EcoRoleTabScreenLayout(
        items = MerchantTab.entries,
        selectedTab = MerchantTab.PROFILE,
        onTabClick = onTabClick,
        onBack = actions.onBack,
        compactHeader = true,
        contentSpacing = 20.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.change_profile_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.change_profile_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        ChangeProfileContent(profile = profile, actions = actions)
    }
}

// Perfil ilustrativo del comercio usado solo en las vistas previas
internal val PreviewMerchantProfile = UserProfile(
    fullName = "Tienda Semilla de Ejemplo",
    role = UserRole.COMERCIO,
    city = "Tuluá",
    email = "comercio@ecoruta.co",
    approvedRoles = setOf(UserRole.COMERCIO, UserRole.CIUDADANO, UserRole.RECICLADOR),
)

private val PreviewMerchantDetails = MerchantProfileDetails(
    category = "Tienda de alimentos",
    maskedAddress = "Calle de Ejemplo # ••-••, Tuluá",
    maskedNit = "•••••••••45",
)

@Preview(name = "Perfil del comercio", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantProfilePreview() {
    EcoRutaTheme {
        MerchantProfileScreen(
            state = MerchantProfileUiState(profile = PreviewMerchantProfile, details = PreviewMerchantDetails),
            actions = MerchantProfileActions(),
        )
    }
}

@Preview(name = "Perfil del comercio · Cerrando sesión", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantProfileSigningOutPreview() {
    EcoRutaTheme {
        MerchantProfileScreen(
            state = MerchantProfileUiState(profile = PreviewMerchantProfile, details = PreviewMerchantDetails, isSigningOut = true),
            actions = MerchantProfileActions(),
        )
    }
}

@Preview(name = "Perfil del comercio · Cargando", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantProfileLoadingPreview() {
    EcoRutaTheme { MerchantProfileScreen(state = MerchantProfileUiState(isLoading = true), actions = MerchantProfileActions()) }
}

@Preview(name = "Perfil del comercio · Vacío", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantProfileEmptyPreview() {
    EcoRutaTheme { MerchantProfileScreen(state = MerchantProfileUiState(), actions = MerchantProfileActions()) }
}

@Preview(name = "Perfil del comercio · Error", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantProfileErrorPreview() {
    EcoRutaTheme { MerchantProfileScreen(state = MerchantProfileUiState(hasError = true), actions = MerchantProfileActions()) }
}

@Preview(name = "Perfil del comercio · Sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantProfileOfflinePreview() {
    EcoRutaTheme { MerchantProfileScreen(state = MerchantProfileUiState(isOffline = true), actions = MerchantProfileActions()) }
}

@Preview(name = "Cambiar perfil · Comercio", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantChangeProfilePreview() {
    EcoRutaTheme { MerchantChangeProfileScreen(profile = PreviewMerchantProfile, actions = ChangeProfileActions(), onTabClick = {}) }
}

@Preview(name = "Cambiar perfil · Comercio con solicitud disponible", widthDp = 393, heightDp = 852)
@Composable
private fun MerchantChangeProfileWithRequestPreview() {
    EcoRutaTheme {
        MerchantChangeProfileScreen(
            profile = PreviewMerchantProfile.copy(approvedRoles = setOf(UserRole.COMERCIO, UserRole.CIUDADANO)),
            actions = ChangeProfileActions(),
            onTabClick = {},
        )
    }
}
