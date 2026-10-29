package com.ecoruta.app.features.auth.presentation.register

// Paso de datos personales del ciudadano en el registro

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoCheckboxRow
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

private val DocumentType.shortLabelRes: Int
    @StringRes get() = when (this) {
        DocumentType.CEDULA_CIUDADANIA -> R.string.document_cc_short
        DocumentType.TARJETA_IDENTIDAD -> R.string.document_ti_short
        DocumentType.CEDULA_EXTRANJERIA -> R.string.document_ce_short
        DocumentType.PERMISO_PROTECCION_TEMPORAL -> R.string.document_ppt_short
        DocumentType.PASAPORTE -> R.string.document_passport_short
    }

private val DocumentType.labelRes: Int
    @StringRes get() = when (this) {
        DocumentType.CEDULA_CIUDADANIA -> R.string.document_cc
        DocumentType.TARJETA_IDENTIDAD -> R.string.document_ti
        DocumentType.CEDULA_EXTRANJERIA -> R.string.document_ce
        DocumentType.PERMISO_PROTECCION_TEMPORAL -> R.string.document_ppt
        DocumentType.PASAPORTE -> R.string.document_passport
    }

data class CitizenProfileActions(
    val onDocumentTypeChange: (DocumentType) -> Unit = {},
    val onDocumentNumberChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onNeighborhoodChange: (String) -> Unit = {},
    val onDataPolicyChange: (Boolean) -> Unit = {},
    val onViewPolicyClick: () -> Unit = {},
    val onCreateAccount: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun CitizenProfileScreen(
    state: CitizenRegistrationUiState,
    actions: CitizenProfileActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_2_of_2),
        stageLabel = stringResource(R.string.registration_stage_profile),
        progress = 1f,
        modifier = modifier,
        bottomActions = {
            EcoCheckboxRow(
                checked = state.acceptedDataPolicy,
                onCheckedChange = actions.onDataPolicyChange,
                text = stringResource(R.string.registration_policy_consent),
                linkText = stringResource(R.string.registration_view_policy),
                onLinkClick = actions.onViewPolicyClick,
            )
            EcoPrimaryButton(
                text = stringResource(R.string.registration_create_account),
                onClick = {
                    focusManager.clearFocus()
                    actions.onCreateAccount()
                },
                enabled = state.acceptedDataPolicy,
                isLoading = state.isCreatingAccount,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            EcoTextLink(text = stringResource(R.string.registration_back), onClick = actions.onBack)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_profile_title),
            description = stringResource(R.string.registration_profile_description),
        )
        EcoTextField(
            value = state.documentNumber,
            onValueChange = actions.onDocumentNumberChange,
            label = stringResource(R.string.registration_document_label),
            placeholder = stringResource(R.string.registration_document_placeholder),
            leadingIcon = {
                DocumentTypeSelector(selected = state.documentType, onSelected = actions.onDocumentTypeChange)
            },
            errorMessage = state.documentErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (state.documentType == DocumentType.PASAPORTE) KeyboardType.Ascii else KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        )
        EcoTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = stringResource(R.string.registration_phone_label),
            placeholder = stringResource(R.string.registration_phone_placeholder),
            errorMessage = state.phoneErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.neighborhood,
            onValueChange = actions.onNeighborhoodChange,
            label = stringResource(R.string.registration_neighborhood_label),
            placeholder = stringResource(R.string.registration_neighborhood_placeholder),
            errorMessage = state.neighborhoodErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}

@Composable
private fun DocumentTypeSelector(
    selected: DocumentType,
    onSelected: (DocumentType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectorLabel = stringResource(R.string.registration_document_type_selector)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.DropdownList, onClickLabel = selectorLabel) { expanded = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(selected.shortLabelRes),
                    style = EcoTextStyles.FieldLabel.copy(fontSize = 13.sp, letterSpacing = 0.sp),
                    color = EcoTitle,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = EcoSubtitle,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(90f),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DocumentType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(stringResource(type.labelRes), style = EcoTextStyles.Subtitle, color = EcoTitle) },
                        onClick = {
                            expanded = false
                            onSelected(type)
                        },
                    )
                }
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .height(24.dp)
                .background(EcoBorder),
        )
    }
}

@Preview(name = "Paso 2 Perfil", widthDp = 393, heightDp = 852)
@Composable
private fun CitizenProfilePreview() {
    EcoRutaTheme {
        CitizenProfileScreen(
            state = CitizenRegistrationUiState(
                documentType = DocumentType.CEDULA_CIUDADANIA,
                documentNumber = "1085324910",
                phone = "3154829103",
                neighborhood = "Torobajo",
                acceptedDataPolicy = true,
            ),
            actions = CitizenProfileActions(),
        )
    }
}
