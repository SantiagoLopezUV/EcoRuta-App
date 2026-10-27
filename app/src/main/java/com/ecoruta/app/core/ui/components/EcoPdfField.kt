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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
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

// Recuadro con borde punteado que abre el selector de documentos del sistema filtrado a PDF
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
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val picker = if (inPreview) {
        null
    } else {
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onPicked(readDocument(context, uri))
        }
    }
    val shape = RoundedCornerShape(10.dp)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = EcoTextStyles.EtiquetaCampo, color = EcoTitle)
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
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.pdf_field_pick)) { picker?.launch(arrayOf(PDF_MIME)) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (document == null) {
                Text(stringResource(R.string.pdf_field_attach), style = EcoTextStyles.EtiquetaCampo.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = EcoTitle)
                Text(hint, style = EcoTextStyles.Subtitulo.copy(fontSize = 12.sp), color = EcoSubtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Icon(painterResource(R.drawable.ic_file_text), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(document.name, style = EcoTextStyles.EtiquetaCampo.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = EcoTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    document.sizeBytes?.let { Text(formatSize(it), style = EcoTextStyles.Subtitulo.copy(fontSize = 12.sp), color = EcoSubtitle) }
                }
                Text(
                    stringResource(R.string.pdf_field_remove),
                    style = EcoTextStyles.EtiquetaCampo,
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
