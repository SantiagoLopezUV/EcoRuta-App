package com.ecoruta.app.features.reports.presentation.create

// Paso 3 del reporte: ubicación en el mapa y dirección del punto afectado

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPermissionKind
import com.ecoruta.app.core.ui.components.EcoPermissionScreen
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Tercer paso del formulario de reporte para indicar la ubicación en el mapa y dirección
data class ReportLocationActions(
    val onUseCurrentLocation: () -> Unit = {},
    val onLocationPermissionDenied: () -> Unit = {},
    val onEditAddress: () -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onAddressDone: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun ReportLocationScreen(state: NewReportUiState, actions: ReportLocationActions) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val permissionLauncher = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) actions.onUseCurrentLocation() else actions.onLocationPermissionDenied()
        }
    }
    var showLocationRationale by rememberSaveable { mutableStateOf(false) }
    val requestLocation: () -> Unit = {
        if (context.hasLocationPermission()) actions.onUseCurrentLocation() else showLocationRationale = true
    }
    if (showLocationRationale) {
        EcoPermissionScreen(
            kind = EcoPermissionKind.LOCATION,
            onAllow = {
                showLocationRationale = false
                permissionLauncher?.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            onBack = { showLocationRationale = false },
        )
        return
    }
    if (!inPreview) {
        LaunchedEffect(Unit) {
            if (state.address.isBlank() && !state.hasGpsLocation && context.hasLocationPermission()) actions.onUseCurrentLocation()
        }
    }

    ReportStepLayout(
        step = 3,
        stageLabel = stringResource(R.string.report_stage_location),
        title = stringResource(R.string.report_location_title),
        description = stringResource(R.string.report_location_description),
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(if (state.isSubmitting) R.string.report_sending else R.string.report_send),
                onClick = actions.onSubmit,
                isLoading = state.isSubmitting,
                enabled = !state.isLocating,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IllustrativeMap(
                hasLocation = state.hasGpsLocation,
                isLocating = state.isLocating,
                onMyLocationClick = requestLocation,
            )
            Text(stringResource(R.string.report_location_helper), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            OutlinedButton(
                onClick = requestLocation,
                enabled = !state.isLocating && !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EcoBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = EcoWhite, contentColor = EcoPrimary),
            ) {
                if (state.isLocating) {
                    CircularProgressIndicator(color = EcoPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(if (state.isLocating) R.string.report_locating else R.string.report_use_current_location),
                    style = EcoTextStyles.LinkText,
                )
            }
            if (state.isEditingAddress) {
                EcoTextField(
                    value = state.address,
                    onValueChange = actions.onAddressChange,
                    label = stringResource(R.string.report_address_label),
                    placeholder = stringResource(R.string.report_address_placeholder),
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { actions.onAddressDone() }),
                )
                if (state.address.isNotBlank()) {
                    EcoTextLink(text = stringResource(R.string.report_address_save), onClick = actions.onAddressDone)
                }
            } else if (state.address.isNotBlank()) {
                AddressCard(state = state, onEdit = actions.onEditAddress)
            }
            state.locationError?.let { EcoErrorMessage(text = stringResource(it)) }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
private fun AddressCard(state: NewReportUiState, onEdit: () -> Unit) {
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
            Text(state.address, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(
                state.area.ifBlank { stringResource(R.string.report_default_area) },
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
            )
            if (state.latitude != null && state.longitude != null) {
                Text(
                    stringResource(R.string.report_gps_coordinates, state.latitude, state.longitude),
                    style = EcoTextStyles.FieldLabel.copy(fontSize = 11.sp),
                    color = EcoSubtitle,
                )
            }
        }
        EcoTextLink(text = stringResource(R.string.action_edit), onClick = onEdit)
    }
}

private val MapBlocks = listOf(
    floatArrayOf(18f, 12f, 74f, 46f), floatArrayOf(115f, 12f, 74f, 46f), floatArrayOf(214f, 12f, 90f, 46f),
    floatArrayOf(18f, 80f, 74f, 48f), floatArrayOf(115f, 80f, 74f, 48f),
    floatArrayOf(18f, 151f, 74f, 48f), floatArrayOf(115f, 151f, 74f, 48f), floatArrayOf(215f, 151f, 90f, 48f),
)

@Composable
private fun IllustrativeMap(hasLocation: Boolean, isLocating: Boolean, onMyLocationClick: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(212.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground),
    ) {
        val scale = maxWidth / 350f
        MapBlocks.forEach { (x, y, w, h) ->
            Box(
                Modifier
                    .offset(x = scale * x, y = y.dp)
                    .size(width = scale * w, height = h.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(EcoWhite),
            )
        }
        Box(
            Modifier
                .offset(x = scale * 215f, y = 80.dp)
                .size(width = scale * 90f, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(EcoGreenSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.report_map_park),
                style = EcoTextStyles.FieldLabel,
                color = EcoWhite
            )
        }
        Text(
            stringResource(R.string.report_map_street_a),
            style = EcoTextStyles.FieldLabel,
            color = EcoSubtitle,
            modifier = Modifier.offset(x = scale * 21f, y = 60.dp)
        )
        Text(
            stringResource(R.string.report_map_street_b),
            style = EcoTextStyles.FieldLabel,
            color = EcoSubtitle,
            modifier = Modifier.offset(x = scale * 22f, y = 131.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(if (hasLocation) EcoPrimary else EcoSubtitle),
            contentAlignment = Alignment.Center,
        ) {
            if (isLocating) {
                CircularProgressIndicator(
                    color = EcoWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_map_pin_white),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.report_map_illustrative),
            style = EcoTextStyles.NavigationLabel,
            color = EcoSubtitle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EcoWhite)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(EcoWhite)
                .border(1.dp, EcoPrimary, CircleShape)
                .clickable(
                    enabled = !isLocating,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.report_use_current_location),
                    onClick = onMyLocationClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_map_pin),
                contentDescription = stringResource(R.string.report_use_current_location),
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview(name = "Paso 3 Ubicación", widthDp = 393, heightDp = 852)
@Composable
private fun ReportLocationPreview() {
    EcoRutaTheme {
        ReportLocationScreen(
            state = NewReportUiState(
                address = "Cra. 22E #6-40",
                area = "Torobajo · Pasto",
                latitude = 1.2136,
                longitude = -77.2811,
                isEditingAddress = false,
            ),
            actions = ReportLocationActions(),
        )
    }
}
