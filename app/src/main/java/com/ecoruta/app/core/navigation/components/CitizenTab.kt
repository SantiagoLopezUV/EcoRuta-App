package com.ecoruta.app.core.navigation.components

// Catálogo de pestañas principales de la barra inferior para el rol ciudadano
// Asocia cada sección principal con su etiqueta de texto e icono correspondiente

import com.ecoruta.app.R

// Opciones de navegación inferior disponibles para el ciudadano
enum class CitizenTab(
    override val labelRes: Int,
    override val iconRes: Int,
) : EcoBottomBarItem {
    HOME(R.string.tab_inicio, R.drawable.ic_nav_home),
    BENEFITS(R.string.tab_beneficios, R.drawable.ic_nav_gift),
    COUPONS(R.string.tab_cupones, R.drawable.ic_nav_ticket),
    MAP(R.string.tab_mapa, R.drawable.ic_nav_map),
    EDUCATION(R.string.tab_educacion, R.drawable.ic_nav_book),
    PROFILE(R.string.tab_perfil, R.drawable.ic_nav_user),
}
