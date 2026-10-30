package com.ecoruta.app.features.education.presentation.navigation

// Subgrafo de navegación para el módulo de educación ambiental y guías didácticas
// Conecta el catálogo de lecciones y la guía de separación con y sin sesión

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.navigateToCitizenTab
import com.ecoruta.app.core.navigation.navigateToPublicTab
import com.ecoruta.app.features.education.presentation.EducationActions
import com.ecoruta.app.features.education.presentation.EducationScreen
import com.ecoruta.app.features.education.presentation.EducationTopic
import com.ecoruta.app.features.education.presentation.EducationUiState
import com.ecoruta.app.features.education.presentation.GuestSeparationGuideScreen
import com.ecoruta.app.features.education.presentation.SeparationGuideActions
import com.ecoruta.app.features.education.presentation.SeparationGuideScreen
import com.ecoruta.app.features.education.presentation.SeparationGuideUiState
import com.ecoruta.app.features.education.presentation.TopicIcon
import com.ecoruta.app.features.education.presentation.TopicReward

private val SampleEducationTopics = listOf(
    EducationTopic(
        id = "EDU-01",
        title = "Cómo separar tus residuos",
        summary = "Guía rápida del código de colores en Colombia",
        rewardPoints = 50,
        icon = TopicIcon.LIST,
        opensSeparationGuide = true,
    ),
)

// Registra las rutas del catálogo educativo y de la guía de separación de residuos
fun NavGraphBuilder.educationGraph(
    navController: NavHostController,
    showNotice: (String) -> Unit,
) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }

    composable(EcoRutaDestination.EDUCATION) {
        var state by remember {
            mutableStateOf(
                EducationUiState(
                    isLoading = false,
                    topics = SampleEducationTopics,
                    isSample = true,
                ),
            )
        }
        val context = LocalContext.current
        EducationScreen(
            state = state,
            actions = EducationActions(
                onBack = { navController.popBackStack() },
                onQueryChange = { state = state.copy(query = it) },
                onTopicClick = { topic ->
                    if (topic.opensSeparationGuide) {
                        open(EcoRutaDestination.SEPARATION_GUIDE)
                    } else {
                        showNotice(context.getString(R.string.education_topic_coming_soon))
                    }
                },
                onToggleFavorite = { clicked ->
                    state = state.copy(
                        topics = state.topics.map { if (it.id == clicked.id) it.copy(favorite = !it.favorite) else it },
                    )
                },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.SEPARATION_GUIDE) {
        var state by remember { mutableStateOf(SeparationGuideUiState()) }
        SeparationGuideScreen(
            state = state,
            actions = SeparationGuideActions(
                onBack = { navController.popBackStack() },
                onToggleFavorite = { state = state.copy(favorite = !state.favorite) },
                onComplete = {
                    state = state.copy(
                        completed = true,
                        reward = TopicReward(pointsEarned = state.rewardPoints),
                        completedWithoutBackend = true,
                    )
                },
                onViewBenefits = {
                    state = state.copy(reward = null)
                    navController.navigateToCitizenTab(CitizenTab.BENEFITS)
                },
                onCloseReward = {
                    state = state.copy(reward = null)
                    navController.popBackStack()
                },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.GUEST_EDUCATION) {
        var state by remember {
            mutableStateOf(
                EducationUiState(
                    isLoading = false,
                    topics = SampleEducationTopics,
                    isSample = true,
                ),
            )
        }
        val context = LocalContext.current
        EducationScreen(
            state = state,
            isSignedIn = false,
            actions = EducationActions(
                onBack = { navController.popBackStack() },
                onQueryChange = { state = state.copy(query = it) },
                onTopicClick = { topic ->
                    if (topic.opensSeparationGuide) {
                        open(EcoRutaDestination.GUEST_SEPARATION_GUIDE)
                    } else {
                        showNotice(context.getString(R.string.education_topic_coming_soon))
                    }
                },
                onToggleFavorite = { open(EcoRutaDestination.LOGIN) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.GUEST_SEPARATION_GUIDE) {
        GuestSeparationGuideScreen(
            onBack = { navController.popBackStack() },
            onLogin = { open(EcoRutaDestination.LOGIN) },
            onTabClick = { tab -> navController.navigateToPublicTab(tab) },
        )
    }
}
