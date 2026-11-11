package com.ecoruta.app

import android.app.Application
import android.util.Log
import com.ecoruta.app.core.network.supabase.supabaseEnvironmentLogLine
import dagger.hilt.android.HiltAndroidApp

// Inicializador principal de la aplicación
// Configura el árbol de dependencias global con Dagger Hilt
@HiltAndroidApp
class EcoRutaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Deja en Logcat el entorno y el host de Supabase para saber a qué servidor apunta esta compilación
        Log.i(LOG_TAG, supabaseEnvironmentLogLine(BuildConfig.SUPABASE_ENV, BuildConfig.SUPABASE_URL))
    }

    private companion object {
        const val LOG_TAG = "EcoRuta"
    }
}
