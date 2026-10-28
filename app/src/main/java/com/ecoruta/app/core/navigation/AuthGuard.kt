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
        EcoRutaDestination.INICIO_PUBLICO,
        EcoRutaDestination.INICIAR_SESION,
        EcoRutaDestination.REGISTRO,
        EcoRutaDestination.SELECCION_TIPO_REGISTRO,
        EcoRutaDestination.REGISTRO_CIUDADANO_CUENTA,
        EcoRutaDestination.REGISTRO_CIUDADANO_PERFIL,
        EcoRutaDestination.REGISTRO_CIUDADANO_CONFIRMACION,
        EcoRutaDestination.REGISTRO_CIUDADANO_LISTO,
        EcoRutaDestination.RECUPERACION,
        EcoRutaDestination.RECUPERAR_CORREO,
        EcoRutaDestination.RECUPERAR_CODIGO,
        EcoRutaDestination.RESTABLECER_CONTRASENA,
        EcoRutaDestination.CONTRASENA_ACTUALIZADA,
        EcoRutaDestination.SELECCION_BARRIO_SIN_SESION,
        EcoRutaDestination.RUTAS_BARRIO_SIN_SESION,
        EcoRutaDestination.NOTIFICACIONES_SIN_SESION,
        EcoRutaDestination.RESIDUOS_ESPECIALES_SIN_SESION,
        EcoRutaDestination.REPORTES_SIN_SESION,
        EcoRutaDestination.RECICLAJE_SIN_SESION,
        EcoRutaDestination.BENEFICIOS_SIN_SESION,
        EcoRutaDestination.CUPONES_SIN_SESION,
        EcoRutaDestination.EDUCACION_SIN_SESION,
        EcoRutaDestination.GUIA_SEPARACION_SIN_SESION,
        EcoRutaDestination.PERFIL_SIN_SESION,
        EcoRutaDestination.ACCESO_NO_DISPONIBLE,
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
        EcoRutaDestination.SOLICITAR_RETIRO, EcoRutaDestination.MIS_SOLICITUDES_ESCOMBROS -> EcoRutaDestination.RESIDUOS_ESPECIALES_SIN_SESION
        EcoRutaDestination.NUEVO_REPORTE, EcoRutaDestination.MIS_REPORTES -> EcoRutaDestination.REPORTES_SIN_SESION
        EcoRutaDestination.SELECCION_BARRIO -> EcoRutaDestination.SELECCION_BARRIO_SIN_SESION
        EcoRutaDestination.RECICLAJE, EcoRutaDestination.PUBLICAR_MATERIAL -> EcoRutaDestination.RECICLAJE_SIN_SESION
        EcoRutaDestination.BENEFICIOS -> EcoRutaDestination.BENEFICIOS_SIN_SESION
        EcoRutaDestination.MIS_CUPONES -> EcoRutaDestination.CUPONES_SIN_SESION
        EcoRutaDestination.EDUCACION, EcoRutaDestination.GUIA_SEPARACION -> EcoRutaDestination.EDUCACION_SIN_SESION
        EcoRutaDestination.MI_PERFIL -> EcoRutaDestination.PERFIL_SIN_SESION
        EcoRutaDestination.NOTIFICACIONES -> EcoRutaDestination.NOTIFICACIONES_SIN_SESION
        else -> null
    }
}

// Navega hacia una ruta verificando si el usuario tiene permiso según su estado de sesión
fun NavHostController.navigateGuarded(
    route: String,
    isSignedIn: Boolean,
    onAuthRequired: () -> Unit = {
        navigate(EcoRutaDestination.INICIAR_SESION) { launchSingleTop = true }
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
