package com.ecoruta.app.core.ui.components

// Campo interactivo para seleccionar y adjuntar archivos PDF desde el dispositivo
// Muestra el nombre y tamaño del documento seleccionado con opción de retirarlo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import java.util.Locale

// Representa un archivo PDF seleccionado en el dispositivo para adjuntar en formularios
data class PickedDocument(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long? = null,
)

// Color utilizado para el borde punteado del área de carga de documentos
private val DashedBorderColor = Color(0xFFCBD5E1)

// Campo que abre por su cuenta el selector de PDF para los formularios que guardan el documento completo
@Composable
fun EcoPdfField(
    label: String,
    hint: String,
    document: PickedDocument?,
    onPicked: (PickedDocument) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    val pickPdf = rememberPdfPicker(onPicked)
    EcoPdfField(
        label = label,
        hint = hint,
        fileName = document?.name,
        fileSize = document?.sizeBytes,
        onPickClick = pickPdf,
        onRemove = onRemove,
        modifier = modifier,
        errorMessage = errorMessage,
    )
}

// Prepara el selector de documentos del sistema filtrado a PDF y devuelve la acción que lo abre
@Composable
fun rememberPdfPicker(onPicked: (PickedDocument) -> Unit): () -> Unit {
    // En la vista previa no existe un registro de resultados, así que la acción no hace nada
    if (LocalInspectionMode.current) return {}
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPicked(readDocument(context, uri))
    }
    return remember(launcher) { { launcher.launch(arrayOf(PDF_MIME)) } }
}

// Recuadro con borde punteado que muestra el PDF adjunto y avisa cuando se toca para elegir uno
@Composable
fun EcoPdfField(
    label: String,
    hint: String,
    fileName: String?,
    onPickClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    fileSize: Long? = null,
    errorMessage: String? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoTitle)
        val borderColor = if (errorMessage != null) EcoError else DashedBorderColor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(EcoFieldBackground)
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                    )
                }
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.pdf_field_pick), onClick = onPickClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (fileName == null) {
                Text(stringResource(R.string.pdf_field_attach), style = EcoTextStyles.FieldLabel.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = EcoTitle)
                Text(hint, style = EcoTextStyles.Subtitle.copy(fontSize = 12.sp), color = EcoSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Icon(painterResource(R.drawable.ic_file_text), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(fileName, style = EcoTextStyles.FieldLabel.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = EcoTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    fileSize?.let { Text(formatSize(it), style = EcoTextStyles.Subtitle.copy(fontSize = 12.sp), color = EcoSubtitle) }
                }
                Text(
                    stringResource(R.string.pdf_field_remove),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoError,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onRemove)
                        .padding(4.dp),
                )
            }
        }
        if (errorMessage != null) EcoErrorMessage(text = errorMessage)
    }
}

private const val PDF_MIME = "application/pdf"

// Obtiene el nombre y el tamaño en bytes del documento seleccionado desde el proveedor de archivos
private fun readDocument(context: Context, uri: Uri): PickedDocument {
    var name: String? = null
    var size: Long? = null
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
    }
    return PickedDocument(uri = uri, name = name ?: uri.lastPathSegment.orEmpty(), sizeBytes = size)
}

// Formatea una cantidad de bytes en una cadena legible en kilobytes o megabytes
private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) String.format(Locale.forLanguageTag("es-CO"), "%.0f KB", kb) else String.format(Locale.forLanguageTag("es-CO"), "%.1f MB", kb / 1024)
}

@Preview(name = "Campo PDF vacío", widthDp = 354)
@Composable
private fun EcoPdfFieldEmptyPreview() {
    EcoRutaTheme {
        EcoPdfField(label = stringResource(R.string.recycler_registration_id_pdf), hint = stringResource(R.string.recycler_application_document_pdf_hint), fileName = null, onPickClick = {}, onRemove = {})
    }
}

@Preview(name = "Campo PDF cargado", widthDp = 354)
@Composable
private fun EcoPdfFieldLoadedPreview() {
    EcoRutaTheme {
        EcoPdfField(label = stringResource(R.string.recycler_registration_id_pdf), hint = stringResource(R.string.recycler_application_document_pdf_hint), fileName = "cedula_ana_torres.pdf", onPickClick = {}, onRemove = {})
    }
}

@Preview(name = "Campo PDF con error", widthDp = 354)
@Composable
private fun EcoPdfFieldErrorPreview() {
    EcoRutaTheme {
        EcoPdfField(
            label = stringResource(R.string.recycler_registration_id_pdf),
            hint = stringResource(R.string.recycler_application_document_pdf_hint),
            fileName = null,
            onPickClick = {},
            onRemove = {},
            errorMessage = stringResource(R.string.recycler_registration_error_pdf),
        )
    }
}
