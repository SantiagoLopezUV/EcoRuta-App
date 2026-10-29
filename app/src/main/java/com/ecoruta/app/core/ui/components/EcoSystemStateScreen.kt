package com.ecoruta.app.core.ui.components

// Pantallas transversales para avisos del sistema, permisos y estados de conexión
// Incluye vistas de solicitud de permisos, falta de internet y acceso exclusivo en web

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Plantilla base para pantallas de estado del sistema con ilustración central y acción principal
@Composable
fun EcoSystemStateScreen(
    title: String,
    subtitle: String,
    @DrawableRes illustrationRes: Int,
    heading: String,
    body: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onBack: (() -> Unit)? = null,
) {
    if (onBack != null) BackHandler(onBack = onBack)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            EcoTopBar()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = EcoTextStyles.Title, color = EcoTitle)
                Text(subtitle, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoFieldBackground)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Image(painterResource(illustrationRes), contentDescription = null, modifier = Modifier.size(104.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(heading, style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(body, style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        EcoPrimaryButton(text = actionText, onClick = onAction, isLoading = isLoading, modifier = Modifier.padding(top = 16.dp))
    }
}

// Tipos de permisos del dispositivo que la aplicación explica antes de solicitarlos al sistema
enum class EcoPermissionKind(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val headingRes: Int,
    @StringRes val bodyRes: Int,
    @StringRes val actionRes: Int,
    @DrawableRes val illustrationRes: Int,
) {
    UBICACION(
        R.string.permission_location_title, R.string.permission_location_subtitle, R.string.permission_location_heading,
        R.string.permission_location_body, R.string.permission_location_action, R.drawable.ill_state_location,
    ),
    CAMARA(
        R.string.permission_camera_title, R.string.permission_camera_subtitle, R.string.permission_camera_heading,
        R.string.permission_camera_body, R.string.permission_camera_action, R.drawable.ill_state_camera,
    ),
    NOTIFICACIONES(
        R.string.permission_notifications_title, R.string.permission_notifications_subtitle, R.string.permission_notifications_heading,
        R.string.permission_notifications_body, R.string.permission_notifications_action, R.drawable.ill_state_notifications,
    ),
}

// Pantalla explicativa previa a lanzar la solicitud de un permiso del sistema
@Composable
fun EcoPermissionScreen(kind: EcoPermissionKind, onAllow: () -> Unit, onBack: () -> Unit) {
    EcoSystemStateScreen(
        title = stringResource(kind.titleRes),
        subtitle = stringResource(kind.subtitleRes),
        illustrationRes = kind.illustrationRes,
        heading = stringResource(kind.headingRes),
        body = stringResource(kind.bodyRes),
        actionText = stringResource(kind.actionRes),
        onAction = onAllow,
        onBack = onBack,
    )
}

// Pantalla informativa mostrada cuando el dispositivo no cuenta con conexión a internet
@Composable
fun OfflineScreen(onRetry: () -> Unit) {
    EcoSystemStateScreen(
        title = stringResource(R.string.offline_title),
        subtitle = stringResource(R.string.offline_subtitle),
        illustrationRes = R.drawable.ill_state_offline,
        heading = stringResource(R.string.offline_heading),
        body = stringResource(R.string.offline_body),
        actionText = stringResource(R.string.action_retry),
        onAction = onRetry,
    )
}

// Pantalla informativa para roles administrativos cuyo acceso está habilitado únicamente en la web
@Composable
fun WebOnlyAccessScreen(isSigningOut: Boolean, onSignOut: () -> Unit) {
    EcoSystemStateScreen(
        title = stringResource(R.string.web_only_title),
        subtitle = stringResource(R.string.web_only_subtitle),
        illustrationRes = R.drawable.ill_state_web_only,
        heading = stringResource(R.string.web_only_heading),
        body = stringResource(R.string.web_only_body),
        actionText = stringResource(R.string.home_logout),
        onAction = onSignOut,
        isLoading = isSigningOut,
        // El botón de retroceso del sistema ejecuta el cierre de sesión
        onBack = onSignOut,
    )
}

@Preview(name = "Permiso de ubicación", widthDp = 393, heightDp = 852)
@Composable
private fun LocationPermissionPreview() {
    EcoRutaTheme { EcoPermissionScreen(EcoPermissionKind.UBICACION, onAllow = {}, onBack = {}) }
}

@Preview(name = "Permiso de cámara", widthDp = 393, heightDp = 852)
@Composable
private fun CameraPermissionPreview() {
    EcoRutaTheme { EcoPermissionScreen(EcoPermissionKind.CAMARA, onAllow = {}, onBack = {}) }
}

@Preview(name = "Permiso de notificaciones", widthDp = 393, heightDp = 852)
@Composable
private fun NotificationsPermissionPreview() {
    EcoRutaTheme { EcoPermissionScreen(EcoPermissionKind.NOTIFICACIONES, onAllow = {}, onBack = {}) }
}
