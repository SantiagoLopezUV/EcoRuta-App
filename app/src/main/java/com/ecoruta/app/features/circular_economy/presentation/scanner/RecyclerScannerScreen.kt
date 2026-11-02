package com.ecoruta.app.features.circular_economy.presentation.scanner

// Vista composable del escáner QR con CameraX y ML Kit Barcode Scanning
// Superpone visor de cámara con recuadro guía y botón para confirmación de entrega

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoSampleQrCode
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
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.RecyclerDelivery
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.SampleRecyclerDeliveries

// Pantalla para leer el código QR que muestra el ciudadano al entregar el material
@Composable
fun RecyclerScannerScreen(
    state: RecyclerScannerUiState,
    onScan: () -> Unit,
    onBack: () -> Unit,
    onTabClick: (RecyclerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    EcoRoleTabScreenLayout(
        items = RecyclerTab.entries,
        selectedTab = RecyclerTab.SCANNER,
        onTabClick = onTabClick,
        onBack = onBack,
        contentSpacing = 8.dp,
        modifier = modifier,
    ) {
        ScreenTitle(
            title = stringResource(R.string.recycler_scanner_title),
            description = stringResource(R.string.recycler_scanner_description),
        )
        CameraViewer(onScan = onScan)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_shield_check), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.recycler_scanner_location_title), style = EcoTextStyles.ButtonText, color = EcoPrimary)
            }
            Text(stringResource(R.string.recycler_scanner_location_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        state.delivery?.let { delivery ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(delivery.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
                Text(delivery.area, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
    }
}

// Visor ilustrativo de la cámara; tocarlo simula la lectura del código mientras no hay cámara real
@Composable
private fun CameraViewer(onScan: () -> Unit) {
    val scanLabel = stringResource(R.string.recycler_scanner_simulate)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(374.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(EcoBlack)
            .clickable(role = Role.Button, onClickLabel = scanLabel, onClick = onScan),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_camera), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.recycler_scanner_camera), style = EcoTextStyles.FieldLabel, color = EcoWhite)
        }
        Box(Modifier.align(Alignment.Center).size(240.dp)) {
            Canvas(Modifier.size(240.dp)) {
                val corner = 48.dp.toPx()
                val stroke = 4.dp.toPx()
                val w = size.width
                val h = size.height
                fun line(start: Offset, end: Offset) = drawLine(EcoAccent, start, end, strokeWidth = stroke, cap = StrokeCap.Square)
                line(Offset(0f, 0f), Offset(corner, 0f)); line(Offset(0f, 0f), Offset(0f, corner))
                line(Offset(w, 0f), Offset(w - corner, 0f)); line(Offset(w, 0f), Offset(w, corner))
                line(Offset(0f, h), Offset(corner, h)); line(Offset(0f, h), Offset(0f, h - corner))
                line(Offset(w, h), Offset(w - corner, h)); line(Offset(w, h), Offset(w, h - corner))
            }
            EcoSampleQrCode(seed = "ECO-REC-QR", color = EcoWhite, modifier = Modifier.align(Alignment.Center).size(104.dp))
        }
        Text(
            stringResource(R.string.recycler_scanner_hint),
            style = EcoTextStyles.Subtitle,
            color = EcoWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

// Pantalla con el resultado de la verificación: entrega completada o motivos del error
@Composable
fun RecyclerScanResultScreen(
    state: RecyclerScannerUiState,
    onBackToMap: () -> Unit,
    onBack: () -> Unit,
    onTabClick: (RecyclerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    EcoRoleTabScreenLayout(
        items = RecyclerTab.entries,
        selectedTab = RecyclerTab.SCANNER,
        onTabClick = onTabClick,
        onBack = onBack,
        modifier = modifier,
    ) {
        ScreenTitle(
            title = stringResource(R.string.recycler_result_title),
            description = stringResource(R.string.recycler_result_description),
        )
        when (val result = state.result) {
            ScanResult.Completed -> CompletedCard(delivery = state.delivery)
            is ScanResult.Failed -> FailedCard(reasons = result.reasons)
        }
        EcoPrimaryButton(text = stringResource(R.string.recycler_result_back_to_map), onClick = onBackToMap, height = 56.dp)
    }
}

@Composable
private fun CompletedCard(delivery: RecyclerDelivery?) {
    ResultCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EcoGreenSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(22.dp))
            }
            Text(stringResource(R.string.recycler_result_completed), style = EcoTextStyles.ButtonText, color = EcoPrimary)
        }
        Text(stringResource(R.string.recycler_result_completed_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        delivery?.let {
            Text(it.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(stringResource(R.string.recycler_result_volume, it.area, it.volume), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
    }
}

@Composable
private fun FailedCard(reasons: List<ScanFailureReason>) {
    ResultCard(spacing = 10) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_alert_triangle), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(24.dp))
            Text(stringResource(R.string.recycler_result_failed), style = EcoTextStyles.ButtonText, color = EcoTitle)
        }
        reasons.forEach { reason ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(reason.titleRes()), style = EcoTextStyles.FieldLabel, color = EcoTitle)
                Text(stringResource(reason.bodyRes()), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
        Text(stringResource(R.string.recycler_result_failed_note), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
    }
}

@Composable
private fun ResultCard(spacing: Int = 8, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
    ) { content() }
}

// Título y descripción usados en las pantallas del escaneo
@Composable
internal fun ScreenTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = EcoTextStyles.Title, color = EcoTitle)
        Text(description, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

private fun ScanFailureReason.titleRes(): Int = when (this) {
    ScanFailureReason.EXPIRED_CODE -> R.string.recycler_result_expired_title
    ScanFailureReason.USED_CODE -> R.string.recycler_result_used_title
    ScanFailureReason.INVALID_LOCATION -> R.string.recycler_result_location_title
}

private fun ScanFailureReason.bodyRes(): Int = when (this) {
    ScanFailureReason.EXPIRED_CODE -> R.string.recycler_result_expired_body
    ScanFailureReason.USED_CODE -> R.string.recycler_result_used_body
    ScanFailureReason.INVALID_LOCATION -> R.string.recycler_result_location_body
}

@Preview(name = "Escáner QR", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerScannerPreview() {
    EcoRutaTheme {
        RecyclerScannerScreen(
            state = RecyclerScannerUiState(delivery = SampleRecyclerDeliveries.first()),
            onScan = {},
            onBack = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Resultado del escaneo · Entrega completada", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerScanCompletedPreview() {
    EcoRutaTheme {
        RecyclerScanResultScreen(
            state = RecyclerScannerUiState(delivery = SampleRecyclerDeliveries.first()),
            onBackToMap = {},
            onBack = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Resultado del escaneo · No se pudo completar", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerScanFailedPreview() {
    EcoRutaTheme {
        RecyclerScanResultScreen(
            state = RecyclerScannerUiState(delivery = SampleRecyclerDeliveries.first(), result = ScanResult.Failed()),
            onBackToMap = {},
            onBack = {},
            onTabClick = {},
        )
    }
}
