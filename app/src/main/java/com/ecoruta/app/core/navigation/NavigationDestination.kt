package com.ecoruta.app.core.navigation

// Catálogo oficial de destinos y rutas tipadas de la aplicación
// Define argumentos de navegación y clasifica pantallas públicas y protegidas

import androidx.navigation.NavHostController
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordMode
import com.ecoruta.app.features.profile.presentation.UserRole

typealias EcoRutaDestination = NavigationDestination

// Objeto con las constantes de ruta, argumentos y constructores de navegación
object NavigationDestination {
    // Pantalla inicial de presentación de la aplicación
    const val SPLASH = "splash"

    // Pantalla de inicio público sin sesión iniciada
    const val PUBLIC_HOME = "inicio_publico"

    // Pantalla de inicio de sesión
    const val LOGIN = "iniciar_sesion"

    // Rutas del flujo de recuperación de contraseña
    const val PASSWORD_RECOVERY = "recuperacion"
    const val RECOVERY_EMAIL = "recuperacion/correo"
    const val RECOVERY_CODE = "recuperacion/codigo"
    const val RESET_PASSWORD = "recuperacion/restablecer"
    const val PASSWORD_UPDATED = "recuperacion/actualizada"

    // Rutas del flujo de registro de nuevos usuarios
    const val REGISTRATION = "registro"
    const val REGISTRATION_TYPE_SELECTION = "registro/seleccion"
    const val CITIZEN_REGISTRATION_ACCOUNT = "registro/ciudadano/cuenta"
    const val CITIZEN_REGISTRATION_PROFILE = "registro/ciudadano/perfil"
    const val CITIZEN_REGISTRATION_CONFIRMATION = "registro/ciudadano/confirmacion"
    const val CITIZEN_REGISTRATION_DONE = "registro/ciudadano/listo"

    // Rutas y argumentos para el cambio de contraseña por primer ingreso o desde el perfil
    const val CHANGE_PASSWORD = "cuenta/cambiar-contrasena/{modo}/{rol}"
    const val PASSWORD_CHANGED = "cuenta/contrasena-cambiada/{modo}/{rol}"
    const val ARG_MODE = "modo"
    const val ARG_ROLE = "rol"

    fun changePassword(mode: ChangePasswordMode, role: UserRole) = "cuenta/cambiar-contrasena/${mode.name}/${role.name}"
    fun passwordChanged(mode: ChangePasswordMode, role: UserRole) = "cuenta/contrasena-cambiada/${mode.name}/${role.name}"

    // Pantallas principales de otros roles del sistema
    const val DELIVERY_MAP = "reciclador/mapa_entregas"
    const val MY_ORDERS = "chofer/mis_ordenes"
    const val STORE_DASHBOARD = "comercio/panel"

    // Devuelve la ruta inicial correspondiente al rol activo tras iniciar sesión
    fun homeForRole(role: UserRole): String = when (role) {
        UserRole.CIUDADANO -> CITIZEN_HOME
        UserRole.RECICLADOR -> DELIVERY_MAP
        UserRole.CHOFER -> MY_ORDERS
        UserRole.COMERCIO -> STORE_DASHBOARD
    }

    // Rutas de inicio y perfil para el ciudadano con sesión activa
    const val CITIZEN_HOME = "inicio_ciudadano"
    const val MY_PROFILE = "ciudadano/mi_perfil"

    // Rutas del flujo para crear un nuevo reporte ciudadano
    const val NEW_REPORT = "ciudadano/nuevo_reporte"
    const val REPORT_PHOTO = "ciudadano/nuevo_reporte/foto"
    const val REPORT_TYPE = "ciudadano/nuevo_reporte/tipo"
    const val REPORT_LOCATION = "ciudadano/nuevo_reporte/ubicacion"
    const val REPORT_SENT = "ciudadano/nuevo_reporte/enviado"

    // Rutas de consulta de reportes ciudadanos y vista informativa sin sesión
    const val MY_REPORTS = "ciudadano/mis_reportes"
    const val REPORT_DETAIL = "ciudadano/reportes/{id}"
    const val ARG_REPORT_ID = "id"
    const val GUEST_REPORTS = "reportes_sin_sesion"

    fun reportDetail(id: String) = "ciudadano/reportes/$id"

    // Rutas del flujo para solicitar retiro de residuos voluminosos o escombros
    const val REQUEST_DEBRIS_PICKUP = "ciudadano/solicitar_retiro"
    const val DEBRIS_REQUEST_FORM = "ciudadano/solicitar_retiro/solicitud"
    const val DEBRIS_GUIDE = "ciudadano/solicitar_retiro/guia"
    const val DEBRIS_DATE = "ciudadano/solicitar_retiro/fecha"
    const val DEBRIS_REQUEST_SENT = "ciudadano/solicitar_retiro/enviada"
    const val NEW_DEBRIS_TRACKING = "ciudadano/solicitar_retiro/seguimiento"

    // Rutas de historial y seguimiento de solicitudes de escombros
    const val MY_DEBRIS_REQUESTS = "ciudadano/mis_solicitudes_escombros"
    const val DEBRIS_TRACKING = "ciudadano/escombros/{id}"
    const val GUEST_SPECIAL_WASTE = "residuos_especiales_sin_sesion"

    fun debrisTracking(id: String) = "ciudadano/escombros/$id"

    // Rutas de selección de barrio, consulta de horarios de recolección y configuración de radar
    const val NEIGHBORHOOD_SELECTION = "seleccion_barrio"
    const val GUEST_NEIGHBORHOOD_SELECTION = "seleccion_barrio_sin_sesion"
    const val NEIGHBORHOOD_ROUTES = "ciudadano/rutas_barrio/{id}"
    const val GUEST_NEIGHBORHOOD_ROUTES = "rutas_barrio_sin_sesion/{id}"
    const val RADAR = "ciudadano/radar/{id}"
    const val PROXIMITY_ALERT = "ciudadano/alerta/{id}/{radio}/{avisos}"
    const val ARG_ID = "id"
    const val ARG_RADIUS = "radio"
    const val ARG_NOTIFICATIONS_ENABLED = "avisos"

    fun neighborhoodRoutes(id: String) = "ciudadano/rutas_barrio/$id"
    fun guestNeighborhoodRoutes(id: String) = "rutas_barrio_sin_sesion/$id"
    fun radar(id: String) = "ciudadano/radar/$id"
    fun proximityAlert(id: String, radius: Int, notificationsEnabled: Boolean) = "ciudadano/alerta/$id/$radius/$notificationsEnabled"

    // Rutas de consulta de notificaciones con y sin sesión
    const val NOTIFICATIONS = "ciudadano/notificaciones"
    const val GUEST_NOTIFICATIONS = "notificaciones_sin_sesion"

    // Rutas del módulo de reciclaje, publicación de materiales y código de entrega
    const val RECYCLING = "ciudadano/reciclaje"
    const val GUEST_RECYCLING = "reciclaje_sin_sesion"
    const val PUBLISH_MATERIAL = "ciudadano/reciclaje/publicar"
    const val PUBLICATION_DETAIL = "ciudadano/publicacion/{id}"
    const val DELIVERY_CODE = "ciudadano/publicacion/{id}/codigo"
    const val DELIVERY_COMPLETED = "ciudadano/publicacion/{id}/completada"

    fun publicationDetail(id: String) = "ciudadano/publicacion/$id"
    fun deliveryCode(id: String) = "ciudadano/publicacion/$id/codigo"
    fun deliveryCompleted(id: String) = "ciudadano/publicacion/$id/completada"

    // Rutas del catálogo de beneficios, canje de cupones e historial de puntos
    const val BENEFITS = "ciudadano/beneficios"
    const val GUEST_BENEFITS = "beneficios_sin_sesion"
    const val COUPON_REDEMPTION = "ciudadano/beneficios/cupon/{id}"
    const val ECOPOINTS_HISTORY = "ciudadano/beneficios/historial"
    const val MY_COUPONS = "ciudadano/mis_cupones"
    const val PROFILE_MY_COUPONS = "ciudadano/perfil/mis_cupones"
    const val ARG_FROM_PROFILE = "perfil"
    const val MY_COUPON_DETAIL = "ciudadano/mis_cupones/{id}?perfil={perfil}"

    fun couponRedemption(id: String) = "ciudadano/beneficios/cupon/$id"
    fun myCouponDetail(id: String, fromProfile: Boolean) = "ciudadano/mis_cupones/$id?perfil=$fromProfile"

    // Rutas del catálogo educativo y guía de separación de residuos
    const val EDUCATION = "ciudadano/educacion"
    const val GUEST_EDUCATION = "educacion_sin_sesion"
    const val SEPARATION_GUIDE = "ciudadano/educacion/separar-residuos"
    const val GUEST_SEPARATION_GUIDE = "educacion_sin_sesion/separar-residuos"

    // Ruta para cambiar entre perfiles habilitados o solicitar un nuevo rol
    const val SWITCH_PROFILE = "ciudadano/cambiar_perfil"

    // Rutas para solicitar el registro de un comercio aliado
    const val STORE_REQUEST = "ciudadano/solicitud_comercio"
    const val ARG_WITHOUT_BACKEND = "sinBackend"
    const val STORE_REQUEST_REVIEW = "ciudadano/solicitud_comercio/revision?sinBackend={sinBackend}"

    fun storeRequestReview(withoutBackend: Boolean) = "ciudadano/solicitud_comercio/revision?sinBackend=$withoutBackend"

    // Rutas del flujo de postulación para el rol de reciclador
    const val RECYCLER_APPLICATION = "ciudadano/postulacion_reciclador"
    const val RECYCLER_APPLICATION_FORM = "ciudadano/postulacion_reciclador/formulario"
    const val RECYCLER_APPLICATION_COMPANY_SEARCH = "ciudadano/postulacion_reciclador/buscar_empresa"
    const val RECYCLER_APPLICATION_SELECTED_COMPANY = "ciudadano/postulacion_reciclador/empresa"
    const val RECYCLER_APPLICATION_SENT = "ciudadano/postulacion_reciclador/enviada"
    const val RECYCLER_APPLICATION_COMPANY_SENT = "ciudadano/postulacion_reciclador/empresa_enviada"

    // Ruta informativa para cuentas cuyo acceso es exclusivo desde el panel web
    const val WEB_ONLY_ACCESS = "acceso_no_disponible"

    // Rutas correspondientes a módulos pendientes de otros roles
    const val INDEPENDENT_RECYCLER_REGISTRATION = "registro_reciclador_independiente"
    const val COMPANY_RECYCLER_REGISTRATION = "registro_reciclador_empresa"
    const val STORE_REGISTRATION = "registro_comercio"
    const val GUEST_PROFILE = "perfil_sin_sesion"

    // Mapa con los títulos descriptivos de las pantallas pendientes de implementar
    val pendingScreens: Map<String, String> = mapOf(
        DELIVERY_MAP to "Reciclador · Mapa de entregas",
        MY_ORDERS to "Chofer · Mis órdenes",
        STORE_DASHBOARD to "Comercio · Panel del comercio",
        INDEPENDENT_RECYCLER_REGISTRATION to "Registro · Reciclador independiente",
        COMPANY_RECYCLER_REGISTRATION to "Registro · Reciclador vinculado a empresa",
        STORE_REGISTRATION to "Registro · Comercios aliados",
    )
}

// Redirige al inicio público limpiando toda la pila de navegación tras cerrar sesión
fun NavHostController.goToPublicHomeAfterSignOut() {
    navigate(EcoRutaDestination.PUBLIC_HOME) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

// Navega entre las pestañas principales cuando el usuario explora sin sesión iniciada
fun NavHostController.navigateToPublicTab(tab: CitizenTab) {
    val route = when (tab) {
        CitizenTab.HOME -> {
            popBackStack(EcoRutaDestination.PUBLIC_HOME, inclusive = false)
            return
        }
        CitizenTab.BENEFITS, CitizenTab.COUPONS -> EcoRutaDestination.GUEST_BENEFITS
        CitizenTab.MAP -> EcoRutaDestination.GUEST_NEIGHBORHOOD_SELECTION
        CitizenTab.EDUCATION -> EcoRutaDestination.GUEST_EDUCATION
        CitizenTab.PROFILE -> EcoRutaDestination.GUEST_PROFILE
    }
    navigate(route) {
        popUpTo(EcoRutaDestination.PUBLIC_HOME)
        launchSingleTop = true
    }
}

// Navega entre las pestañas principales del ciudadano con sesión activa
fun NavHostController.navigateToCitizenTab(tab: CitizenTab) {
    val route = when (tab) {
        CitizenTab.HOME -> {
            popBackStack(EcoRutaDestination.CITIZEN_HOME, inclusive = false)
            return
        }
        CitizenTab.BENEFITS -> EcoRutaDestination.BENEFITS
        CitizenTab.COUPONS -> EcoRutaDestination.MY_COUPONS
        CitizenTab.MAP -> EcoRutaDestination.NEIGHBORHOOD_SELECTION
        CitizenTab.EDUCATION -> EcoRutaDestination.EDUCATION
        CitizenTab.PROFILE -> EcoRutaDestination.MY_PROFILE
    }
    navigate(route) {
        popUpTo(EcoRutaDestination.CITIZEN_HOME)
        launchSingleTop = true
    }
}
