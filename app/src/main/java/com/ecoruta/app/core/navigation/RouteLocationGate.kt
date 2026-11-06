package com.ecoruta.app.core.navigation

// Validación de la ubicación que comparten los grafos antes de iniciar una ruta
// Pide el permiso, revisa el GPS, abre los ajustes y recalcula el estado al volver a la app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ecoruta.app.core.common.utils.hasLocationPermission
import com.ecoruta.app.core.common.utils.isLocationEnabled
import com.ecoruta.app.core.ui.components.RouteLocationStatus
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

// Estado de la ubicación junto con las acciones que lo resuelven antes de iniciar una ruta
class RouteLocationGate(
    val status: RouteLocationStatus,
    val onStart: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onEnableLocation: () -> Unit,
)

// Pide el permiso de ubicación precisa, revisa el GPS y solo con ambos en orden ejecuta onReady
@Composable
fun rememberRouteLocationGate(lifecycle: Lifecycle, onReady: () -> Unit): RouteLocationGate {
    val context = LocalContext.current
    val currentOnReady by rememberUpdatedState(onReady)
    var status by remember { mutableStateOf(RouteLocationStatus.OK) }

    // Calcula el estado a partir del permiso concedido y del GPS del teléfono
    fun currentStatus(): RouteLocationStatus = when {
        !context.hasLocationPermission() -> RouteLocationStatus.PERMISSION_DENIED
        !context.isLocationEnabled() -> RouteLocationStatus.GPS_DISABLED
        else -> RouteLocationStatus.OK
    }

    // Actualiza el estado e inicia la ruta solo si todo está en orden
    fun startIfReady() {
        status = currentStatus()
        if (status == RouteLocationStatus.OK) currentOnReady()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startIfReady() }
    val resolutionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { status = currentStatus() }

    // Al volver de los ajustes del teléfono se recalcula el estado si había un problema
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && status != RouteLocationStatus.OK) status = currentStatus()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return RouteLocationGate(
        status = status,
        onStart = {
            if (context.hasLocationPermission()) {
                startIfReady()
            } else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        },
        onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        },
        onEnableLocation = {
            // Muestra el diálogo del sistema para encender el GPS y si no se puede abre los ajustes de ubicación
            val request = LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).build())
                .build()
            LocationServices.getSettingsClient(context).checkLocationSettings(request)
                .addOnSuccessListener { status = currentStatus() }
                .addOnFailureListener { error ->
                    if (error is ResolvableApiException) {
                        resolutionLauncher.launch(IntentSenderRequest.Builder(error.resolution).build())
                    } else {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
        },
    )
}
