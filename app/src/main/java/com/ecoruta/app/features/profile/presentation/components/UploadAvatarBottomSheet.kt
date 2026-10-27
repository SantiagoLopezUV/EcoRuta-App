package com.ecoruta.app.features.profile.presentation.components

// Hoja inferior composable para selección y captura de foto de perfil
// Opciones para tomar fotografía con cámara o seleccionar desde galería

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import java.io.File

// Hoja inferior para tomar foto con la cámara, elegir de la galería o eliminar la foto actual
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoSheet(
    hasPhoto: Boolean,
    onPhotoSelected: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onCameraUnavailable: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            if (saved && uri != null) onPhotoSelected(uri)
            onDismiss()
        }
    }
    val pickPhoto = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onPhotoSelected(uri)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = EcoWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.profile_photo_sheet_title), style = EcoTextStyles.Boton.copy(fontSize = 20.sp), color = EcoTitle)
                Text(stringResource(R.string.profile_photo_sheet_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
            }
            SheetOption(stringResource(R.string.profile_photo_take)) {
                runCatching { newProfilePhotoUri(context) }
                    .onSuccess { uri ->
                        pendingCameraUri = uri
                        try {
                            takePicture?.launch(uri)
                        } catch (_: ActivityNotFoundException) {
                            pendingCameraUri = null
                            onCameraUnavailable()
                        }
                    }
                    .onFailure { onCameraUnavailable() }
            }
            SheetOption(stringResource(R.string.profile_photo_gallery)) {
                pickPhoto?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            if (hasPhoto) {
                SheetOption(stringResource(R.string.profile_photo_remove), color = EcoError) {
                    onRemovePhoto()
                    onDismiss()
                }
            }
            Text(
                stringResource(R.string.action_cancel),
                style = EcoTextStyles.Boton.copy(fontSize = 14.sp),
                color = EcoSubtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

// Opción seleccionable dentro de la hoja inferior con borde personalizado
@Composable
private fun SheetOption(text: String, color: Color = EcoTitle, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        style = EcoTextStyles.Boton.copy(fontSize = 15.sp),
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, if (color == EcoError) EcoError else EcoBorder, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    )
}

// Crea un archivo temporal en caché donde la cámara guarda la foto capturada
private fun newProfilePhotoUri(context: Context): Uri {
    val directory = File(context.cacheDir, "profile_photos").apply { mkdirs() }
    val file = File.createTempFile("perfil_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
