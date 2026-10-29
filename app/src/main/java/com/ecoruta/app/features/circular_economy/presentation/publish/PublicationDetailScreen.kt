package com.ecoruta.app.features.circular_economy.presentation.publish

// Detalle de una publicación de material reciclable y su estado

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

data class PublicationDetailActions(
    val onBack: () -> Unit = {},
    val onShowQr: () -> Unit = {},
    val onCallRecycler: (String) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onViewDelivery: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun PublicationDetailScreen(state: PublicationUiState, isCancelling: Boolean, actions: PublicationDetailActions) {
    val publication = (state as? PublicationUiState.Loaded)?.publication
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    val bottomActions: (@Composable ColumnScope.() -> Unit)? = when (publication?.status) {
        PublicationStatus.EN_CAMINO -> {
            { EcoPrimaryButton(text = stringResource(R.string.recycling_show_qr), onClick = actions.onShowQr, trailingIconRes = R.drawable.ic_arrow_right) }
        }
        PublicationStatus.DISPONIBLE -> {
            {
                OutlinedButton(
                    onClick = { confirmCancel = true },
                    enabled = !isCancelling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EcoTitle),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = EcoWhite, contentColor = EcoTitle),
                ) { Text(stringResource(R.string.recycling_cancel_publication), style = EcoTextStyles.ButtonText) }
            }
        }
        PublicationStatus.ENTREGADA -> {
            { EcoPrimaryButton(text = stringResource(R.string.recycling_view_delivery), onClick = actions.onViewDelivery, trailingIconRes = R.drawable.ic_arrow_right) }
        }
        else -> null
    }

    EcoTabScreenLayout(
        selectedTab = CitizenTab.HOME,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = bottomActions,
    ) {
        Text(stringResource(R.string.recycling_detail_title), style = EcoTextStyles.Title, color = EcoTitle)
        when (state) {
            PublicationUiState.Loading -> CenteredLoading()
            PublicationUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EcoErrorMessage(text = stringResource(R.string.recycling_detail_error))
                EcoTextLink(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            PublicationUiState.NotFound -> EcoErrorMessage(text = stringResource(R.string.recycling_not_found))
            is PublicationUiState.Loaded -> PublicationDetailContent(state.publication, state.isSample, actions.onCallRecycler)
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.recycling_cancel_title), style = EcoTextStyles.ButtonText, color = EcoTitle) },
            text = { Text(stringResource(R.string.recycling_cancel_message), style = EcoTextStyles.Subtitle, color = EcoSubtitle) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    actions.onCancel()
                }) { Text(stringResource(R.string.debris_cancel_confirm), style = EcoTextStyles.ButtonText, color = EcoError) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) {
                    Text(stringResource(R.string.debris_cancel_dismiss), style = EcoTextStyles.ButtonText, color = EcoPrimary)
                }
            },
            containerColor = EcoWhite,
        )
    }
}

@Composable
private fun PublicationDetailContent(publication: RecyclingPublication, isSample: Boolean, onCallRecycler: (String) -> Unit) {
    Text(stringResource(R.string.recycling_published_on, formatLong(publication.createdAtMillis)), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    PublicationStatusChip(publication.status)
    if (isSample) EcoInfoHint(text = stringResource(R.string.recycling_sample_hint))
    PublicationSummary(publication)
    val recycler = publication.recycler
    if (recycler != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoPrimary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.recycling_your_recycler), style = EcoTextStyles.FieldLabel, color = EcoWhite)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.ic_avatar_default), contentDescription = null, modifier = Modifier.size(44.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(recycler.name, style = EcoTextStyles.ButtonText, color = EcoWhite)
                    Text(recycler.affiliation, style = EcoTextStyles.Subtitle, color = EcoWhite.copy(alpha = 0.85f))
                }
            }
            val phone = recycler.phone
            if (phone != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.recycling_phone, phone), style = EcoTextStyles.Subtitle, color = EcoWhite, modifier = Modifier.weight(1f))
                    if (publication.status == PublicationStatus.EN_CAMINO) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(EcoWhite.copy(alpha = 0.15f))
                                .clickable(role = Role.Button, onClick = { onCallRecycler(phone) })
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_phone), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.recycling_call), style = EcoTextStyles.FieldLabel, color = EcoWhite)
                        }
                    }
                }
            }
            Text(
                text = when (publication.status) {
                    PublicationStatus.ENTREGADA -> stringResource(
                        R.string.recycling_received_on,
                        publication.deliveredAtMillis?.let(::formatShort) ?: "—",
                    )
                    else -> stringResource(R.string.recycling_on_the_way_text)
                },
                style = EcoTextStyles.Subtitle,
                color = EcoWhite,
            )
        }
    }
    when (publication.status) {
        PublicationStatus.DISPONIBLE -> EcoInfoHint(text = stringResource(R.string.recycling_available_hint))
        PublicationStatus.CANCELADA -> EcoInfoHint(text = stringResource(R.string.recycling_cancelled_hint))
        else -> Unit
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EcoDataLabel(stringResource(R.string.recycling_delivery_place))
        Text(stringResource(R.string.recycling_address_with_area, publication.address, publication.area), style = EcoTextStyles.Subtitle, color = EcoTitle)
        Text(stringResource(R.string.report_default_area), style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
    if (publication.status == PublicationStatus.EN_CAMINO) {
        Text(stringResource(R.string.recycling_qr_instructions), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

// Tarjeta de resumen con el tipo de material, el código identificador y el volumen estimado
@Composable
fun PublicationSummary(publication: RecyclingPublication) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(publication.materialName(), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
            EcoDataLabel(publication.code)
        }
        Text(stringResource(publication.volume.labelRes()), style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

@Preview(name = "Detalle Publicación", widthDp = 393, heightDp = 900)
@Composable
private fun PublicationDetailPreview() {
    EcoRutaTheme {
        PublicationDetailScreen(
            state = PublicationUiState.Loaded(publication = previewPublicationOnTheWay, isSample = false),
            isCancelling = false,
            actions = PublicationDetailActions(),
        )
    }
}
