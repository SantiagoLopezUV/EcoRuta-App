package com.ecoruta.app.features.education.presentation

// Vista composable con el catálogo de módulos y lecciones ambientales
// Lista guías didácticas de separación (blanco, verde, negro) con estado completado

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

data class EducationActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onTopicClick: (EducationTopic) -> Unit = {},
    val onToggleFavorite: (EducationTopic) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun EducationScreen(state: EducationUiState, actions: EducationActions, isSignedIn: Boolean = true) {
    EcoTabScreenLayout(selectedTab = CitizenTab.EDUCACION, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.education_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.education_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        EcoTextField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            label = stringResource(R.string.education_search_label),
            placeholder = stringResource(R.string.education_search_placeholder),
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            },
        )
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
            state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(state.error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            else -> {
                if (state.isSample && isSignedIn) EcoInfoHint(text = stringResource(R.string.education_sample_hint))
                if (state.visibleTopics.isEmpty()) {
                    Text(stringResource(R.string.education_no_results), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.visibleTopics.forEach { topic ->
                        TopicCard(
                            topic = topic,
                            onClick = { actions.onTopicClick(topic) },
                            onToggleFavorite = { actions.onToggleFavorite(topic) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCard(topic: EducationTopic, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.education_open_topic), onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(24.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(topic.icon.iconRes()), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(topic.title, style = EcoTextStyles.LinkText, color = EcoTitle)
            Text(topic.summary, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EcoStatusChip(stringResource(R.string.education_reward, topic.rewardPoints), EcoAccent, EcoBlack)
                if (topic.completed) {
                    EcoStatusChip(stringResource(R.string.education_status_completed), EcoPrimary, EcoWhite)
                } else {
                    EcoStatusChip(stringResource(R.string.education_status_pending), EcoFieldBackground, EcoSubtitle)
                }
            }
        }
        FavoriteButton(favorite = topic.favorite, onClick = onToggleFavorite, cornerRadius = 10, iconSize = 16, bordered = true)
        Text("›", style = EcoTextStyles.ButtonText, color = EcoPrimary)
    }
}

@Composable
fun FavoriteButton(favorite: Boolean, onClick: () -> Unit, cornerRadius: Int, iconSize: Int, bordered: Boolean) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(if (bordered) EcoWhite else EcoFieldBackground)
            .then(if (bordered) Modifier.border(1.dp, EcoFieldBackground, shape) else Modifier)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(if (favorite) R.string.education_favorite_remove else R.string.education_favorite_add),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (favorite) R.drawable.ic_heart_filled else R.drawable.ic_heart),
            contentDescription = stringResource(if (favorite) R.string.education_favorite_remove else R.string.education_favorite_add),
            tint = Color.Unspecified,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

private fun TopicIcon.iconRes(): Int = when (this) {
    TopicIcon.LISTA -> R.drawable.ic_list
    TopicIcon.RECICLAJE -> R.drawable.ic_recycle
    TopicIcon.ESCOMBROS -> R.drawable.ic_trash
    TopicIcon.ECOPUNTOS -> R.drawable.ic_truck
}

@Preview(name = "Educación", widthDp = 393, heightDp = 900)
@Composable
private fun EducationPreview() {
    EcoRutaTheme {
        EducationScreen(
            EducationUiState(
                isLoading = false,
                topics = listOf(
                    EducationTopic(
                        id = "EDU-01",
                        title = "Cómo separar tus residuos",
                        summary = "Guía rápida del código de colores en Colombia",
                        rewardPoints = 50,
                        icon = TopicIcon.LISTA,
                        opensSeparationGuide = true,
                    ),
                ),
                isSample = true,
            ),
            EducationActions(),
        )
    }
}
