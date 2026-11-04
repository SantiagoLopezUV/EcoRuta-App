package com.ecoruta.app.core.navigation.components

// Catálogo de pestañas principales de la barra inferior para el rol chofer
// Asocia cada sección del módulo chofer con su etiqueta de texto e icono correspondiente

import com.ecoruta.app.R

// Opciones de navegación inferior disponibles para el chofer
enum class DriverTab(
    override val labelRes: Int,
    override val iconRes: Int,
) : EcoBottomBarItem {
    ORDERS(R.string.driver_tab_orders, R.drawable.ic_profile_clipboard),
    MAP(R.string.tab_mapa, R.drawable.ic_nav_map),
    PROFILE(R.string.tab_perfil, R.drawable.ic_nav_user),
}
