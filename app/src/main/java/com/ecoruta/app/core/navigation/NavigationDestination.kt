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
    const val INICIO_PUBLICO = "inicio_publico"

    // Pantalla de inicio de sesión
    const val INICIAR_SESION = "iniciar_sesion"

    // Rutas del flujo de recuperación de contraseña
    const val RECUPERACION = "recuperacion"
    const val RECUPERAR_CORREO = "recuperacion/correo"
    const val RECUPERAR_CODIGO = "recuperacion/codigo"
    const val RESTABLECER_CONTRASENA = "recuperacion/restablecer"
    const val CONTRASENA_ACTUALIZADA = "recuperacion/actualizada"

    // Rutas del flujo de registro de nuevos usuarios
    const val REGISTRO = "registro"
    const val SELECCION_TIPO_REGISTRO = "registro/seleccion"
    const val REGISTRO_CIUDADANO_CUENTA = "registro/ciudadano/cuenta"
    const val REGISTRO_CIUDADANO_PERFIL = "registro/ciudadano/perfil"
    const val REGISTRO_CIUDADANO_CONFIRMACION = "registro/ciudadano/confirmacion"
    const val REGISTRO_CIUDADANO_LISTO = "registro/ciudadano/listo"

    // Rutas y argumentos para el cambio de contraseña por primer ingreso o desde el perfil
    const val CAMBIAR_CONTRASENA = "cuenta/cambiar-contrasena/{modo}/{rol}"
    const val CONTRASENA_CAMBIADA = "cuenta/contrasena-cambiada/{modo}/{rol}"
    const val ARG_MODO = "modo"
    const val ARG_ROL = "rol"

    fun cambiarContrasena(modo: ChangePasswordMode, rol: UserRole) = "cuenta/cambiar-contrasena/${modo.name}/${rol.name}"
    fun contrasenaCambiada(modo: ChangePasswordMode, rol: UserRole) = "cuenta/contrasena-cambiada/${modo.name}/${rol.name}"

    // Pantallas principales de otros roles del sistema
    const val MAPA_ENTREGAS = "reciclador/mapa_entregas"
    const val MIS_ORDENES = "chofer/mis_ordenes"
    const val PANEL_COMERCIO = "comercio/panel"

    // Devuelve la ruta inicial correspondiente al rol activo tras iniciar sesión
    fun inicioDelRol(rol: UserRole): String = when (rol) {
        UserRole.CIUDADANO -> INICIO_CIUDADANO
        UserRole.RECICLADOR -> MAPA_ENTREGAS
        UserRole.CHOFER -> MIS_ORDENES
        UserRole.COMERCIO -> PANEL_COMERCIO
    }

    // Rutas de inicio y perfil para el ciudadano con sesión activa
    const val INICIO_CIUDADANO = "inicio_ciudadano"
    const val MI_PERFIL = "ciudadano/mi_perfil"

    // Rutas del flujo para crear un nuevo reporte ciudadano
    const val NUEVO_REPORTE = "ciudadano/nuevo_reporte"
    const val REPORTE_FOTO = "ciudadano/nuevo_reporte/foto"
    const val REPORTE_TIPO = "ciudadano/nuevo_reporte/tipo"
    const val REPORTE_UBICACION = "ciudadano/nuevo_reporte/ubicacion"
    const val REPORTE_ENVIADO = "ciudadano/nuevo_reporte/enviado"

    // Rutas de consulta de reportes ciudadanos y vista informativa sin sesión
    const val MIS_REPORTES = "ciudadano/mis_reportes"
    const val DETALLE_REPORTE = "ciudadano/reportes/{id}"
    const val ARG_REPORT_ID = "id"
    const val REPORTES_SIN_SESION = "reportes_sin_sesion"

    fun detalleReporte(id: String) = "ciudadano/reportes/$id"

    // Rutas del flujo para solicitar retiro de residuos voluminosos o escombros
    const val SOLICITAR_RETIRO = "ciudadano/solicitar_retiro"
    const val RETIRO_SOLICITUD = "ciudadano/solicitar_retiro/solicitud"
    const val RETIRO_GUIA = "ciudadano/solicitar_retiro/guia"
    const val RETIRO_FECHA = "ciudadano/solicitar_retiro/fecha"
    const val RETIRO_ENVIADA = "ciudadano/solicitar_retiro/enviada"
    const val RETIRO_SEGUIMIENTO_NUEVA = "ciudadano/solicitar_retiro/seguimiento"

    // Rutas de historial y seguimiento de solicitudes de escombros
    const val MIS_SOLICITUDES_ESCOMBROS = "ciudadano/mis_solicitudes_escombros"
    const val SEGUIMIENTO_RETIRO = "ciudadano/escombros/{id}"
    const val RESIDUOS_ESPECIALES_SIN_SESION = "residuos_especiales_sin_sesion"

    fun seguimientoRetiro(id: String) = "ciudadano/escombros/$id"

    // Rutas de selección de barrio, consulta de horarios de recolección y configuración de radar
    const val SELECCION_BARRIO = "seleccion_barrio"
    const val SELECCION_BARRIO_SIN_SESION = "seleccion_barrio_sin_sesion"
    const val RUTAS_BARRIO = "ciudadano/rutas_barrio/{id}"
    const val RUTAS_BARRIO_SIN_SESION = "rutas_barrio_sin_sesion/{id}"
    const val RADAR = "ciudadano/radar/{id}"
    const val ALERTA_PROXIMIDAD = "ciudadano/alerta/{id}/{radio}/{avisos}"
    const val ARG_ID = "id"
    const val ARG_RADIO = "radio"
    const val ARG_AVISOS = "avisos"

    fun rutasBarrio(id: String) = "ciudadano/rutas_barrio/$id"
    fun rutasBarrioSinSesion(id: String) = "rutas_barrio_sin_sesion/$id"
    fun radar(id: String) = "ciudadano/radar/$id"
    fun alertaProximidad(id: String, radio: Int, avisos: Boolean) = "ciudadano/alerta/$id/$radio/$avisos"

    // Rutas de consulta de notificaciones con y sin sesión
    const val NOTIFICACIONES = "ciudadano/notificaciones"
    const val NOTIFICACIONES_SIN_SESION = "notificaciones_sin_sesion"

    // Rutas del módulo de reciclaje, publicación de materiales y código de entrega
    const val RECICLAJE = "ciudadano/reciclaje"
    const val RECICLAJE_SIN_SESION = "reciclaje_sin_sesion"
    const val PUBLICAR_MATERIAL = "ciudadano/reciclaje/publicar"
    const val DETALLE_PUBLICACION = "ciudadano/publicacion/{id}"
    const val CODIGO_ENTREGA = "ciudadano/publicacion/{id}/codigo"
    const val ENTREGA_COMPLETADA = "ciudadano/publicacion/{id}/completada"

    fun detallePublicacion(id: String) = "ciudadano/publicacion/$id"
    fun codigoEntrega(id: String) = "ciudadano/publicacion/$id/codigo"
    fun entregaCompletada(id: String) = "ciudadano/publicacion/$id/completada"

    // Rutas del catálogo de beneficios, canje de cupones e historial de puntos
    const val BENEFICIOS = "ciudadano/beneficios"
    const val BENEFICIOS_SIN_SESION = "beneficios_sin_sesion"
    const val CANJE_CUPON = "ciudadano/beneficios/cupon/{id}"
    const val HISTORIAL_ECOPUNTOS = "ciudadano/beneficios/historial"
    const val MIS_CUPONES = "ciudadano/mis_cupones"
    const val MIS_CUPONES_PERFIL = "ciudadano/perfil/mis_cupones"
    const val ARG_DESDE_PERFIL = "perfil"
    const val DETALLE_MI_CUPON = "ciudadano/mis_cupones/{id}?perfil={perfil}"

    fun canjeCupon(id: String) = "ciudadano/beneficios/cupon/$id"
    fun detalleMiCupon(id: String, desdePerfil: Boolean) = "ciudadano/mis_cupones/$id?perfil=$desdePerfil"

    // Rutas del catálogo educativo y guía de separación de residuos
    const val EDUCACION = "ciudadano/educacion"
    const val EDUCACION_SIN_SESION = "educacion_sin_sesion"
    const val GUIA_SEPARACION = "ciudadano/educacion/separar-residuos"
    const val GUIA_SEPARACION_SIN_SESION = "educacion_sin_sesion/separar-residuos"

    // Ruta para cambiar entre perfiles habilitados o solicitar un nuevo rol
    const val CAMBIAR_PERFIL = "ciudadano/cambiar_perfil"

    // Rutas para solicitar el registro de un comercio aliado
    const val SOLICITUD_COMERCIO = "ciudadano/solicitud_comercio"
    const val ARG_SIN_BACKEND = "sinBackend"
    const val SOLICITUD_COMERCIO_REVISION = "ciudadano/solicitud_comercio/revision?sinBackend={sinBackend}"

    fun solicitudComercioRevision(sinBackend: Boolean) = "ciudadano/solicitud_comercio/revision?sinBackend=$sinBackend"

    // Rutas del flujo de postulación para el rol de reciclador
    const val POSTULACION_RECICLADOR = "ciudadano/postulacion_reciclador"
    const val POSTULACION_FORMULARIO = "ciudadano/postulacion_reciclador/formulario"
    const val POSTULACION_BUSCAR_EMPRESA = "ciudadano/postulacion_reciclador/buscar_empresa"
    const val POSTULACION_EMPRESA_SELECCIONADA = "ciudadano/postulacion_reciclador/empresa"
    const val POSTULACION_ENVIADA = "ciudadano/postulacion_reciclador/enviada"
    const val POSTULACION_EMPRESA_ENVIADA = "ciudadano/postulacion_reciclador/empresa_enviada"

    // Ruta informativa para cuentas cuyo acceso es exclusivo desde el panel web
    const val ACCESO_NO_DISPONIBLE = "acceso_no_disponible"

    // Rutas correspondientes a módulos pendientes de otros roles
    const val REGISTRO_RECICLADOR_INDEPENDIENTE = "registro_reciclador_independiente"
    const val REGISTRO_RECICLADOR_EMPRESA = "registro_reciclador_empresa"
    const val REGISTRO_COMERCIO = "registro_comercio"
    const val PERFIL_SIN_SESION = "perfil_sin_sesion"

    // Mapa con los títulos descriptivos de las pantallas pendientes de implementar
    val pendingScreens: Map<String, String> = mapOf(
        MAPA_ENTREGAS to "Reciclador · Mapa de entregas",
        MIS_ORDENES to "Chofer · Mis órdenes",
        PANEL_COMERCIO to "Comercio · Panel del comercio",
        REGISTRO_RECICLADOR_INDEPENDIENTE to "Registro · Reciclador independiente",
        REGISTRO_RECICLADOR_EMPRESA to "Registro · Reciclador vinculado a empresa",
        REGISTRO_COMERCIO to "Registro · Comercios aliados",
    )
}

// Redirige al inicio público limpiando toda la pila de navegación tras cerrar sesión
fun NavHostController.goToPublicHomeAfterSignOut() {
    navigate(EcoRutaDestination.INICIO_PUBLICO) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

// Navega entre las pestañas principales cuando el usuario explora sin sesión iniciada
fun NavHostController.navigateToPublicTab(tab: CitizenTab) {
    val route = when (tab) {
        CitizenTab.INICIO -> {
            popBackStack(EcoRutaDestination.INICIO_PUBLICO, inclusive = false)
            return
        }
        CitizenTab.BENEFICIOS, CitizenTab.CUPONES -> EcoRutaDestination.BENEFICIOS_SIN_SESION
        CitizenTab.MAPA -> EcoRutaDestination.SELECCION_BARRIO_SIN_SESION
        CitizenTab.EDUCACION -> EcoRutaDestination.EDUCACION_SIN_SESION
        CitizenTab.PERFIL -> EcoRutaDestination.PERFIL_SIN_SESION
    }
    navigate(route) {
        popUpTo(EcoRutaDestination.INICIO_PUBLICO)
        launchSingleTop = true
    }
}

// Navega entre las pestañas principales del ciudadano con sesión activa
fun NavHostController.navigateToCitizenTab(tab: CitizenTab) {
    val route = when (tab) {
        CitizenTab.INICIO -> {
            popBackStack(EcoRutaDestination.INICIO_CIUDADANO, inclusive = false)
            return
        }
        CitizenTab.BENEFICIOS -> EcoRutaDestination.BENEFICIOS
        CitizenTab.CUPONES -> EcoRutaDestination.MIS_CUPONES
        CitizenTab.MAPA -> EcoRutaDestination.SELECCION_BARRIO
        CitizenTab.EDUCACION -> EcoRutaDestination.EDUCACION
        CitizenTab.PERFIL -> EcoRutaDestination.MI_PERFIL
    }
    navigate(route) {
        popUpTo(EcoRutaDestination.INICIO_CIUDADANO)
        launchSingleTop = true
    }
}
