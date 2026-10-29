package com.ecoruta.app.features.profile.presentation.components

// Pantallas de postulación para trabajar como reciclador
// Incluye la modalidad independiente o vinculada a una empresa y sus confirmaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoFocusLayout
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPdfField
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.core.ui.components.PickedDocument
import com.ecoruta.app.features.auth.presentation.register.CenteredBrand
import com.ecoruta.app.features.auth.presentation.register.RegistrationLayout
import com.ecoruta.app.features.profile.presentation.Company
import com.ecoruta.app.features.profile.presentation.RecyclerApplicationUiState
import com.ecoruta.app.features.profile.presentation.RecyclerModality

// Flujo de postulación como reciclador en modalidad independiente o vinculado
data class RecyclerApplicationActions(
    val onBack: () -> Unit = {},
    val onModalitySelected: (RecyclerModality) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onDocumentPicked: (PickedDocument) -> Unit = {},
    val onDocumentRemoved: () -> Unit = {},
    val onContinue: () -> Unit = {},
)

@Composable
fun RecyclerApplicationScreen(state: RecyclerApplicationUiState, actions: RecyclerApplicationActions) {
    val focusManager = LocalFocusManager.current
    val isLinked = state.modality == RecyclerModality.VINCULADO
    EcoFocusLayout(
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(if (isLinked) R.string.recycler_application_select_company else R.string.recycler_application_submit),
                onClick = {
                    focusManager.clearFocus()
                    actions.onContinue()
                },
                isLoading = state.isSubmitting,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycler_application_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.recycler_application_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycler_application_modality), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            RecyclerModality.entries.forEach { modality ->
                ModalityOption(
                    text = stringResource(modality.labelRes()),
                    selected = state.modality == modality,
                    onClick = { actions.onModalitySelected(modality) },
                )
            }
        }
        ReadOnlyField(stringResource(R.string.recycler_application_full_name), state.fullName, R.drawable.ic_user_white)
        ReadOnlyField(stringResource(R.string.recycler_application_document), state.documentNumber.ifBlank { "—" }, R.drawable.ic_id_card)
        EcoTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = stringResource(R.string.recycler_application_phone),
            placeholder = stringResource(R.string.store_application_phone_placeholder),
            leadingIcon = { FieldIcon(R.drawable.ic_phone) },
            errorMessage = state.phoneError?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        EcoPdfField(
            label = stringResource(R.string.recycler_application_document_pdf),
            hint = stringResource(R.string.recycler_application_document_pdf_hint),
            document = state.idDocument,
            onPicked = actions.onDocumentPicked,
            onRemove = actions.onDocumentRemoved,
            errorMessage = state.documentError?.let { stringResource(it) },
        )
        if (isLinked) EcoInfoHint(text = stringResource(R.string.recycler_application_linked_hint))
    }
}

@Composable
private fun ModalityOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(EcoFieldBackground)
            .border(if (selected) 1.dp else 1.5.dp, if (selected) EcoGreenSecondary else EcoFieldBackground, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .then(if (selected) Modifier.background(EcoGreenSecondary) else Modifier.border(1.5.dp, EcoSubtitle, CircleShape)),
        )
        Text(text, style = EcoTextStyles.LinkText, color = EcoTitle, modifier = Modifier.weight(1f))
    }
}

@Composable
fun CompanySearchScreen(
    state: RecyclerApplicationUiState,
    onQueryChange: (String) -> Unit,
    onCompanySelected: (Company) -> Unit,
    onBack: () -> Unit,
) {
    RegistrationLayout(
        stepLabel = stringResource(R.string.recycler_flow_registration),
        stageLabel = stringResource(R.string.recycler_flow_stage_company),
        progress = 0.5f,
        bottomActions = { EcoTextLink(text = stringResource(R.string.registration_back), onClick = onBack) },
    ) {
        StageTitle(stringResource(R.string.recycler_company_search_title), stringResource(R.string.recycler_company_search_description))
        EcoTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = stringResource(R.string.recycler_company_search_label),
            placeholder = stringResource(R.string.recycler_company_search_placeholder),
            leadingIcon = { FieldIcon(R.drawable.ic_search) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        when {
            state.isSearching -> Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EcoPrimary, modifier = Modifier.size(28.dp))
            }
            state.searchError != null -> EcoErrorMessage(text = stringResource(state.searchError))
            state.query.isNotBlank() && state.results.isEmpty() ->
                Text(stringResource(R.string.recycler_company_search_empty), style = EcoTextStyles.Subtitle, color = EcoTitle)
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.results.forEach { company -> CompanyCard(company = company, selected = false, onClick = { onCompanySelected(company) }) }
            }
        }
        if (state.query.isBlank()) Text(stringResource(R.string.recycler_company_search_hint), style = EcoTextStyles.Subtitle, color = EcoTitle)
        CenteredBrand(Modifier.padding(top = 48.dp))
    }
}

@Composable
fun SelectedCompanyScreen(
    state: RecyclerApplicationUiState,
    onChangeCompany: () -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    RegistrationLayout(
        stepLabel = stringResource(R.string.recycler_flow_registration),
        stageLabel = stringResource(R.string.recycler_flow_stage_company),
        progress = 1f,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.recycler_company_complete), onClick = onComplete, isLoading = state.isSubmitting)
            EcoTextLink(text = stringResource(R.string.registration_back), onClick = onBack)
        },
    ) {
        Text(stringResource(R.string.recycler_company_selected_title), style = EcoTextStyles.Title, color = EcoTitle)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycler_company_linked_label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            state.company?.let { CompanyCard(company = it, selected = true, onClick = null) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EcoTextLink(text = stringResource(R.string.recycler_company_change), onClick = onChangeCompany)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_alert_circle), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.recycler_company_must_activate), style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        CenteredBrand(Modifier.padding(top = 48.dp))
    }
}

@Composable
fun CompanyRequestSentScreen(state: RecyclerApplicationUiState, onGoHome: () -> Unit) {
    RegistrationLayout(
        stepLabel = stringResource(R.string.recycler_flow_application),
        stageLabel = stringResource(R.string.recycler_flow_stage_activation),
        progress = 1f,
        bottomActions = { EcoPrimaryButton(text = stringResource(R.string.role_request_go_home), onClick = onGoHome) },
    ) {
        StageTitle(stringResource(R.string.recycler_company_sent_title), stringResource(R.string.recycler_company_sent_description))
        if (state.submittedWithoutBackend) EcoInfoHint(text = stringResource(R.string.role_request_design_mode))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.recycler_company_pending_activation), style = EcoTextStyles.LinkText, color = EcoGreenSecondary)
        }
        state.company?.let { CompanyCard(company = it, selected = true, onClick = null) }
        Text(stringResource(R.string.recycler_company_sent_body), style = EcoTextStyles.Subtitle, color = EcoTitle)
        CenteredBrand(Modifier.padding(top = 32.dp))
    }
}

@Composable
private fun StageTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = EcoTextStyles.Title, color = EcoTitle)
        Text(description, style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

@Composable
private fun CompanyCard(company: Company, selected: Boolean, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(if (selected) 1.75.dp else 1.dp, if (selected) EcoGreenSecondary else EcoBorder, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_building), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(company.name, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(company.type, style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        Icon(
            painterResource(if (selected) R.drawable.ic_circle_check else R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = EcoGreenSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun RecyclerApplicationSentScreen(state: RecyclerApplicationUiState, onBack: () -> Unit, onGoHome: () -> Unit) {
    EcoFocusLayout(
        onBack = onBack,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.role_request_go_home), onClick = onGoHome, trailingIconRes = R.drawable.ic_arrow_right)
        },
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(EcoGreenSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(32.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycler_sent_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.recycler_sent_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        if (state.submittedWithoutBackend) EcoInfoHint(text = stringResource(R.string.role_request_design_mode))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                .background(EcoPrimary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_file_text), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.recycler_sent_modality_label), style = EcoTextStyles.FieldLabel, color = EcoWhite)
                    Text(stringResource(state.modality.labelRes()), style = EcoTextStyles.ButtonText, color = EcoWhite)
                }
            }
            HorizontalDivider(color = EcoWhite.copy(alpha = 0.6f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.recycler_sent_status_label), style = EcoTextStyles.FieldLabel, color = EcoWhite, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.recycler_sent_status),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoTitle,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EcoWhite)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.recycler_sent_operator_title), style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(stringResource(R.string.recycler_sent_operator_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.recycler_sent_notification), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
    }
}

private fun RecyclerModality.labelRes(): Int = when (this) {
    RecyclerModality.INDEPENDIENTE -> R.string.recycler_modality_independent
    RecyclerModality.VINCULADO -> R.string.recycler_modality_linked
}

private val previewCompany = Company(
    id = "comp-1",
    name = "Asociación Recicladores del Sur",
    type = "Organización de recicladores autorizada · Pasto",
)

@Preview(name = "Postular Reciclador", widthDp = 393, heightDp = 900)
@Composable
private fun RecyclerApplicationPreview() {
    EcoRutaTheme {
        RecyclerApplicationScreen(
            state = RecyclerApplicationUiState(
                fullName = "Camila Andrea Gómez",
                documentNumber = "1085324910",
                phone = "3154829103",
                modality = RecyclerModality.INDEPENDIENTE,
            ),
            actions = RecyclerApplicationActions(),
        )
    }
}

@Preview(name = "Buscar Empresa Recicladora", widthDp = 393, heightDp = 852)
@Composable
private fun CompanySearchPreview() {
    EcoRutaTheme {
        CompanySearchScreen(
            state = RecyclerApplicationUiState(
                query = "Sur",
                results = listOf(previewCompany),
            ),
            onQueryChange = {},
            onCompanySelected = {},
            onBack = {},
        )
    }
}

@Preview(name = "Empresa Recicladora Seleccionada", widthDp = 393, heightDp = 852)
@Composable
private fun SelectedCompanyPreview() {
    EcoRutaTheme {
        SelectedCompanyScreen(
            state = RecyclerApplicationUiState(company = previewCompany),
            onChangeCompany = {},
            onComplete = {},
            onBack = {},
        )
    }
}

@Preview(name = "Solicitud Empresa Enviada", widthDp = 393, heightDp = 852)
@Composable
private fun CompanyRequestSentPreview() {
    EcoRutaTheme {
        CompanyRequestSentScreen(
            state = RecyclerApplicationUiState(company = previewCompany),
            onGoHome = {},
        )
    }
}

@Preview(name = "Solicitud Reciclador Enviada", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerApplicationSentPreview() {
    EcoRutaTheme {
        RecyclerApplicationSentScreen(
            state = RecyclerApplicationUiState(modality = RecyclerModality.INDEPENDIENTE),
            onBack = {},
            onGoHome = {},
        )
    }
}
