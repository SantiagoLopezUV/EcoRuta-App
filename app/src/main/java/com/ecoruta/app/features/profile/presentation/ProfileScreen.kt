package com.ecoruta.app.features.profile.presentation

// Vista composable de perfil adaptable según estado de autenticación
// Con sesión: datos y balance. Sin sesión: banner de bienvenida y candados en privadas

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.components.EcoBottomBar
import com.ecoruta.app.core.ui.components.EcoAccountOption
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTopBar
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.profile.presentation.components.ProfilePhotoSheet
import com.ecoruta.app.features.reports.presentation.create.rememberPhotoThumbnail
import java.text.NumberFormat
import java.util.Locale

// Color verde claro para el enlace de cambio de foto sobre fondo primario
private val PhotoLinkColor = Color(0xFF99F2BF)

// Acciones de la pantalla de perfil con sesión activa
data class ProfileActions(
    val onBack: () -> Unit = {},
    val onPhotoSelected: (Uri) -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onCameraUnavailable: () -> Unit = {},
    val onMyReports: () -> Unit = {},
    val onMyDebrisRequests: () -> Unit = {},
    val onMyCoupons: () -> Unit = {},
    val onChangeProfile: () -> Unit = {},
    val onChangePassword: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Diseño común con cabecera, título de perfil, contenido desplazable y barra inferior
@Composable
private fun ProfileLayout(
    onBack: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EcoTopBar(onBackClick = onBack)
            Text(stringResource(R.string.my_profile), style = EcoTextStyles.Titulo, color = EcoTitle)
            content()
        }
        EcoBottomBar(
            items = CitizenTab.entries,
            selectedItem = CitizenTab.PERFIL,
            onItemClick = onTabClick,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

// Pantalla principal de perfil para el ciudadano autenticado
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    isSigningOut: Boolean,
    actions: ProfileActions,
    isUpdatingPhoto: Boolean = false,
) {
    var showPhotoSheet by rememberSaveable { mutableStateOf(false) }
    if (showPhotoSheet && state is ProfileUiState.Loaded) {
        ProfilePhotoSheet(
            hasPhoto = state.hasPhoto,
            onPhotoSelected = actions.onPhotoSelected,
            onRemovePhoto = actions.onRemovePhoto,
            onCameraUnavailable = actions.onCameraUnavailable,
            onDismiss = { showPhotoSheet = false },
        )
    }
    ProfileLayout(onBack = actions.onBack, onTabClick = actions.onTabClick) {
        when (state) {
            ProfileUiState.Loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }

            ProfileUiState.Error -> Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EcoErrorMessage(text = stringResource(R.string.profile_load_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }

            is ProfileUiState.Loaded -> {
                UserCard(
                    profile = state.profile,
                    localPhoto = state.localPhoto,
                    isUpdatingPhoto = isUpdatingPhoto,
                    onChangePhoto = { showPhotoSheet = true },
                )
                EcoPointsCard(points = state.profile.ecoPoints)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EcoAccountOption(stringResource(R.string.profile_my_reports), R.drawable.ic_profile_clipboard, actions.onMyReports)
                    EcoAccountOption(stringResource(R.string.profile_my_debris_requests), R.drawable.ic_profile_clipboard, actions.onMyDebrisRequests)
                    EcoAccountOption(stringResource(R.string.profile_my_coupons), R.drawable.ic_profile_ticket, actions.onMyCoupons)
                    EcoAccountOption(stringResource(R.string.profile_change_profile), R.drawable.ic_profile_users, actions.onChangeProfile)
                    EcoAccountOption(stringResource(R.string.change_password_title), R.drawable.ic_profile_lock, actions.onChangePassword)
                    EcoAccountOption(
                        text = stringResource(if (isSigningOut) R.string.profile_signing_out else R.string.home_logout),
                        iconRes = R.drawable.ic_profile_logout,
                        onClick = { if (!isSigningOut) actions.onSignOut() },
                    )
                }
            }
        }
    }
}

// Tarjeta con los datos principales del usuario y su foto de perfil
@Composable
private fun UserCard(profile: UserProfile, localPhoto: Uri?, isUpdatingPhoto: Boolean, onChangePhoto: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
            .background(EcoPrimary)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(50.dp)) {
                val photoDescription = stringResource(R.string.profile_photo_description, profile.fullName)
                val thumbnail = localPhoto?.let { rememberPhotoThumbnail(it, maxSizePx = 160) }
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = photoDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_avatar_default),
                        contentDescription = photoDescription,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_camera_badge),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = 32.dp, y = 32.dp)
                        .size(18.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.fullName, style = EcoTextStyles.Boton, color = EcoWhite)
                Text(
                    stringResource(R.string.profile_role_city, stringResource(profile.role.roleNameRes()), profile.city),
                    style = EcoTextStyles.Subtitulo,
                    color = EcoWhite,
                )
                Text(
                    text = stringResource(if (isUpdatingPhoto) R.string.profile_photo_saving else R.string.profile_change_photo),
                    style = EcoTextStyles.EtiquetaCampo.copy(letterSpacing = 0.sp),
                    color = PhotoLinkColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(role = Role.Button, enabled = !isUpdatingPhoto, onClick = onChangePhoto),
                )
            }
        }
        Text(profile.email, style = EcoTextStyles.Subtitulo, color = EcoWhite)
    }
}

// Tarjeta informativa con el saldo actual de EcoPuntos del usuario
@Composable
private fun EcoPointsCard(points: Int?, modifier: Modifier = Modifier, emptyText: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EcoGreenSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ecopoints_leaf),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(if (points != null) R.string.profile_ecopoints_available else R.string.profile_ecopoints),
                style = EcoTextStyles.EtiquetaCampo,
                color = EcoSubtitle,
            )
            if (points != null) {
                Text(formatPoints(points), style = EcoTextStyles.Numeros, color = EcoPrimary)
            } else if (emptyText != null) {
                Text(emptyText, style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
            }
        }
    }
}

// Formatea el puntaje con separador de miles colombiano
private fun formatPoints(points: Int): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(points)

private fun UserRole.roleNameRes(): Int = when (this) {
    UserRole.CIUDADANO -> R.string.role_citizen
    UserRole.RECICLADOR -> R.string.role_recycler
    UserRole.CHOFER -> R.string.role_driver
    UserRole.COMERCIO -> R.string.role_store
}

// Acciones de la pantalla de perfil sin sesión activa
data class GuestProfileActions(
    val onBack: () -> Unit = {},
    val onLogin: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Pantalla de perfil para visitantes con opciones protegidas que redirigen al inicio de sesión
@Composable
fun GuestProfileScreen(actions: GuestProfileActions) {
    ProfileLayout(onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                .background(EcoPrimary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EcoWhite.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_user_white),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.profile_guest_title), style = EcoTextStyles.Boton, color = EcoWhite)
                    Text(stringResource(R.string.profile_guest_subtitle), style = EcoTextStyles.Subtitulo, color = EcoWhite)
                }
            }
            Text(stringResource(R.string.profile_guest_description), style = EcoTextStyles.Subtitulo, color = EcoWhite)
            EcoPrimaryButton(text = stringResource(R.string.home_login), onClick = actions.onLogin)
        }
        EcoPointsCard(points = null, emptyText = stringResource(R.string.profile_guest_ecopoints))
        Text(
            stringResource(R.string.profile_requires_login),
            style = EcoTextStyles.EtiquetaCampo,
            color = EcoTitle,
            modifier = Modifier.padding(top = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EcoAccountOption(stringResource(R.string.service_reports_title), R.drawable.ic_profile_clipboard, actions.onLogin, locked = true)
            EcoAccountOption(stringResource(R.string.profile_debris_requests), R.drawable.ic_profile_clipboard, actions.onLogin, locked = true)
            EcoAccountOption(stringResource(R.string.tab_cupones), R.drawable.ic_profile_ticket, actions.onLogin, locked = true)
        }
    }
}

@Preview(name = "Mi perfil", widthDp = 393, heightDp = 900)
@Composable
private fun ProfilePreview() {
    EcoRutaTheme {
        ProfileScreen(
            state = ProfileUiState.Loaded(
                UserProfile(fullName = "Santiago", role = UserRole.CIUDADANO, city = "Tuluá", email = "santiago@gmail.com", ecoPoints = 1250),
            ),
            isSigningOut = false,
            actions = ProfileActions(),
        )
    }
}

@Preview(name = "Mi perfil sin sesión", widthDp = 393, heightDp = 852)
@Composable
private fun GuestProfilePreview() {
    EcoRutaTheme { GuestProfileScreen(GuestProfileActions()) }
}
