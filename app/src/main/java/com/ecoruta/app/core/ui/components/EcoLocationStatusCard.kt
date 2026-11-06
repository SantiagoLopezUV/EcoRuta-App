package com.ecoruta.app.core.ui.components

// Tarjeta que explica el uso de la ubicación durante una ruta y avisa cuando no se puede compartir
// La comparten el chofer y el reciclador vinculado antes de iniciar su recorrido

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles

// Estado de la ubicación del teléfono al intentar iniciar una ruta
enum class RouteLocationStatus {
    OK,
    PERMISSION_DENIED,
    GPS_DISABLED,
}

// Muestra la tarjeta informativa o la de error con su botón según el estado de la ubicación
@Composable
fun EcoLocationStatusCard(
    status: RouteLocationStatus,
    infoBody: String,
    onOpenSettings: () -> Unit,
    onEnableLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(EcoDimens.ControlCornerRadius)
    when (status) {
        RouteLocationStatus.OK -> Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(EcoFieldBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.recycler_route_location_title), style = EcoTextStyles.FieldLabel, color = EcoPrimary)
            Text(infoBody, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
        RouteLocationStatus.PERMISSION_DENIED -> LocationErrorCard(
            title = stringResource(R.string.route_location_denied_title),
            body = stringResource(R.string.route_location_denied_body),
            action = stringResource(R.string.route_location_open_settings),
            onAction = onOpenSettings,
            modifier = modifier,
        )
        RouteLocationStatus.GPS_DISABLED -> LocationErrorCard(
            title = stringResource(R.string.route_location_gps_off_title),
            body = stringResource(R.string.route_location_gps_off_body),
            action = stringResource(R.string.route_location_enable),
            onAction = onEnableLocation,
            modifier = modifier,
        )
    }
}

// Tarjeta con borde y título en color de error y un botón para resolver el problema
@Composable
private fun LocationErrorCard(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(EcoDimens.ControlCornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoFieldBackground)
            .border(EcoDimens.BorderWidth, EcoError, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = EcoTextStyles.FieldLabel, color = EcoError)
        Text(body, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        EcoSecondaryButton(text = action, onClick = onAction)
    }
}

@Preview(name = "Ubicación lista", widthDp = 354)
@Composable
private fun EcoLocationStatusOkPreview() {
    EcoRutaTheme {
        EcoLocationStatusCard(status = RouteLocationStatus.OK, infoBody = stringResource(R.string.recycler_route_location_body), onOpenSettings = {}, onEnableLocation = {})
    }
}

@Preview(name = "Ubicación sin permiso", widthDp = 354)
@Composable
private fun EcoLocationStatusDeniedPreview() {
    EcoRutaTheme {
        EcoLocationStatusCard(status = RouteLocationStatus.PERMISSION_DENIED, infoBody = stringResource(R.string.recycler_route_location_body), onOpenSettings = {}, onEnableLocation = {})
    }
}

@Preview(name = "Ubicación con GPS apagado", widthDp = 354)
@Composable
private fun EcoLocationStatusGpsOffPreview() {
    EcoRutaTheme {
        EcoLocationStatusCard(status = RouteLocationStatus.GPS_DISABLED, infoBody = stringResource(R.string.recycler_route_location_body), onOpenSettings = {}, onEnableLocation = {})
    }
}
