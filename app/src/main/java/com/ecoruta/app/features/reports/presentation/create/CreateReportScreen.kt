package com.ecoruta.app.features.reports.presentation.create

// Componentes compartidos del flujo de reporte y primer paso de captura de fotografías
// Paso 1: Fotografías. Los pasos de afectación, ubicación y envío están en archivos propios

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoWizardLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal const val REPORT_STEPS = 3

@Composable
internal fun ReportStepLayout(
    step: Int,
    stageLabel: String,
    title: String,
    description: String,
    onBack: () -> Unit,
    bottomActions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) = EcoWizardLayout(step, REPORT_STEPS, stageLabel, title, description, onBack, bottomActions, content)

@Composable
fun ReportsTabLayout(
    selectedTab: CitizenTab,
    onBack: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = EcoTabScreenLayout(
    selectedTab = selectedTab,
    onBack = onBack,
    onTabClick = onTabClick,
    bottomActions = bottomActions,
    content = content,
)

@Composable
fun ReportStatusChip(status: ReportStatus, modifier: Modifier = Modifier) {
    val (background, content) = when (status) {
        ReportStatus.PENDIENTE -> EcoSubtitle to EcoWhite
        ReportStatus.APROBADO -> EcoAccent to EcoBlack
        ReportStatus.ATENDIDO -> EcoGreenSecondary to EcoWhite
        ReportStatus.RECHAZADO -> EcoPrimary to EcoWhite
        ReportStatus.DUPLICADO -> EcoTitle to EcoWhite
    }
    EcoStatusChip(stringResource(status.labelRes()), background, content, modifier)
}

@Composable
fun ReportPhotoPlaceholder(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    caption: String? = null,
) {
    Box(
        modifier = modifier.background(EcoFieldBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_image),
                contentDescription = null,
                tint = EcoSubtitle,
                modifier = Modifier.size(iconSize),
            )
            if (caption != null) Text(caption, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
    }
}

@Composable
fun ReportInfoHint(text: String, modifier: Modifier = Modifier) = EcoInfoHint(text, modifier)

@StringRes
fun ReportType.labelRes(): Int = when (this) {
    ReportType.CONTENEDOR_LLENO -> R.string.report_type_full_container
    ReportType.BOTADERO_CLANDESTINO -> R.string.report_type_illegal_dump
}

@StringRes
fun ReportStatus.labelRes(): Int = when (this) {
    ReportStatus.PENDIENTE -> R.string.report_status_pending
    ReportStatus.APROBADO -> R.string.report_status_approved
    ReportStatus.ATENDIDO -> R.string.report_status_attended
    ReportStatus.RECHAZADO -> R.string.report_status_rejected
    ReportStatus.DUPLICADO -> R.string.report_status_duplicate
}

fun formatReportDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-CO")).format(Date(millis))

internal fun newReportPhotoUri(context: Context): Uri {
    val directory = File(context.cacheDir, "report_photos").apply { mkdirs() }
    val file = File.createTempFile("reporte_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
fun rememberPhotoThumbnail(uri: Uri, maxSizePx: Int = 360): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { decodeThumbnail(context, uri, maxSizePx)?.asImageBitmap() }
    }
    return bitmap
}

private fun decodeThumbnail(context: Context, uri: Uri, maxSizePx: Int): Bitmap? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSizePx && bounds.outHeight / (sample * 2) >= maxSizePx) sample *= 2
    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return@runCatching null
    val rotation = resolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    if (rotation == 0f) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
}.getOrNull()

// Primer paso del formulario de reporte para captura de fotografía y descripción
data class ReportPhotoActions(
    val onPhotoAdded: (Uri) -> Unit = {},
    val onPhotoRemoved: (Uri) -> Unit = {},
    val onPhotoLimitReached: () -> Unit = {},
    val onCameraUnavailable: () -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun ReportPhotoScreen(state: NewReportUiState, actions: ReportPhotoActions) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            if (saved && uri != null) actions.onPhotoAdded(uri)
        }
    }
    val pickPhotos = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(NewReportUiState.MAX_PHOTOS),
        ) { uris ->
            val free = NewReportUiState.MAX_PHOTOS - state.photos.size
            uris.take(free).forEach(actions.onPhotoAdded)
            if (uris.size > free) actions.onPhotoLimitReached()
        }
    }
    val openCamera: () -> Unit = {
        if (!state.canAddPhoto) {
            actions.onPhotoLimitReached()
        } else {
            runCatching { newReportPhotoUri(context) }
                .onSuccess { uri ->
                    pendingCameraUri = uri
                    try {
                        takePicture?.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        pendingCameraUri = null
                        actions.onCameraUnavailable()
                    }
                }
                .onFailure { actions.onCameraUnavailable() }
        }
    }
    val openGallery: () -> Unit = {
        if (state.canAddPhoto) {
            pickPhotos?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            actions.onPhotoLimitReached()
        }
    }

    ReportStepLayout(
        step = 1,
        stageLabel = stringResource(R.string.report_stage_photo),
        title = stringResource(R.string.report_new_title),
        description = stringResource(R.string.report_photo_description),
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = actions.onContinue,
                height = 48.dp,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        CaptureBox(canAddPhoto = state.canAddPhoto, hasPhotos = state.photos.isNotEmpty(), onClick = openCamera)
        OutlinedButton(
            onClick = openGallery,
            enabled = state.canAddPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EcoBorder),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = EcoWhite, contentColor = EcoPrimary),
        ) {
            Icon(painterResource(R.drawable.ic_image), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.report_upload_from_gallery), style = EcoTextStyles.LinkText)
        }
        if (state.photos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.report_photo_count, state.photos.size, NewReportUiState.MAX_PHOTOS),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoTitle,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.photos.forEachIndexed { index, uri ->
                        PhotoThumbnail(uri = uri, index = index + 1, onRemove = { actions.onPhotoRemoved(uri) })
                    }
                }
            }
        }
        state.photoError?.let { EcoErrorMessage(text = stringResource(it)) }
        ReportInfoHint(text = stringResource(R.string.report_photo_hint))
        EcoTextField(
            value = state.description,
            onValueChange = actions.onDescriptionChange,
            label = stringResource(R.string.report_description_label),
            placeholder = stringResource(R.string.report_description_placeholder),
            supportingText = stringResource(R.string.report_description_counter, state.description.length, NewReportUiState.MAX_DESCRIPTION),
            singleLine = false,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
}

@Composable
private fun CaptureBox(canAddPhoto: Boolean, hasPhotos: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .clickable(enabled = canAddPhoto, role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ic_camera_large), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(32.dp))
        Text(stringResource(R.string.report_capture_title), style = EcoTextStyles.ButtonText.copy(fontSize = 14.sp, lineHeight = 20.sp), color = EcoTitle)
        Text(
            text = stringResource(
                when {
                    !canAddPhoto -> R.string.report_photo_max_reached
                    hasPhotos -> R.string.report_add_another_photo
                    else -> R.string.report_take_photo
                },
            ),
            style = EcoTextStyles.LinkText,
            color = if (canAddPhoto) EcoPrimary else EcoSubtitle,
        )
    }
}

@Composable
private fun PhotoThumbnail(uri: Uri, index: Int, onRemove: () -> Unit) {
    val bitmap = rememberPhotoThumbnail(uri)
    Box(Modifier.size(72.dp)) {
        val shape = RoundedCornerShape(12.dp)
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.report_photo_number, index),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
            )
        } else {
            ReportPhotoPlaceholder(Modifier.fillMaxSize().clip(shape))
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(EcoTitle.copy(alpha = 0.75f))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.report_remove_photo), onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.report_remove_photo),
                tint = Color.Unspecified,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(name = "Paso 1 Foto", widthDp = 393, heightDp = 852)
@Composable
private fun ReportPhotoPreview() {
    EcoRutaTheme {
        ReportPhotoScreen(
            state = NewReportUiState(description = "Hay bolsas acumuladas fuera del contenedor desde ayer."),
            actions = ReportPhotoActions(),
        )
    }
}
