package com.ecoruta.app.core.common.utils

// Comprobaciones del permiso de ubicación y del estado del GPS del teléfono
// Se usan antes de iniciar una ruta para saber si la ubicación se puede compartir

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

// Indica si el usuario concedió la ubicación precisa, la aproximada no basta para registrar visitas
fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

// Indica si la ubicación del teléfono está encendida
fun Context.isLocationEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return LocationManagerCompat.isLocationEnabled(manager)
}
