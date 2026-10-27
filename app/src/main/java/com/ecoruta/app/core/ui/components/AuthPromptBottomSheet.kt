package com.ecoruta.app.core.ui.components

// Hoja inferior interactiva para invitación de acceso y registro
// Alternativa visual en modal inferior para acciones que requieren autenticación

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import kotlinx.coroutines.delay

// Hoja inferior interactiva para invitar al usuario a iniciar sesión o crear cuenta
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthPromptBottomSheet(
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.auth_prompt_title),
    subtitle: String = stringResource(R.string.auth_prompt_subtitle),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = EcoWhite,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoFieldBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = EcoPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = EcoTextStyles.Titulo,
                    color = EcoTitle,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = EcoTextStyles.Subtitulo,
                    color = EcoSubtitle,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EcoPrimaryButton(
                    text = stringResource(R.string.auth_action_login),
                    onClick = onLoginClick,
                    trailingIconRes = R.drawable.ic_arrow_right,
                )
                EcoSecondaryButton(
                    text = stringResource(R.string.auth_action_register),
                    onClick = onRegisterClick,
                )
                EcoTextLink(
                    text = stringResource(R.string.auth_action_continue_browsing),
                    onClick = onDismiss,
                )
            }
        }
    }
}

// Textos descriptivos para presentar un servicio que requiere iniciar sesión
data class GuestServiceContent(
    @StringRes val banner: Int,
    @StringRes val lockedSection: Int,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val benefits: List<Int>,
)

data class GuestServiceActions(
    val onBack: () -> Unit = {},
    val onLogin: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Plantilla compartida para pantallas de servicios consultados sin sesión iniciada
@Composable
fun GuestServiceScreen(content: GuestServiceContent, actions: GuestServiceActions) {
    EcoTabScreenLayout(selectedTab = CitizenTab.INICIO, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                .background(EcoPrimary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(content.banner), style = EcoTextStyles.Subtitulo, color = EcoWhite)
            EcoPrimaryButton(text = stringResource(R.string.home_login), onClick = actions.onLogin, height = 48.dp)
            Text(stringResource(content.lockedSection), style = EcoTextStyles.EtiquetaCampo, color = EcoWhite)
        }
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(content.title), style = EcoTextStyles.Titulo, color = EcoTitle)
            Text(stringResource(content.description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.guest_can_title), style = EcoTextStyles.Boton, color = EcoTitle)
            content.benefits.forEach { res ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(EcoPrimary),
                    )
                    Text(stringResource(res), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
                }
            }
        }
    }
}

// Vista temporal utilizada para indicar pantallas que aún están en construcción
@Composable
fun PendingScreen(screenName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = EcoDimens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = screenName, style = EcoTextStyles.Titulo, color = EcoTitle, textAlign = TextAlign.Center)
        Text(
            text = "Pantalla pendiente de implementar",
            style = EcoTextStyles.Subtitulo,
            color = EcoSubtitle,
            textAlign = TextAlign.Center,
        )
    }
}

private const val SPLASH_DURATION_MS = 1_000
private const val INTRO_ANIMATION_MS = 300
private val IntroEasing = CubicBezierEasing(0f, 0.676f, 0.695f, 0.213f)
private val LogoWidth = 145.dp
private val LogoHeight = 146.dp
private val LogoTextGap = 24.dp
private val BrandTextLineHeight = 50.dp
private val FrameBottomPadding = 32.dp
private val LogoFinalOffset = -(FrameBottomPadding / 2 + (LogoHeight + LogoTextGap + BrandTextLineHeight) / 2 - LogoHeight / 2)
private val TextOffsetFromLogo = LogoHeight / 2 + LogoTextGap + BrandTextLineHeight / 2

// Pantalla de bienvenida que muestra la animación de entrada de la marca EcoRuta
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnSplashFinished by rememberUpdatedState(onSplashFinished)
    val inPreview = LocalInspectionMode.current
    val introProgress = remember { Animatable(if (inPreview) 1f else 0f) }

    LaunchedEffect(Unit) {
        introProgress.animateTo(1f, tween(INTRO_ANIMATION_MS, easing = IntroEasing))
        delay((SPLASH_DURATION_MS - INTRO_ANIMATION_MS).toLong())
        currentOnSplashFinished()
    }

    LightSystemBarIcons()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EcoPrimary)
            .padding(horizontal = EcoDimens.ScreenHorizontalPadding),
    ) {
        Image(
            painter = painterResource(R.drawable.ecoruta_logo_circular),
            contentDescription = stringResource(R.string.logo_ecoruta_description),
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = LogoWidth, height = LogoHeight)
                .graphicsLayer { translationY = (LogoFinalOffset * introProgress.value).toPx() },
        )
        Text(
            text = stringResource(R.string.app_name),
            style = EcoTextStyles.MarcaSplash,
            color = EcoWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationY = (LogoFinalOffset * introProgress.value + TextOffsetFromLogo).toPx()
                    alpha = introProgress.value
                },
        )
    }
}

// Configura temporalmente los íconos del sistema en modo claro para fondos oscuros
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                previousStatus?.let { controller.isAppearanceLightStatusBars = it }
                previousNavigation?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}

// Previsualización de la pantalla de bienvenida EcoRuta
@Preview(name = "Splash Screen", widthDp = 393, heightDp = 852)
@Composable
private fun SplashScreenPreview() {
    EcoRutaTheme {
        SplashScreen(onSplashFinished = {})
    }
}
