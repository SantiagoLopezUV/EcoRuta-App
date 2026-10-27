package com.ecoruta.app.features.routes.presentation.radar

// Diálogo composable de configuración del radar de proximidad
// Slider de selección de radio (100 a 1000 m), switch de alertas y botón guardar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPermissionKind
import com.ecoruta.app.core.ui.components.EcoPermissionScreen
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.routes.presentation.map.formatMeters

data class RadarActions(
    val onBack: () -> Unit = {},
    val onChangeNeighborhood: () -> Unit = {},
    val onRadiusChange: (Int) -> Unit = {},
    val onNotificationsChange: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onNotificationsDenied: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun RadarScreen(state: RadarUiState, actions: RadarActions) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    var showNotificationsRationale by rememberSaveable { mutableStateOf(false) }
    val notificationsLauncher = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) actions.onNotificationsDenied()
            actions.onSave()
        }
    }
    val onSaveClick: () -> Unit = {
        if (state.notificationsEnabled && context.needsNotificationPermission()) showNotificationsRationale = true else actions.onSave()
    }
    if (showNotificationsRationale) {
        EcoPermissionScreen(
            kind = EcoPermissionKind.NOTIFICACIONES,
            onAllow = {
                showNotificationsRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    actions.onSave()
                }
            },
            onBack = { showNotificationsRationale = false },
        )
        return
    }
    EcoTabScreenLayout(
        selectedTab = CitizenTab.MAPA,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.radar_save),
                onClick = onSaveClick,
                isLoading = state.isSaving,
                trailingIconRes = R.drawable.ic_checkbox_check,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.radar_title), style = EcoTextStyles.Titulo, color = EcoTitle)
            Text(stringResource(R.string.radar_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoFieldBackground)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.neighborhoodName ?: "—", style = EcoTextStyles.Boton, color = EcoTitle)
                Text(stringResource(R.string.report_default_area), style = EcoTextStyles.EtiquetaCampo, color = EcoSubtitle)
            }
            EcoTextLink(text = stringResource(R.string.radar_change), onClick = actions.onChangeNeighborhood)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.radar_radius), style = EcoTextStyles.Boton, color = EcoTitle, modifier = Modifier.weight(1f))
                Text(formatMeters(state.radiusMeters), style = EcoTextStyles.Numeros, color = EcoTitle)
            }
            RadarRings(radiusMeters = state.radiusMeters)
            Slider(
                value = state.radiusMeters.toFloat(),
                onValueChange = { actions.onRadiusChange(Math.round(it)) },
                valueRange = RadarDefaults.MIN_RADIUS.toFloat()..RadarDefaults.MAX_RADIUS.toFloat(),
                steps = (RadarDefaults.MAX_RADIUS - RadarDefaults.MIN_RADIUS) / RadarDefaults.STEP - 1,
                colors = SliderDefaults.colors(
                    thumbColor = EcoPrimary,
                    activeTrackColor = EcoPrimary,
                    inactiveTrackColor = EcoBorder,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Row {
                EcoDataLabel(formatMeters(RadarDefaults.MIN_RADIUS), Modifier.weight(1f))
                EcoDataLabel(formatMeters(RadarDefaults.MAX_RADIUS))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.radar_notifications), style = EcoTextStyles.Boton, color = EcoTitle)
                Text(
                    stringResource(if (state.notificationsEnabled) R.string.radar_notifications_on else R.string.radar_notifications_off),
                    style = EcoTextStyles.Subtitulo,
                    color = EcoSubtitle,
                )
            }
            Switch(
                checked = state.notificationsEnabled,
                onCheckedChange = actions.onNotificationsChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EcoWhite,
                    checkedTrackColor = EcoAccent,
                    uncheckedThumbColor = EcoSubtitle,
                    uncheckedTrackColor = EcoFieldBackground,
                    uncheckedBorderColor = EcoBorder,
                ),
            )
        }
        Text(
            stringResource(R.string.radar_explanation, formatMeters(state.radiusMeters)),
            style = EcoTextStyles.Subtitulo,
            color = EcoSubtitle,
        )
        EcoInfoHint(text = stringResource(R.string.radar_push_pending_hint))
    }
}

@Composable
private fun RadarRings(radiusMeters: Int) {
    val fraction = (radiusMeters - RadarDefaults.MIN_RADIUS).toFloat() / (RadarDefaults.MAX_RADIUS - RadarDefaults.MIN_RADIUS)
    Box(
        Modifier
            .fillMaxWidth()
            .height(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = (28.dp.toPx() + fraction * 36.dp.toPx())
            drawCircle(color = EcoAccent.copy(alpha = 0.12f), radius = outer)
            drawCircle(
                color = EcoAccent,
                radius = outer,
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(EcoPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_map_pin_white), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun ProximityAlertScreen(
    neighborhoodName: String?,
    radiusMeters: Int,
    notificationsEnabled: Boolean,
    onViewRoute: () -> Unit,
    onBack: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    val name = neighborhoodName ?: "—"
    EcoTabScreenLayout(
        selectedTab = CitizenTab.MAPA,
        onBack = onBack,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.alert_view_route), onClick = onViewRoute, trailingIconRes = R.drawable.ic_arrow_right)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.alert_title), style = EcoTextStyles.Titulo, color = EcoTitle)
            Text(stringResource(R.string.alert_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(EcoPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_brand_leaf), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(14.dp))
                }
                Text(stringResource(R.string.app_name), style = EcoTextStyles.EtiquetaCampo, color = EcoTitle, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.alert_preview_label), style = EcoTextStyles.EtiquetaCampo, color = EcoSubtitle)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.alert_message_title), style = EcoTextStyles.Boton, color = EcoTitle)
            }
            Text(stringResource(R.string.alert_message_detail, name, formatMeters(radiusMeters)), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                .background(EcoPrimary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_truck), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = EcoTextStyles.Boton, color = EcoWhite)
                    Text(stringResource(R.string.alert_route_caption), style = EcoTextStyles.EtiquetaCampo, color = EcoWhite.copy(alpha = 0.8f))
                }
            }
            Row {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.radar_radius), style = EcoTextStyles.EtiquetaCampo, color = EcoWhite.copy(alpha = 0.8f))
                    Text(formatMeters(radiusMeters), style = EcoTextStyles.Numeros, color = EcoWhite)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.radar_notifications), style = EcoTextStyles.EtiquetaCampo, color = EcoWhite.copy(alpha = 0.8f))
                    Text(
                        stringResource(if (notificationsEnabled) R.string.radar_notifications_on else R.string.radar_notifications_off),
                        style = EcoTextStyles.Boton,
                        color = EcoWhite,
                    )
                }
            }
        }
        if (!notificationsEnabled) EcoInfoHint(text = stringResource(R.string.alert_notifications_off_hint))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.alert_prepare_title), style = EcoTextStyles.Boton, color = EcoTitle)
            Text(stringResource(R.string.alert_prepare_body), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
    }
}

private fun Context.needsNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

@Preview(name = "Radar de proximidad", widthDp = 393, heightDp = 900)
@Composable
private fun RadarPreview() {
    EcoRutaTheme { RadarScreen(RadarUiState(neighborhoodName = "La Esperanza"), RadarActions()) }
}

@Preview(name = "Alerta de proximidad", widthDp = 393, heightDp = 852)
@Composable
private fun ProximityAlertPreview() {
    EcoRutaTheme {
        ProximityAlertScreen(
            neighborhoodName = "La Esperanza",
            radiusMeters = 250,
            notificationsEnabled = true,
            onViewRoute = {},
            onBack = {},
            onTabClick = {},
        )
    }
}
