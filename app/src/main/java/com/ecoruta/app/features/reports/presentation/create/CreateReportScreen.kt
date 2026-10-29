package com.ecoruta.app.features.reports.presentation.create

// Vista composable para el formulario guiado de reporte en 3 pasos
// Paso 1: Fotografías. Paso 2: Afectación. Paso 3: Ubicación en mapa y dirección

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPermissionKind
import com.ecoruta.app.core.ui.components.EcoPermissionScreen
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSelectionCard
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.EcoTopBar
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

// Segundo paso del formulario de reporte para seleccionar el tipo de afectación
data class ReportTypeActions(
    val onTypeSelected: (ReportType) -> Unit = {},
    val onSpecialWasteClick: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun ReportTypeScreen(state: NewReportUiState, actions: ReportTypeActions) {
    ReportStepLayout(
        step = 2,
        stageLabel = stringResource(R.string.report_stage_type),
        title = stringResource(R.string.report_type_title),
        description = stringResource(R.string.report_type_description),
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
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EcoSelectionCard(
                title = stringResource(R.string.report_type_full_container),
                description = stringResource(R.string.report_type_full_container_description),
                iconRes = R.drawable.ic_trash,
                iconSize = 24.dp,
                selected = state.type == ReportType.CONTENEDOR_LLENO,
                onClick = { actions.onTypeSelected(ReportType.CONTENEDOR_LLENO) },
            )
            EcoSelectionCard(
                title = stringResource(R.string.report_type_illegal_dump),
                description = stringResource(R.string.report_type_illegal_dump_description),
                iconRes = R.drawable.ic_report_alert,
                iconSize = 24.dp,
                selected = state.type == ReportType.BOTADERO_CLANDESTINO,
                onClick = { actions.onTypeSelected(ReportType.BOTADERO_CLANDESTINO) },
            )
        }
        state.typeError?.let { EcoErrorMessage(text = stringResource(it)) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoFieldBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.report_special_waste_hint), style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
            EcoTextLink(
                text = stringResource(R.string.report_special_waste_link),
                onClick = actions.onSpecialWasteClick,
                modifier = Modifier.padding(start = 30.dp),
            )
        }
    }
}

// Tercer paso del formulario de reporte para indicar la ubicación en el mapa y dirección
data class ReportLocationActions(
    val onUseCurrentLocation: () -> Unit = {},
    val onLocationPermissionDenied: () -> Unit = {},
    val onEditAddress: () -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onAddressDone: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun ReportLocationScreen(state: NewReportUiState, actions: ReportLocationActions) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val permissionLauncher = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) actions.onUseCurrentLocation() else actions.onLocationPermissionDenied()
        }
    }
    var showLocationRationale by rememberSaveable { mutableStateOf(false) }
    val requestLocation: () -> Unit = {
        if (context.hasLocationPermission()) actions.onUseCurrentLocation() else showLocationRationale = true
    }
    if (showLocationRationale) {
        EcoPermissionScreen(
            kind = EcoPermissionKind.UBICACION,
            onAllow = {
                showLocationRationale = false
                permissionLauncher?.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
            onBack = { showLocationRationale = false },
        )
        return
    }
    if (!inPreview) {
        LaunchedEffect(Unit) {
            if (state.address.isBlank() && !state.hasGpsLocation && context.hasLocationPermission()) actions.onUseCurrentLocation()
        }
    }

    ReportStepLayout(
        step = 3,
        stageLabel = stringResource(R.string.report_stage_location),
        title = stringResource(R.string.report_location_title),
        description = stringResource(R.string.report_location_description),
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(if (state.isSubmitting) R.string.report_sending else R.string.report_send),
                onClick = actions.onSubmit,
                isLoading = state.isSubmitting,
                enabled = !state.isLocating,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IllustrativeMap(
                hasLocation = state.hasGpsLocation,
                isLocating = state.isLocating,
                onMyLocationClick = requestLocation,
            )
            Text(stringResource(R.string.report_location_helper), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            OutlinedButton(
                onClick = requestLocation,
                enabled = !state.isLocating && !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EcoBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = EcoWhite, contentColor = EcoPrimary),
            ) {
                if (state.isLocating) {
                    CircularProgressIndicator(color = EcoPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(if (state.isLocating) R.string.report_locating else R.string.report_use_current_location),
                    style = EcoTextStyles.LinkText,
                )
            }
            if (state.isEditingAddress) {
                EcoTextField(
                    value = state.address,
                    onValueChange = actions.onAddressChange,
                    label = stringResource(R.string.report_address_label),
                    placeholder = stringResource(R.string.report_address_placeholder),
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { actions.onAddressDone() }),
                )
                if (state.address.isNotBlank()) {
                    EcoTextLink(text = stringResource(R.string.report_address_save), onClick = actions.onAddressDone)
                }
            } else if (state.address.isNotBlank()) {
                AddressCard(state = state, onEdit = actions.onEditAddress)
            }
            state.locationError?.let { EcoErrorMessage(text = stringResource(it)) }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
private fun AddressCard(state: NewReportUiState, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EcoFieldBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(state.address, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(
                state.area.ifBlank { stringResource(R.string.report_default_area) },
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
            )
            if (state.latitude != null && state.longitude != null) {
                Text(
                    stringResource(R.string.report_gps_coordinates, state.latitude, state.longitude),
                    style = EcoTextStyles.FieldLabel.copy(fontSize = 11.sp),
                    color = EcoSubtitle,
                )
            }
        }
        EcoTextLink(text = stringResource(R.string.action_edit), onClick = onEdit)
    }
}

private val MapBlocks = listOf(
    floatArrayOf(18f, 12f, 74f, 46f), floatArrayOf(115f, 12f, 74f, 46f), floatArrayOf(214f, 12f, 90f, 46f),
    floatArrayOf(18f, 80f, 74f, 48f), floatArrayOf(115f, 80f, 74f, 48f),
    floatArrayOf(18f, 151f, 74f, 48f), floatArrayOf(115f, 151f, 74f, 48f), floatArrayOf(215f, 151f, 90f, 48f),
)

@Composable
private fun IllustrativeMap(hasLocation: Boolean, isLocating: Boolean, onMyLocationClick: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(212.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground),
    ) {
        val scale = maxWidth / 350f
        MapBlocks.forEach { (x, y, w, h) ->
            Box(
                Modifier
                    .offset(x = scale * x, y = y.dp)
                    .size(width = scale * w, height = h.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(EcoWhite),
            )
        }
        Box(
            Modifier
                .offset(x = scale * 215f, y = 80.dp)
                .size(width = scale * 90f, height = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(EcoGreenSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.report_map_park),
                style = EcoTextStyles.FieldLabel,
                color = EcoWhite
            )
        }
        Text(
            stringResource(R.string.report_map_street_a),
            style = EcoTextStyles.FieldLabel,
            color = EcoSubtitle,
            modifier = Modifier.offset(x = scale * 21f, y = 60.dp)
        )
        Text(
            stringResource(R.string.report_map_street_b),
            style = EcoTextStyles.FieldLabel,
            color = EcoSubtitle,
            modifier = Modifier.offset(x = scale * 22f, y = 131.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .clip(CircleShape)
                .background(if (hasLocation) EcoPrimary else EcoSubtitle),
            contentAlignment = Alignment.Center,
        ) {
            if (isLocating) {
                CircularProgressIndicator(
                    color = EcoWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_map_pin_white),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.report_map_illustrative),
            style = EcoTextStyles.NavigationLabel,
            color = EcoSubtitle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EcoWhite)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(EcoWhite)
                .border(1.dp, EcoPrimary, CircleShape)
                .clickable(
                    enabled = !isLocating,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.report_use_current_location),
                    onClick = onMyLocationClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_map_pin),
                contentDescription = stringResource(R.string.report_use_current_location),
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// Pantalla de confirmación tras enviar el reporte ciudadano
@Composable
fun ReportSentScreen(
    sentWithoutBackend: Boolean,
    onViewMyReports: () -> Unit,
    onGoHome: () -> Unit,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoHome)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        EcoTopBar(onBackClick = onGoHome)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(56.dp))
            }
            Text(stringResource(R.string.report_sent_title), style = EcoTextStyles.Title, color = EcoTitle, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.report_sent_description),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
                textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.report_current_status), style = EcoTextStyles.FieldLabel, color = EcoTitle)
                ReportStatusChip(ReportStatus.PENDIENTE)
            }
            if (sentWithoutBackend) {
                ReportInfoHint(text = stringResource(R.string.report_design_mode_sent_hint))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            EcoPrimaryButton(
                text = stringResource(R.string.report_view_my_reports),
                onClick = onViewMyReports,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            EcoTextLink(text = stringResource(R.string.report_back_home), onClick = onGoHome)
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

@Preview(name = "Paso 2 Tipo", widthDp = 393, heightDp = 852)
@Composable
private fun ReportTypePreview() {
    EcoRutaTheme {
        ReportTypeScreen(
            state = NewReportUiState(type = ReportType.CONTENEDOR_LLENO),
            actions = ReportTypeActions(),
        )
    }
}

@Preview(name = "Paso 3 Ubicación", widthDp = 393, heightDp = 852)
@Composable
private fun ReportLocationPreview() {
    EcoRutaTheme {
        ReportLocationScreen(
            state = NewReportUiState(
                address = "Cra. 22E #6-40",
                area = "Torobajo · Pasto",
                latitude = 1.2136,
                longitude = -77.2811,
                isEditingAddress = false,
            ),
            actions = ReportLocationActions(),
        )
    }
}

@Preview(name = "Reporte Enviado", widthDp = 393, heightDp = 852)
@Composable
private fun ReportSentPreview() {
    EcoRutaTheme {
        ReportSentScreen(
            sentWithoutBackend = false,
            onViewMyReports = {},
            onGoHome = {},
        )
    }
}
