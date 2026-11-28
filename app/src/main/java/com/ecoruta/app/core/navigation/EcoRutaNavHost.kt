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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecoruta.app.core.network.supabase.supabase
import com.ecoruta.app.core.ui.components.PendingScreen
import com.ecoruta.app.core.ui.components.SplashScreen
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordMode
import com.ecoruta.app.features.auth.presentation.navigation.authGraph
import com.ecoruta.app.features.bulky_waste.presentation.navigation.debrisGraph
import com.ecoruta.app.features.circular_economy.presentation.navigation.circularEconomyGraph
import com.ecoruta.app.features.circular_economy.presentation.navigation.recyclerGraph
import com.ecoruta.app.features.driver.presentation.navigation.driverGraph
import com.ecoruta.app.features.education.presentation.navigation.educationGraph
import com.ecoruta.app.features.gamification.presentation.navigation.gamificationGraph
import com.ecoruta.app.features.gamification.presentation.navigation.merchantGraph
import com.ecoruta.app.features.profile.domain.model.UserRole
import com.ecoruta.app.features.profile.presentation.navigation.profileGraph
import com.ecoruta.app.features.reports.presentation.navigation.reportsGraph
import com.ecoruta.app.features.routes.presentation.navigation.routesGraph
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

// Curva de animación para la transición desde la pantalla de carga hacia el inicio
private val SplashExitEasing = CubicBezierEasing(0f, 0.676f, 0.695f, 0.213f)
private const val SPLASH_TRANSITION_MS = 300

// Obtiene la ruta inicial según el rol y la obligación de cambio de contraseña
private fun resolveHomeDestination(): String {
    val user = supabase.auth.currentUserOrNull()
    val rawRole = runCatching {
        user?.userMetadata?.get("role_code")?.jsonPrimitive?.contentOrNull
    }.getOrNull()
    val role = UserRole.fromCode(rawRole)
    val mustChange = runCatching {
        user?.userMetadata?.get("must_change_password")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
    }.getOrNull() == true
    return if (mustChange) {
        EcoRutaDestination.changePassword(ChangePasswordMode.FIRST_LOGIN, role)
    } else {
        EcoRutaDestination.homeForRole(role)
    }
}

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

    // Redirige al inicio cuando se valida la sesión o limpia el estado si falla el refresco
    LaunchedEffect(navController) {
        supabase.auth.sessionStatus.collect { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val currentRoute = navController.currentDestination?.route.orEmpty()
                    val isRegistrationRoute = currentRoute.startsWith("registro")
                    if (isRegistrationRoute) {
                        val targetHome = resolveHomeDestination()
                        navController.navigate(targetHome) {
                            popUpTo(EcoRutaDestination.GUEST_USER) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
                is SessionStatus.RefreshFailure -> {
                    // Limpia la sesión zombi en el almacenamiento para evitar que el usuario quede atrapado
                    // Si el token ya vencií y el servidor lo renueva, limpia la sesion local con signOut() y dirige al modo visitante
                    runCatching { supabase.auth.signOut() }
                    val currentRoute = navController.currentDestination?.route.orEmpty()
                    if (currentRoute.isNotBlank() && currentRoute != EcoRutaDestination.LOGIN && currentRoute != EcoRutaDestination.GUEST_USER && currentRoute != EcoRutaDestination.SPLASH) {
                        showNotice("Tu sesión ha expirado. Por favor inicia sesión nuevamente.")
                        navController.navigate(EcoRutaDestination.GUEST_USER) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                else -> Unit
            }
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
                        scope.launch {
                            val destination = runCatching {
                                val currentSession = supabase.auth.currentSessionOrNull()
                                if (currentSession != null) {
                                    // Intenta refrescar el token si está próximo a vencer antes de entrar al inicio
                                    val refreshResult = runCatching { supabase.auth.refreshCurrentSession() }
                                    if (refreshResult.isFailure && supabase.auth.currentSessionOrNull() == null) {
                                        runCatching { supabase.auth.signOut() }
                                        EcoRutaDestination.GUEST_USER
                                    } else {
                                        resolveHomeDestination()
                                    }
                                } else {
                                    EcoRutaDestination.GUEST_USER
                                }
                            }.getOrDefault(EcoRutaDestination.GUEST_USER)

                            navController.navigate(destination) {
                                popUpTo(EcoRutaDestination.SPLASH) { inclusive = true }
                            }
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
            recyclerGraph(navController = navController)
            driverGraph(navController = navController)
            gamificationGraph(navController = navController)
            merchantGraph(navController = navController)
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
