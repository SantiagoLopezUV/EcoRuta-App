package com.ecoruta.app.core.navigation.components

// Catálogo de pestañas principales de la barra inferior para el rol comercio aliado
// Asocia cada sección del módulo comercio con su etiqueta de texto e icono correspondiente

import com.ecoruta.app.R

// Opciones de navegación inferior disponibles para el comercio aliado
enum class MerchantTab(
    override val labelRes: Int,
    override val iconRes: Int,
) : EcoBottomBarItem {
    DASHBOARD(R.string.merchant_tab_dashboard, R.drawable.ic_nav_home),
    CREATE_COUPON(R.string.merchant_tab_create, R.drawable.ic_plus),
    VALIDATE(R.string.merchant_tab_validate, R.drawable.ic_qr_code),
    MY_COUPONS(R.string.merchant_tab_coupons, R.drawable.ic_nav_ticket),
    PROFILE(R.string.tab_perfil, R.drawable.ic_nav_user),
}
