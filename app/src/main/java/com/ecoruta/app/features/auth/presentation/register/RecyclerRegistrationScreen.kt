package com.ecoruta.app.features.auth.presentation.register

// Pantallas de registro del reciclador independiente y del reciclador vinculado a una empresa
// Incluye el formulario de datos, la confirmación por correo y los estados finales del registro

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPdfField
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.PickedDocument
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.profile.presentation.Company
import com.ecoruta.app.features.profile.presentation.components.CompanyCard

// Acciones disponibles en el formulario de registro del reciclador
data class RecyclerRegistrationActions(
    val onFullNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onDocumentTypeChange: (DocumentType) -> Unit = {},
    val onDocumentNumberChange: (String) -> Unit = {},
    val onIdDocumentPicked: (PickedDocument) -> Unit = {},
    val onIdDocumentRemoved: () -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onGoogleClick: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
)

// Formulario de datos del reciclador; el tipo define los textos y el orden de los campos del diseño
@Composable
fun RecyclerRegistrationFormScreen(
    type: RegistrationType,
    state: RecyclerRegistrationUiState,
    actions: RecyclerRegistrationActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val isIndependent = type == RegistrationType.INDEPENDENT_RECYCLER
    val submit = {
        focusManager.clearFocus()
        actions.onSubmit()
    }

    RegistrationLayout(
        stepLabel = stringResource(if (isIndependent) R.string.registration_step_1_of_2 else R.string.recycler_flow_registration),
        stageLabel = stringResource(if (isIndependent) R.string.recycler_registration_stage_form else R.string.registration_stage_account),
        progress = if (isIndependent) 0.375f else 0.5f,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(if (isIndependent) R.string.registration_create_account else R.string.recycler_application_select_company),
                onClick = submit,
                isLoading = state.isCreatingAccount,
                trailingIconRes = if (isIndependent) null else R.drawable.ic_arrow_right,
            )
            EcoTextLink(
                text = stringResource(if (isIndependent) R.string.recycler_registration_back_to_selection else R.string.registration_change_type),
                onClick = actions.onBack,
            )
        },
    ) {
        if (isIndependent) {
            Text(stringResource(R.string.recycler_registration_form_title), style = EcoTextStyles.Title, color = EcoTitle)
        } else {
            RegistrationTitle(
                title = stringResource(R.string.recycler_registration_account_title),
                description = stringResource(R.string.recycler_registration_account_description),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(if (isIndependent) 12.dp else 16.dp)) {
            EcoTextField(
                value = state.fullName,
                onValueChange = actions.onFullNameChange,
                label = stringResource(R.string.registration_full_name_label),
                placeholder = stringResource(R.string.registration_full_name_placeholder),
                errorMessage = state.fullNameErrorRes?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            EcoTextField(
                value = state.email,
                onValueChange = actions.onEmailChange,
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_email_placeholder),
                errorMessage = state.emailErrorRes?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
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
            if (isIndependent) {
                RecyclerIdDocumentField(state = state, actions = actions)
                RecyclerPhoneField(state = state, actions = actions, onDone = submit)
            } else {
                RecyclerPhoneField(state = state, actions = actions, onDone = { focusManager.clearFocus() })
                RecyclerIdDocumentField(state = state, actions = actions)
            }
        }
        if (isIndependent) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
                Text(stringResource(R.string.registration_or_email), style = EcoTextStyles.LinkText, color = EcoTitle)
                HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
            }
            EcoSecondaryButton(
                text = stringResource(R.string.login_google),
                onClick = actions.onGoogleClick,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}

// Campo de teléfono compartido por ambos formularios de reciclador
@Composable
private fun RecyclerPhoneField(
    state: RecyclerRegistrationUiState,
    actions: RecyclerRegistrationActions,
    onDone: () -> Unit,
) {
    EcoTextField(
        value = state.phone,
        onValueChange = actions.onPhoneChange,
        label = stringResource(R.string.registration_phone_label),
        placeholder = stringResource(R.string.registration_phone_placeholder),
        errorMessage = state.phoneErrorRes?.let { stringResource(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

// Campo para adjuntar la cédula en PDF compartido por ambos formularios de reciclador
@Composable
private fun RecyclerIdDocumentField(
    state: RecyclerRegistrationUiState,
    actions: RecyclerRegistrationActions,
) {
    EcoPdfField(
        label = stringResource(R.string.recycler_registration_id_pdf),
        hint = stringResource(R.string.recycler_application_document_pdf_hint),
        document = state.idDocument,
        onPicked = actions.onIdDocumentPicked,
        onRemove = actions.onIdDocumentRemoved,
        errorMessage = state.idDocumentErrorRes?.let { stringResource(it) },
    )
}

// Pantalla que indica que se envió el correo de confirmación del registro
@Composable
fun RecyclerEmailConfirmationScreen(
    type: RegistrationType,
    email: String,
    onResendClick: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onExit)
    }
    val isIndependent = type == RegistrationType.INDEPENDENT_RECYCLER
    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_2_of_2),
        stageLabel = stringResource(R.string.registration_stage_confirmation),
        progress = 0.57f,
        contentSpacing = 20.dp,
        showBrand = false,
        modifier = modifier,
        bottomActions = {
            EcoTextLink(text = stringResource(R.string.recycler_registration_resend_link), onClick = onResendClick)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.recycler_registration_confirm_title),
            description = stringResource(
                if (isIndependent) R.string.recycler_registration_confirm_independent else R.string.recycler_registration_confirm_company,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.registration_delivery_email), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            Text(email, style = EcoTextStyles.LinkText, color = EcoTitle)
        }
        if (isIndependent) {
            Text(stringResource(R.string.registration_spam_hint), style = EcoTextStyles.LinkText, color = EcoTitle)
        }
        CenteredBrand(modifier = Modifier.padding(top = 48.dp))
    }
}

// Pantalla de correo confirmado; el independiente entra al módulo y el vinculado continúa con la empresa
@Composable
fun RecyclerEmailConfirmedScreen(
    type: RegistrationType,
    email: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onContinue)
    }
    val isIndependent = type == RegistrationType.INDEPENDENT_RECYCLER
    RegistrationLayout(
        // El comercio reutiliza esta pantalla y muestra su propio nombre de flujo en la cabecera
        stepLabel = stringResource(if (type == RegistrationType.STORE) R.string.store_flow_registration else R.string.recycler_flow_registration),
        stageLabel = stringResource(R.string.registration_stage_finished),
        progress = 1f,
        contentSpacing = 20.dp,
        showBrand = false,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(if (isIndependent) R.string.recycler_registration_enter_module else R.string.recycler_registration_continue),
                onClick = onContinue,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.recycler_registration_confirmed_title),
            description = stringResource(
                if (isIndependent) R.string.recycler_registration_confirmed_independent else R.string.recycler_registration_confirmed_company,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_check),
                    contentDescription = null,
                    tint = EcoGreenSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Text(stringResource(R.string.recycler_registration_email_confirmed), style = EcoTextStyles.LinkText, color = EcoGreenSecondary)
            }
            Text(email, style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        CenteredBrand(modifier = Modifier.padding(top = 120.dp))
    }
}

// Pantalla final del reciclador vinculado mientras la empresa activa su registro
@Composable
fun RecyclerPendingActivationScreen(
    company: Company?,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoHome)
    }
    RegistrationLayout(
        stepLabel = stringResource(R.string.recycler_flow_registration),
        stageLabel = stringResource(R.string.recycler_flow_stage_activation),
        progress = 1f,
        showBrand = false,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.recycler_registration_go_home), onClick = onGoHome)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.recycler_registration_done_title),
            description = stringResource(R.string.recycler_registration_done_description),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.recycler_company_pending_activation), style = EcoTextStyles.LinkText, color = EcoGreenSecondary)
        }
        company?.let { CompanyCard(company = it, selected = true, onClick = null) }
        Text(stringResource(R.string.recycler_registration_done_body), style = EcoTextStyles.Subtitle, color = EcoTitle)
        CenteredBrand(modifier = Modifier.padding(top = 96.dp))
    }
}

private val previewRecyclerState = RecyclerRegistrationUiState(
    fullName = "Ana Torres",
    email = "ana.torres@example.com",
    documentNumber = "1115070000",
    phone = "300 123 4567",
)

@Preview(name = "Independiente · Registro vacío", widthDp = 393, heightDp = 852)
@Composable
private fun IndependentRecyclerEmptyPreview() {
    EcoRutaTheme {
        RecyclerRegistrationFormScreen(
            type = RegistrationType.INDEPENDENT_RECYCLER,
            state = RecyclerRegistrationUiState(),
            actions = RecyclerRegistrationActions(),
        )
    }
}

@Preview(name = "Independiente · Registro con validación", widthDp = 393, heightDp = 1000)
@Composable
private fun IndependentRecyclerValidationPreview() {
    EcoRutaTheme {
        RecyclerRegistrationFormScreen(
            type = RegistrationType.INDEPENDENT_RECYCLER,
            state = previewRecyclerState.copy(
                fullName = "",
                fullNameErrorRes = R.string.registration_error_field_required,
                documentErrorRes = R.string.registration_error_document_registered,
                idDocumentErrorRes = R.string.recycler_registration_error_pdf,
            ),
            actions = RecyclerRegistrationActions(),
        )
    }
}

@Preview(name = "Vinculado · Datos de cuenta", widthDp = 393, heightDp = 852)
@Composable
private fun CompanyRecyclerAccountPreview() {
    EcoRutaTheme {
        RecyclerRegistrationFormScreen(
            type = RegistrationType.COMPANY_RECYCLER,
            state = RecyclerRegistrationUiState(),
            actions = RecyclerRegistrationActions(),
        )
    }
}

@Preview(name = "Independiente · Confirmación por correo", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerEmailConfirmationPreview() {
    EcoRutaTheme {
        RecyclerEmailConfirmationScreen(
            type = RegistrationType.INDEPENDENT_RECYCLER,
            email = "ana.torres@example.com",
            onResendClick = {},
            onExit = {},
        )
    }
}

@Preview(name = "Independiente · Registro confirmado", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerEmailConfirmedPreview() {
    EcoRutaTheme {
        RecyclerEmailConfirmedScreen(
            type = RegistrationType.INDEPENDENT_RECYCLER,
            email = "ana.torres@example.com",
            onContinue = {},
        )
    }
}

@Preview(name = "Vinculado · Pendiente de activación", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPendingActivationPreview() {
    EcoRutaTheme {
        RecyclerPendingActivationScreen(
            company = Company("EMP-01", "Asorecicladores del Oriente", "Asociación"),
            onGoHome = {},
        )
    }
}
