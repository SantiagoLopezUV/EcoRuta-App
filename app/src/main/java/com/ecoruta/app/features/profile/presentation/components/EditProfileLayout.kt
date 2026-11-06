package com.ecoruta.app.features.profile.presentation.components

// Estructura común de las pantallas para editar los datos del perfil
// Incluye la cabecera de marca, el título, el aviso de guardado y las acciones fijas abajo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleBrandHeader
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoAccentSoft
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Opacidad del botón de guardar y del enlace de cancelar mientras se guardan los cambios
private const val SavingAlpha = 0.6f

// Pantalla de edición sin barra inferior que recibe los campos de cada rol como contenido
@Composable
internal fun EditProfileLayout(
    description: String,
    isSaving: Boolean,
    isSaved: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EcoRoleBrandHeader(onBack = onBack)
            // Ruta de ubicación que indica que la edición parte de Mi perfil
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.my_profile), style = EcoTextStyles.Subtitle, color = EcoTitle)
                Text(stringResource(R.string.profile_edit_data), style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
            if (isSaved) SavedBanner()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.edit_profile_title), style = EcoTextStyles.Title, color = EcoTitle)
                Text(description, style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
            content()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mientras guarda el botón cambia su texto y se atenúa sin aceptar más toques
            EcoPrimaryButton(
                text = stringResource(if (isSaving) R.string.edit_profile_saving else R.string.merchant_edit_save),
                onClick = { if (!isSaving) onSave() },
                enabled = canSave,
                modifier = if (isSaving) Modifier.alpha(SavingAlpha) else Modifier,
            )
            EcoTextLink(
                text = stringResource(R.string.action_cancel),
                onClick = { if (!isSaving) onCancel() },
                modifier = if (isSaving) Modifier.alpha(SavingAlpha) else Modifier,
            )
        }
    }
}

// Aviso verde que confirma que los cambios se guardaron
@Composable
private fun SavedBanner() {
    val shape = RoundedCornerShape(12.dp)
    Text(
        stringResource(R.string.edit_profile_saved),
        style = EcoTextStyles.Subtitle,
        color = EcoPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoAccentSoft)
            .border(1.dp, EcoAccent, shape)
            .padding(12.dp),
    )
}
