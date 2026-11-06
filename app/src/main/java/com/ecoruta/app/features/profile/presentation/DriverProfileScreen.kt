package com.ecoruta.app.features.profile.presentation

// Pantalla de perfil del chofer dentro de su módulo de trabajo
// Muestra su tarjeta de identidad, la empresa, el aviso de protección de datos y las acciones de cuenta

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.profile.presentation.components.EditDataButton
import com.ecoruta.app.features.profile.presentation.components.ProfilePhotoSheet

// Datos ilustrativos que se muestran en el perfil del chofer
data class DriverProfileDetails(
    val company: String,
)

// Acciones disponibles en el perfil del chofer
data class DriverProfileActions(
    val onPhotoSelected: (Uri) -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onCameraUnavailable: () -> Unit = {},
    val onChangePassword: () -> Unit = {},
    val onEditProfile: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Perfil del chofer con su tarjeta, la empresa a la que pertenece y las acciones de cuenta
@Composable
fun DriverProfileScreen(
    profile: UserProfile,
    details: DriverProfileDetails,
    actions: DriverProfileActions,
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
        items = DriverTab.entries,
        selectedTab = DriverTab.PROFILE,
        onTabClick = actions.onTabClick,
        bottomActions = {
            AccountButton(
                text = stringResource(R.string.change_password_title),
                iconRes = R.drawable.ic_profile_lock,
                background = EcoAccent,
                content = EcoBlack,
                onClick = actions.onChangePassword,
            )
            EditDataButton(onClick = actions.onEditProfile)
            AccountButton(
                text = stringResource(if (isSigningOut) R.string.profile_signing_out else R.string.home_logout),
                iconRes = R.drawable.ic_profile_logout,
                background = EcoFieldBackground,
                content = EcoPrimary,
                onClick = { if (!isSigningOut) actions.onSignOut() },
            )
            Text(
                stringResource(R.string.driver_profile_sign_out_hint),
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.my_profile), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.driver_profile_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        UserCard(
            profile = profile,
            localPhoto = localPhoto,
            isUpdatingPhoto = false,
            onChangePhoto = { showPhotoSheet = true },
            footer = stringResource(R.string.driver_profile_illustrative),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
        ) {
            ProfileDetailRow(R.drawable.ic_building, stringResource(R.string.driver_profile_company), details.company)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.driver_profile_privacy_title), style = EcoTextStyles.ButtonText, color = EcoPrimary)
            Text(stringResource(R.string.driver_profile_privacy_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Text(stringResource(R.string.driver_profile_privacy_law), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
    }
}

// Perfil ilustrativo del chofer usado en el modo diseño y en las previsualizaciones
val SampleDriverProfile = UserProfile(
    fullName = "Julián Restrepo",
    role = UserRole.CHOFER,
    city = "Tuluá",
    email = "chofer@ecoruta.co",
)

// Empresa ilustrativa a la que pertenece el chofer
val SampleDriverDetails = DriverProfileDetails(company = "Aseo Verde Tuluá S.A.S.")

@Preview(name = "Perfil del chofer", widthDp = 390, heightDp = 844)
@Composable
private fun DriverProfilePreview() {
    EcoRutaTheme {
        DriverProfileScreen(profile = SampleDriverProfile, details = SampleDriverDetails, actions = DriverProfileActions())
    }
}
