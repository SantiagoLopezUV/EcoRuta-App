package com.ecoruta.app.core.navigation

// Grafo principal de navegación declarativa con Jetpack Compose
// Orquesta transiciones entre pantallas públicas y protegidas

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecoruta.app.core.ui.components.PendingScreen
import com.ecoruta.app.core.ui.components.SplashScreen
import com.ecoruta.app.features.auth.presentation.navigation.authGraph
import com.ecoruta.app.features.bulky_waste.presentation.navigation.debrisGraph
import com.ecoruta.app.features.circular_economy.presentation.navigation.circularEconomyGraph
import com.ecoruta.app.features.education.presentation.navigation.educationGraph
import com.ecoruta.app.features.gamification.presentation.navigation.gamificationGraph
import com.ecoruta.app.features.profile.presentation.navigation.profileGraph
import com.ecoruta.app.features.reports.presentation.navigation.reportsGraph
import com.ecoruta.app.features.routes.presentation.navigation.routesGraph
import kotlinx.coroutines.launch

// Curva de animación para la transición desde la pantalla de carga hacia el inicio
private val SplashExitEasing = CubicBezierEasing(0f, 0.676f, 0.695f, 0.213f)
private const val SPLASH_TRANSITION_MS = 300

// Contenedor principal de navegación que ensambla los subgrafos modulares de cada funcionalidad
@Composable
fun EcoRutaNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showNotice: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = EcoRutaDestination.SPLASH,
        ) {
            composable(
                route = EcoRutaDestination.SPLASH,
                exitTransition = { fadeOut(tween(SPLASH_TRANSITION_MS, easing = SplashExitEasing)) },
            ) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(EcoRutaDestination.GUEST_USER) {
                            popUpTo(EcoRutaDestination.SPLASH) { inclusive = true }
                        }
                    },
                )
            }

            routesGraph(
                navController = navController,
                showNotice = showNotice,
                publicHomeEnterTransition = fadeIn(tween(SPLASH_TRANSITION_MS, easing = SplashExitEasing)),
            )
            authGraph(navController = navController)
            profileGraph(navController = navController, showNotice = showNotice)
            reportsGraph(navController = navController)
            debrisGraph(navController = navController)
            circularEconomyGraph(navController = navController, showNotice = showNotice)
            gamificationGraph(navController = navController)
            educationGraph(navController = navController, showNotice = showNotice)

            EcoRutaDestination.pendingScreens.forEach { (route, screenName) ->
                composable(route) { PendingScreen(screenName = screenName) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}
