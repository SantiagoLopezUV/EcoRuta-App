package com.ecoruta.app.features.profile.presentation

// Pantallas de perfil del reciclador y de cambio de perfil desde el módulo reciclador
// Muestran sus datos ilustrativos, el estado del perfil y los roles activos de la cuenta

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
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

// Datos ilustrativos que se muestran en el perfil del reciclador
data class RecyclerProfileDetails(
    val maskedPhone: String,
    val modality: RecyclerModality,
    val isApproved: Boolean = true,
)

// Acciones disponibles en el perfil del reciclador
data class RecyclerProfileActions(
    val onBack: () -> Unit = {},
    val onPhotoSelected: (Uri) -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onCameraUnavailable: () -> Unit = {},
    val onChangeProfile: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onTabClick: (RecyclerTab) -> Unit = {},
)

// Perfil del reciclador con su tarjeta de identidad, datos de contacto, estado y acciones de cuenta
@Composable
fun RecyclerProfileScreen(
    profile: UserProfile,
    details: RecyclerProfileDetails,
    actions: RecyclerProfileActions,
    localPhoto: Uri? = null,
    isSigningOut: Boolean = false,
) {
    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }
    if (showPhotoSheet) {
        ProfilePhotoSheet(
            hasPhoto = localPhoto != null || profile.photoUrl != null,
            onPhotoSelected = actions.onPhotoSelected,
            onRemovePhoto = actions.onRemovePhoto,
            onCameraUnavailable = actions.onCameraUnavailable,
            onDismiss = { showPhotoSheet = false },
        )
    }
    EcoRoleTabScreenLayout(
        items = RecyclerTab.entries,
        selectedTab = RecyclerTab.PROFILE,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
        bottomActions = {
            AccountButton(
                text = stringResource(R.string.profile_change_profile),
                iconRes = R.drawable.ic_nav_user,
                background = EcoAccent,
                content = EcoBlack,
                onClick = actions.onChangeProfile,
            )
            AccountButton(
                text = stringResource(if (isSigningOut) R.string.profile_signing_out else R.string.home_logout),
                iconRes = R.drawable.ic_profile_logout,
                background = EcoFieldBackground,
                content = EcoPrimary,
                onClick = { if (!isSigningOut) actions.onSignOut() },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycler_profile_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.recycler_profile_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        UserCard(
            profile = profile,
            localPhoto = localPhoto,
            isUpdatingPhoto = false,
            onChangePhoto = { showPhotoSheet = true },
            footer = stringResource(R.string.recycler_profile_illustrative),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileDetailRow(R.drawable.ic_phone, stringResource(R.string.registration_phone_label), details.maskedPhone)
            ProfileDetailRow(
                R.drawable.ic_nav_user,
                stringResource(R.string.recycler_profile_modality),
                stringResource(
                    if (details.modality == RecyclerModality.INDEPENDENT) R.string.registration_type_independent else R.string.registration_type_company_recycler,
                ),
            )
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
                Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.recycler_profile_status), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                Text(
                    stringResource(if (details.isApproved) R.string.recycler_profile_approved else R.string.recycler_sent_status),
                    style = EcoTextStyles.ButtonText,
                    color = EcoGreenSecondary,
                )
            }
        }
    }
}

// Dato del perfil con icono, etiqueta y valor
@Composable
internal fun ProfileDetailRow(@DrawableRes iconRes: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(iconRes), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            Text(value, style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
    }
}

// Botón de acción de cuenta con icono a la izquierda del texto
@Composable
internal fun AccountButton(text: String, @DrawableRes iconRes: Int, background: Color, content: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Text(text, style = if (background == EcoAccent) EcoTextStyles.ButtonText else EcoTextStyles.LinkText, color = content)
    }
}

// Cambio de perfil dentro del módulo reciclador, con la barra inferior del reciclador
@Composable
fun RecyclerChangeProfileScreen(
    profile: UserProfile,
    actions: ChangeProfileActions,
    onTabClick: (RecyclerTab) -> Unit,
) {
    EcoRoleTabScreenLayout(
        items = RecyclerTab.entries,
        selectedTab = RecyclerTab.PROFILE,
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

// Perfil ilustrativo del reciclador usado en el modo diseño y en las previsualizaciones
val SampleRecyclerProfile = UserProfile(
    fullName = "Reciclador de ejemplo",
    role = UserRole.RECICLADOR,
    city = "Tuluá",
    email = "reciclador@ecoruta.co",
    approvedRoles = setOf(UserRole.RECICLADOR, UserRole.CIUDADANO),
)

// Datos de contacto y modalidad ilustrativos del reciclador
val SampleRecyclerDetails = RecyclerProfileDetails(
    maskedPhone = "3•• ••• ••42",
    modality = RecyclerModality.INDEPENDENT,
)

@Preview(name = "Perfil del reciclador", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerProfilePreview() {
    EcoRutaTheme {
        RecyclerProfileScreen(profile = SampleRecyclerProfile, details = SampleRecyclerDetails, actions = RecyclerProfileActions())
    }
}

@Preview(name = "Cambiar perfil · Reciclador", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerChangeProfilePreview() {
    EcoRutaTheme {
        RecyclerChangeProfileScreen(profile = SampleRecyclerProfile, actions = ChangeProfileActions(), onTabClick = {})
    }
}
