package com.ecoruta.app

// Actividad principal y punto de entrada de la interfaz gráfica
// Inicializa la pantalla de arranque, el tema visual y el grafo de navegación

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ecoruta.app.core.navigation.EcoRutaNavHost
import com.ecoruta.app.core.ui.theme.EcoRutaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EcoRutaTheme {
                EcoRutaNavHost()
            }
        }
    }
}
