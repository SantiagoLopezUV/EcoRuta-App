package com.ecoruta.app.core.ui.components

// Pantalla de bienvenida con la animación de entrada de la marca EcoRuta

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoWhite
import kotlinx.coroutines.delay

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
            style = EcoTextStyles.SplashBrand,
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
