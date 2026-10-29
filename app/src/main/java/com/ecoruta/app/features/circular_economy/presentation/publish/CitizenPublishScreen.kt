package com.ecoruta.app.features.circular_economy.presentation.publish

// Vista composable para que el ciudadano publique material reciclable
// Formulario de selección de material, volumen, foto opcional y dirección

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoFocusLayout
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.GuestServiceActions
import com.ecoruta.app.core.ui.components.GuestServiceContent
import com.ecoruta.app.core.ui.components.GuestServiceScreen
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.reports.presentation.create.rememberPhotoThumbnail
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SpanishColombia: Locale = Locale.forLanguageTag("es-CO")

private fun formatPoints(points: Int): String = NumberFormat.getIntegerInstance(SpanishColombia).format(points)

@StringRes
fun MaterialType.labelRes(): Int = when (this) {
    MaterialType.PET -> R.string.material_pet
    MaterialType.CARTON -> R.string.material_cardboard
    MaterialType.VIDRIO -> R.string.material_glass
    MaterialType.OTRO -> R.string.material_other
}

@StringRes
fun MaterialType.hintRes(): Int = when (this) {
    MaterialType.PET -> R.string.material_hint_pet
    MaterialType.CARTON -> R.string.material_hint_cardboard
    MaterialType.VIDRIO -> R.string.material_hint_glass
    MaterialType.OTRO -> R.string.material_hint_other
}

@StringRes
fun MaterialVolume.labelRes(): Int = when (this) {
    MaterialVolume.BOLSA_PEQUENA -> R.string.volume_small_bag
    MaterialVolume.BOLSA_GRANDE -> R.string.volume_large_bag
    MaterialVolume.VOLUMINOSO -> R.string.volume_bulky
}

// Devuelve el nombre del material seleccionado o la descripción personalizada cuando se elige otro tipo
@Composable
fun RecyclingPublication.materialName(): String =
    otherMaterial?.takeIf { material == MaterialType.OTRO && it.isNotBlank() } ?: stringResource(material.labelRes())

// Etiqueta de estado para las publicaciones de reciclaje según su avance actual
@Composable
fun PublicationStatusChip(status: PublicationStatus, modifier: Modifier = Modifier) {
    val (label, background, content) = when (status) {
        PublicationStatus.DISPONIBLE -> Triple(R.string.publication_status_available, EcoSubtitle, EcoWhite)
        PublicationStatus.EN_CAMINO -> Triple(R.string.publication_status_on_the_way, EcoAccent, EcoBlack)
        PublicationStatus.ENTREGADA -> Triple(R.string.publication_status_delivered, EcoGreenSecondary, EcoWhite)
        PublicationStatus.CANCELADA -> Triple(R.string.publication_status_cancelled, EcoTitle, EcoWhite)
    }
    EcoStatusChip(stringResource(label), background, content, modifier)
}

// Formatea una marca de tiempo en fecha corta con día, mes abreviado y año
fun formatShort(millis: Long): String = SimpleDateFormat("d MMM yyyy", SpanishColombia).format(Date(millis))

// Formatea una marca de tiempo en fecha completa con día, mes y año
fun formatLong(millis: Long): String = SimpleDateFormat("d 'de' MMMM 'de' yyyy", SpanishColombia).format(Date(millis))

// Botón de selección única para elegir el tipo de material o el volumen estimado
@Composable
fun ChoiceOption(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) EcoWhite else EcoFieldBackground)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) EcoAccent else EcoBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) EcoAccent else EcoSubtitle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(EcoAccent),
                )
            }
        }
        Text(text, style = EcoTextStyles.LinkText, color = EcoTitle)
    }
}

// Contenedor de enfoque sin barra de navegación inferior para el formulario y el código de entrega
@Composable
fun FocusLayout(
    onBack: () -> Unit,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = EcoFocusLayout(onBack = onBack, bottomActions = bottomActions, content = content)

// Indica si la publicación continúa activa para contabilizarla en el resumen superior
val RecyclingPublication.isOngoing: Boolean
    get() = status == PublicationStatus.DISPONIBLE || status == PublicationStatus.EN_CAMINO

@Composable
private fun CenteredLoading() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = EcoPrimary) }
}

data class RecyclingHomeActions(
    val onBack: () -> Unit = {},
    val onPublish: () -> Unit = {},
    val onPublicationClick: (RecyclingPublication) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Pantalla principal del módulo de reciclaje con saldo de puntos y listado de publicaciones
@Composable
fun RecyclingHomeScreen(state: RecyclingHomeUiState, actions: RecyclingHomeActions) {
    EcoTabScreenLayout(selectedTab = CitizenTab.HOME, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        val loaded = state as? RecyclingHomeUiState.Loaded
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                .background(EcoPrimary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.service_recycling_title), style = EcoTextStyles.Title, color = EcoWhite)
            Text(stringResource(R.string.recycling_description), style = EcoTextStyles.Subtitle, color = EcoWhite)
            Row {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(loaded?.ecoPoints?.let(::formatPoints) ?: "—", style = EcoTextStyles.Numbers, color = EcoWhite)
                    Text(stringResource(R.string.recycling_current_points), style = EcoTextStyles.FieldLabel, color = EcoWhite)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(loaded?.publications?.count { it.isOngoing }?.toString() ?: "—", style = EcoTextStyles.Numbers, color = EcoWhite)
                    Text(stringResource(R.string.recycling_active_publications), style = EcoTextStyles.FieldLabel, color = EcoWhite)
                }
            }
        }
        EcoPrimaryButton(text = stringResource(R.string.recycling_publish), onClick = actions.onPublish, trailingIconRes = R.drawable.ic_arrow_right)
        Text(stringResource(R.string.recycling_my_publications), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.padding(top = 4.dp))
        when (state) {
            RecyclingHomeUiState.Loading -> CenteredLoading()
            RecyclingHomeUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EcoErrorMessage(text = stringResource(R.string.recycling_load_error))
                EcoTextLink(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            is RecyclingHomeUiState.Loaded -> {
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.recycling_sample_hint))
                if (state.publications.isEmpty()) {
                    Text(stringResource(R.string.recycling_empty), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.publications.forEach { publication ->
                        PublicationCard(publication, onClick = { actions.onPublicationClick(publication) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicationCard(publication: RecyclingPublication, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.recycling_view_detail), onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_recycle), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
            Text(publication.materialName(), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
            PublicationStatusChip(publication.status)
        }
        Text(stringResource(publication.volume.labelRes()), style = EcoTextStyles.Subtitle, color = EcoTitle)
        Row(verticalAlignment = Alignment.CenterVertically) {
            EcoDataLabel(
                stringResource(R.string.recycling_date_code, formatShort(publication.createdAtMillis), publication.code),
                Modifier.weight(1f),
            )
            Text(stringResource(R.string.recycling_view_detail_arrow), style = EcoTextStyles.LinkText, color = EcoPrimary)
        }
    }
}

data class PublishMaterialActions(
    val onBack: () -> Unit = {},
    val onMaterialSelected: (MaterialType) -> Unit = {},
    val onOtherMaterialChange: (String) -> Unit = {},
    val onVolumeSelected: (MaterialVolume) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onPhotoSelected: (Uri) -> Unit = {},
    val onPhotoRemoved: () -> Unit = {},
    val onPublish: () -> Unit = {},
)

@Composable
fun PublishMaterialScreen(state: PublishMaterialUiState, actions: PublishMaterialActions) {
    val inPreview = LocalInspectionMode.current
    val pickPhoto = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) actions.onPhotoSelected(uri)
        }
    }
    val openGallery: () -> Unit = { pickPhoto?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    FocusLayout(
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.recycling_publish),
                onClick = actions.onPublish,
                isLoading = state.isPublishing,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycling_publish), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.recycling_publish_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycling_material_label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            MaterialType.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { material ->
                        ChoiceOption(
                            text = stringResource(material.labelRes()),
                            selected = state.material == material,
                            onClick = { actions.onMaterialSelected(material) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (state.material == MaterialType.OTRO) {
            EcoTextField(
                value = state.otherMaterial,
                onValueChange = actions.onOtherMaterialChange,
                label = stringResource(R.string.recycling_other_label),
                placeholder = stringResource(R.string.recycling_other_placeholder),
                errorMessage = state.otherMaterialError?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycling_volume_label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            MaterialVolume.entries.forEach { volume ->
                ChoiceOption(
                    text = stringResource(volume.labelRes()),
                    selected = state.volume == volume,
                    onClick = { actions.onVolumeSelected(volume) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.volumeError?.let { EcoErrorMessage(text = stringResource(it)) }
        }
        EcoTextField(
            value = state.address,
            onValueChange = actions.onAddressChange,
            label = stringResource(R.string.recycling_address_label),
            placeholder = stringResource(R.string.debris_address_placeholder),
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            },
            errorMessage = state.addressError?.let { stringResource(it) },
            supportingText = stringResource(R.string.report_default_area),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycling_photo_label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            val photo = state.photo
            if (photo == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EcoFieldBackground)
                        .clickable(role = Role.Button, onClick = openGallery)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.recycling_add_photo), style = EcoTextStyles.LinkText, color = EcoPrimary)
                        Text(stringResource(R.string.recycling_add_photo_hint), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                    }
                }
            } else {
                PhotoPreview(uri = photo, onChange = openGallery, onRemove = actions.onPhotoRemoved)
            }
        }
        Text(stringResource(state.material.hintRes()), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

@Composable
private fun PhotoPreview(uri: Uri, onChange: () -> Unit, onRemove: () -> Unit) {
    val bitmap = rememberPhotoThumbnail(uri)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EcoFieldBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = stringResource(R.string.recycling_photo_label), contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp))
            } else {
                CircularProgressIndicator(color = EcoPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EcoTextLink(text = stringResource(R.string.recycling_change_photo), onClick = onChange)
            EcoTextLink(text = stringResource(R.string.report_remove_photo), onClick = onRemove)
        }
    }
}

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

// Vista informativa del servicio de reciclaje para usuarios que aún no han iniciado sesión
@Composable
fun RecyclingGuestScreen(actions: GuestServiceActions) {
    GuestServiceScreen(
        content = GuestServiceContent(
            banner = R.string.recycling_guest_banner,
            lockedSection = R.string.recycling_guest_locked,
            title = R.string.service_recycling_title,
            description = R.string.service_recycling_description,
            benefits = listOf(R.string.recycling_guest_can_1, R.string.recycling_guest_can_2, R.string.recycling_guest_can_3),
        ),
        actions = actions,
    )
}

private val previewPublicationOnTheWay = RecyclingPublication(
    id = "pub-1",
    code = "ECO-REC-3041",
    material = MaterialType.PET,
    volume = MaterialVolume.BOLSA_GRANDE,
    address = "Cra. 22E #6-40",
    area = "Torobajo",
    status = PublicationStatus.EN_CAMINO,
    createdAtMillis = 1_744_800_000_000L,
    recycler = AssignedRecycler(
        name = "Luis Fernando Córdoba",
        affiliation = "Asociación Recicladores del Sur",
        phone = "3167204918",
    ),
)

private val previewPublicationDelivered = previewPublicationOnTheWay.copy(
    id = "pub-2",
    code = "ECO-REC-2980",
    material = MaterialType.CARTON,
    status = PublicationStatus.ENTREGADA,
    deliveredAtMillis = 1_744_850_000_000L,
    pointsEarned = 80,
    balanceAfter = 1_330,
)

@Preview(name = " Inicio", widthDp = 393, heightDp = 900)
@Composable
private fun RecyclingHomePreview() {
    EcoRutaTheme {
        RecyclingHomeScreen(
            RecyclingHomeUiState.Loaded(
                ecoPoints = 1_250,
                publications = listOf(previewPublicationOnTheWay, previewPublicationDelivered),
                isSample = false,
            ),
            RecyclingHomeActions(),
        )
    }
}

@Preview(name = "Publicar Material", widthDp = 393, heightDp = 900)
@Composable
private fun PublishMaterialPreview() {
    EcoRutaTheme {
        PublishMaterialScreen(
            state = PublishMaterialUiState(
                material = MaterialType.PET,
                volume = MaterialVolume.BOLSA_GRANDE,
                address = "Cra. 22E #6-40 · Torobajo",
            ),
            actions = PublishMaterialActions(),
        )
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

@Preview(name = "Reciclaje sin sesión", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclingGuestPreview() {
    EcoRutaTheme {
        RecyclingGuestScreen(actions = GuestServiceActions())
    }
}
