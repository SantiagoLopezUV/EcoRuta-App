package com.ecoruta.app.core.navigation

// Validación del permiso de cámara que comparten los grafos antes de abrir un escáner
// Muestra la explicación previa, pide el permiso, abre los ajustes y recalcula el estado al volver a la app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// Estado del permiso de cámara junto con las acciones que lo resuelven antes de escanear
class CameraPermissionGate(
    val hasPermission: Boolean,
    val showRationale: Boolean,
    val isPermanentlyDenied: Boolean,
    val onRequest: () -> Unit,
    val onAllow: () -> Unit,
    val onDismissRationale: () -> Unit,
)

// Sin permiso muestra primero la explicación y solo al aceptarla lanza la solicitud del sistema
@Composable
fun rememberCameraPermissionGate(lifecycle: Lifecycle): CameraPermissionGate {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var showRationale by rememberSaveable { mutableStateOf(!context.hasCameraPermission()) }
    var isPermanentlyDenied by rememberSaveable { mutableStateOf(false) }

    // Si el sistema ya no deja volver a preguntar, el botón del visor lleva a los ajustes del teléfono
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        val activity = context.findActivity()
        isPermanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }

    // Al volver de los ajustes del teléfono se revisa de nuevo si ya se concedió el permiso
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !hasPermission) hasPermission = context.hasCameraPermission()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return CameraPermissionGate(
        hasPermission = hasPermission,
        showRationale = showRationale && !hasPermission,
        isPermanentlyDenied = isPermanentlyDenied,
        onRequest = {
            if (isPermanentlyDenied) {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            } else {
                showRationale = true
            }
        },
        onAllow = {
            showRationale = false
            permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onDismissRationale = { showRationale = false },
    )
}

// Indica si el usuario ya concedió el acceso a la cámara
private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

// Recorre los contextos envueltos hasta encontrar la actividad que aloja la pantalla
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
