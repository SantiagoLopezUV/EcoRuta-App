package com.ecoruta.app

// Actividad principal y punto de entrada de la interfaz gráfica
// Inicializa la pantalla de arranque, el tema visual y el grafo de navegación

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ecoruta.app.core.navigation.EcoRutaNavHost
import com.ecoruta.app.core.network.supabase.supabase
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.handleDeeplinks

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        supabase.handleDeeplinks(intent)

        setContent {
            EcoRutaTheme {
                EcoRutaNavHost()
            }
        }
    }
// para que el SDK de supabase intercepte el token o codigo PKCE que viene en la URL
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
    }
}
