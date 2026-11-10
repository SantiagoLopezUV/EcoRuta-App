package com.ecoruta.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Inicializador principal de la aplicación
// Configura el árbol de dependencias global con Dagger Hilt
@HiltAndroidApp
class EcoRutaApp : Application()

