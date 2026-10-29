package com.ecoruta.app.features.circular_economy.presentation.publish

// Confirmación de entrega completada de material reciclable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

@Composable
fun DeliveryCompletedScreen(
    state: PublicationUiState,
    onBackToRecycling: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.HOME,
        onBack = onBackToRecycling,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.recycling_back_to_recycling), onClick = onBackToRecycling, trailingIconRes = R.drawable.ic_arrow_right)
        },
    ) {
        val publication = (state as? PublicationUiState.Loaded)?.publication
        when {
            state is PublicationUiState.Loading -> CenteredLoading()
            publication == null || publication.status != PublicationStatus.ENTREGADA ->
                EcoErrorMessage(text = stringResource(R.string.recycling_delivery_not_confirmed))
            else -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(EcoAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(34.dp))
                    }
                    Text(stringResource(R.string.recycling_completed_title), style = EcoTextStyles.Title, color = EcoTitle, textAlign = TextAlign.Center)
                    Text(
                        stringResource(R.string.recycling_completed_description, publication.materialName()),
                        style = EcoTextStyles.Subtitle,
                        color = EcoSubtitle,
                        textAlign = TextAlign.Center,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                        .background(EcoPrimary)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.recycling_points_earned), style = EcoTextStyles.FieldLabel, color = EcoWhite)
                    Text(
                        publication.pointsEarned?.let { stringResource(R.string.recycling_points_plus, formatPoints(it)) }
                            ?: stringResource(R.string.recycling_points_pending),
                        style = EcoTextStyles.Numbers,
                        color = EcoWhite,
                    )
                    publication.balanceAfter?.let { balance ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.recycling_new_balance), style = EcoTextStyles.Subtitle, color = EcoWhite, modifier = Modifier.weight(1f))
                            Text(formatPoints(balance), style = EcoTextStyles.Numbers, color = EcoWhite)
                        }
                    }
                }
                PublicationSummary(publication)
                val recycler = publication.recycler
                Text(
                    stringResource(
                        R.string.recycling_received_by,
                        recycler?.name ?: "—",
                        publication.deliveredAtMillis?.let(::formatShort) ?: "—",
                    ),
                    style = EcoTextStyles.Subtitle,
                    color = EcoSubtitle,
                )
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.recycling_sample_hint))
            }
        }
    }
}

@Preview(name = "Entrega Completada", widthDp = 393, heightDp = 852)
@Composable
private fun DeliveryCompletedPreview() {
    EcoRutaTheme {
        DeliveryCompletedScreen(
            state = PublicationUiState.Loaded(publication = previewPublicationDelivered, isSample = false),
            onBackToRecycling = {},
            onTabClick = {},
        )
    }
}
