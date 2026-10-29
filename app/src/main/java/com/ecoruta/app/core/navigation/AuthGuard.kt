package com.ecoruta.app.core.navigation

// Lógica de guardián de autenticación para navegación y acciones protegidas
// Intercepta solicitudes sin sesión y dispara diálogos de login o registro

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ecoruta.app.core.ui.components.AuthRequiredDialog

// Objeto de políticas de seguridad para clasificar y proteger rutas de la aplicación
object AuthGuard {
    // Conjunto de rutas que pueden ser visitadas libremente en modo visitante
    val publicDestinations: Set<String> = setOf(
        EcoRutaDestination.SPLASH,
        EcoRutaDestination.PUBLIC_HOME,
        EcoRutaDestination.LOGIN,
        EcoRutaDestination.REGISTRATION,
        EcoRutaDestination.REGISTRATION_TYPE_SELECTION,
        EcoRutaDestination.CITIZEN_REGISTRATION_ACCOUNT,
        EcoRutaDestination.CITIZEN_REGISTRATION_PROFILE,
        EcoRutaDestination.CITIZEN_REGISTRATION_CONFIRMATION,
        EcoRutaDestination.CITIZEN_REGISTRATION_DONE,
        EcoRutaDestination.PASSWORD_RECOVERY,
        EcoRutaDestination.RECOVERY_EMAIL,
        EcoRutaDestination.RECOVERY_CODE,
        EcoRutaDestination.RESET_PASSWORD,
        EcoRutaDestination.PASSWORD_UPDATED,
        EcoRutaDestination.GUEST_NEIGHBORHOOD_SELECTION,
        EcoRutaDestination.GUEST_NEIGHBORHOOD_ROUTES,
        EcoRutaDestination.GUEST_NOTIFICATIONS,
        EcoRutaDestination.GUEST_SPECIAL_WASTE,
        EcoRutaDestination.GUEST_REPORTS,
        EcoRutaDestination.GUEST_RECYCLING,
        EcoRutaDestination.GUEST_BENEFITS,
        EcoRutaDestination.GUEST_MY_COUPONS,
        EcoRutaDestination.GUEST_EDUCATION,
        EcoRutaDestination.GUEST_SEPARATION_GUIDE,
        EcoRutaDestination.GUEST_PROFILE,
        EcoRutaDestination.WEB_ONLY_ACCESS,
    )

    // Evalúa si una ruta solicitada requiere obligatoriamente una sesión iniciada
    fun isProtected(route: String?): Boolean {
        if (route.isNullOrBlank()) return false
        val cleanRoute = route.substringBefore("?").substringBefore("/{")
        return publicDestinations.none { publicRoute ->
            val cleanPublic = publicRoute.substringBefore("?").substringBefore("/{")
            cleanRoute == cleanPublic || cleanRoute.startsWith("$cleanPublic/")
        }
    }

    // Devuelve la ruta pública equivalente si existe una versión informativa para visitantes
    fun publicAlternativeFor(route: String?): String? = when (route) {
        EcoRutaDestination.REQUEST_DEBRIS_PICKUP, EcoRutaDestination.MY_DEBRIS_REQUESTS -> EcoRutaDestination.GUEST_SPECIAL_WASTE
        EcoRutaDestination.NEW_REPORT, EcoRutaDestination.MY_REPORTS -> EcoRutaDestination.GUEST_REPORTS
        EcoRutaDestination.NEIGHBORHOOD_SELECTION -> EcoRutaDestination.GUEST_NEIGHBORHOOD_SELECTION
        EcoRutaDestination.RECYCLING, EcoRutaDestination.PUBLISH_MATERIAL -> EcoRutaDestination.GUEST_RECYCLING
        EcoRutaDestination.BENEFITS -> EcoRutaDestination.GUEST_BENEFITS
        EcoRutaDestination.MY_COUPONS -> EcoRutaDestination.GUEST_MY_COUPONS
        EcoRutaDestination.EDUCATION, EcoRutaDestination.SEPARATION_GUIDE -> EcoRutaDestination.GUEST_EDUCATION
        EcoRutaDestination.MY_PROFILE -> EcoRutaDestination.GUEST_PROFILE
        EcoRutaDestination.NOTIFICATIONS -> EcoRutaDestination.GUEST_NOTIFICATIONS
        else -> null
    }
}

// Navega hacia una ruta verificando si el usuario tiene permiso según su estado de sesión
fun NavHostController.navigateGuarded(
    route: String,
    isSignedIn: Boolean,
    onAuthRequired: () -> Unit = {
        navigate(EcoRutaDestination.LOGIN) { launchSingleTop = true }
    },
) {
    if (AuthGuard.isProtected(route) && !isSignedIn) {
        val alternative = AuthGuard.publicAlternativeFor(route)
        if (alternative != null) {
            navigate(alternative) { launchSingleTop = true }
        } else {
            onAuthRequired()
        }
    } else {
        navigate(route) { launchSingleTop = true }
    }
}

// Contenedor composable que bloquea el contenido si no se detecta sesión activa
@Composable
fun AuthGuard(
    isSignedIn: Boolean,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (isSignedIn) {
        content()
    } else {
        AuthRequiredDialog(
            onDismiss = onDismiss,
            onLoginClick = onLoginClick,
            onRegisterClick = onRegisterClick,
        )
    }
}
