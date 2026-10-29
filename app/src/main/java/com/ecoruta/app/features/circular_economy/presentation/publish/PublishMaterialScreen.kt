package com.ecoruta.app.features.circular_economy.presentation.publish

// Formulario para publicar material reciclable
// Selección de material, volumen, foto opcional y dirección

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.reports.presentation.create.rememberPhotoThumbnail

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
