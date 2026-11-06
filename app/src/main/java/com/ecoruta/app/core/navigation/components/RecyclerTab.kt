package com.ecoruta.app.core.navigation.components

// Catálogo de pestañas principales de la barra inferior para el rol reciclador
// Asocia cada sección del módulo reciclador con su etiqueta de texto e icono correspondiente

import com.ecoruta.app.R

// Opciones de navegación inferior disponibles para el reciclador
enum class RecyclerTab(
    override val labelRes: Int,
    override val iconRes: Int,
) : EcoBottomBarItem {
    MAP(R.string.tab_mapa, R.drawable.ic_nav_map),
    ROUTES(R.string.tab_routes, R.drawable.ic_list),
    SCANNER(R.string.tab_scanner, R.drawable.ic_qr_code),
    PROFILE(R.string.tab_perfil, R.drawable.ic_nav_user),
}

// Pestañas visibles según el tipo de reciclador, la de rutas solo aparece para el vinculado a una empresa
fun recyclerTabs(isCompanyLinked: Boolean): List<RecyclerTab> =
    if (isCompanyLinked) RecyclerTab.entries else RecyclerTab.entries - RecyclerTab.ROUTES
