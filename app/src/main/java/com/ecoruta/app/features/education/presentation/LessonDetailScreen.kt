package com.ecoruta.app.features.education.presentation

// Vista composable con el contenido detallado de la lección ambiental
// Enseñanza de separación en contenedores y botón Marcar como completado (+20 pts)

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccentSoft
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

data class SeparationGuideActions(
    val onBack: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onComplete: () -> Unit = {},
    val onViewBenefits: () -> Unit = {},
    val onCloseReward: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

private data class GuideBin(@DrawableRes val illustration: Int, @StringRes val label: Int)

private data class GuideStep(@StringRes val title: Int, @StringRes val body: Int)

private val guideBins = listOf(
    GuideBin(R.drawable.ill_bin_white, R.string.guide_bin_white),
    GuideBin(R.drawable.ill_bin_green, R.string.guide_bin_green),
    GuideBin(R.drawable.ill_bin_black, R.string.guide_bin_black),
)

private val guideSteps = listOf(
    GuideStep(R.string.guide_step_1_title, R.string.guide_step_1_body),
    GuideStep(R.string.guide_step_2_title, R.string.guide_step_2_body),
    GuideStep(R.string.guide_step_3_title, R.string.guide_step_3_body),
)

@Composable
fun SeparationGuideScreen(state: SeparationGuideUiState, actions: SeparationGuideActions) {
    GuideLayout(
        favorite = state.favorite,
        onBack = actions.onBack,
        onToggleFavorite = actions.onToggleFavorite,
        onTabClick = actions.onTabClick,
        bottomAction = {
            if (state.completed) {
                EcoPrimaryButton(
                    text = stringResource(R.string.guide_completed),
                    onClick = {},
                    enabled = false,
                    height = 72.dp,
                    cornerRadius = 16.dp,
                )
            } else {
                EcoPrimaryButton(
                    text = stringResource(R.string.guide_mark_completed, state.rewardPoints),
                    onClick = actions.onComplete,
                    isLoading = state.isCompleting,
                    height = 72.dp,
                    cornerRadius = 16.dp,
                )
            }
        },
    )
    state.reward?.let { reward ->
        LessonCompletedDialog(
            points = reward.pointsEarned,
            withoutBackend = state.completedWithoutBackend,
            onViewBenefits = actions.onViewBenefits,
            onClose = actions.onCloseReward,
        )
    }
}

@Composable
fun GuestSeparationGuideScreen(onBack: () -> Unit, onLogin: () -> Unit, onTabClick: (CitizenTab) -> Unit) {
    GuideLayout(
        favorite = false,
        onBack = onBack,
        onToggleFavorite = onLogin,
        onTabClick = onTabClick,
        bottomAction = {
            EcoPrimaryButton(
                text = stringResource(R.string.guide_guest_login),
                onClick = onLogin,
                height = 72.dp,
                cornerRadius = 16.dp,
            )
        },
    )
}

@Composable
private fun GuideLayout(
    favorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
    bottomAction: @Composable () -> Unit,
) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.EDUCACION,
        onBack = onBack,
        onTabClick = onTabClick,
        topBarAction = { FavoriteButton(favorite = favorite, onClick = onToggleFavorite, cornerRadius = 12, iconSize = 20, bordered = false) },
        bottomActions = { bottomAction() },
    ) {
        Text(stringResource(R.string.guide_title), style = EcoTextStyles.Title, color = EcoTitle)
        BinsIllustration()
        Text(stringResource(R.string.guide_intro), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            guideSteps.forEachIndexed { index, step -> StepRow(number = index + 1, step = step) }
        }
        Text(
            stringResource(R.string.guide_notice),
            style = EcoTextStyles.LinkText,
            color = EcoPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoFieldBackground)
                .padding(12.dp),
        )
    }
}

@Composable
private fun BinsIllustration() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
            .background(EcoFieldBackground),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        guideBins.forEach { bin ->
            Column(Modifier.width(82.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Image(painterResource(bin.illustration), contentDescription = null, modifier = Modifier.size(width = 72.dp, height = 88.dp))
                Text(stringResource(bin.label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, step: GuideStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = EcoTextStyles.Numbers, color = EcoPrimary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(step.title), style = EcoTextStyles.LinkText, color = EcoTitle)
            Text(stringResource(step.body), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
    }
}

@Composable
private fun LessonCompletedDialog(points: Int, withoutBackend: Boolean, onViewBenefits: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EcoWhite)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(EcoAccentSoft)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", style = EcoTextStyles.ButtonText.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold), color = EcoPrimary)
            }
            Text(stringResource(R.string.guide_reward_title), style = EcoTextStyles.ButtonText.copy(fontSize = 20.sp), color = EcoTitle, textAlign = TextAlign.Center)
            Text(stringResource(R.string.guide_reward_body, points), style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center)
            if (withoutBackend) EcoInfoHint(text = stringResource(R.string.guide_reward_design_mode))
            EcoPrimaryButton(text = stringResource(R.string.guide_reward_view_benefits), onClick = onViewBenefits)
            Text(
                stringResource(R.string.guide_reward_close),
                style = EcoTextStyles.LinkText.copy(fontWeight = FontWeight.Bold),
                color = EcoSubtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onClose)
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Preview(name = "Cómo separar tus residuos", widthDp = 393, heightDp = 900)
@Composable
private fun SeparationGuidePreview() {
    EcoRutaTheme { SeparationGuideScreen(SeparationGuideUiState(), SeparationGuideActions()) }
}

@Preview(name = "Guía de separación sin sesión", widthDp = 393, heightDp = 900)
@Composable
private fun GuestSeparationGuidePreview() {
    EcoRutaTheme {
        GuestSeparationGuideScreen(onBack = {}, onLogin = {}, onTabClick = {})
    }
}
