package com.ecoruta.app.features.auth.presentation.register

// Vista composable de registro para nuevos ciudadanos
// Formulario con nombres, celular, dirección, barrio y aceptación de términos

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.ecoruta.app.core.ui.components.EcoBottomActionBar
import com.ecoruta.app.core.ui.components.EcoCheckboxRow
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.EcoSelectionCard
import com.ecoruta.app.core.ui.components.EcoStepHeader
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

@Composable
fun RegistrationLayout(
    stepLabel: String,
    stageLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
    contentSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    onBackClick: (() -> Unit)? = null,
    bottomActions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        EcoStepHeader(stepLabel = stepLabel, stageLabel = stageLabel, progress = progress, onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
        EcoBottomActionBar(content = bottomActions)
    }
}

@Composable
fun RegistrationTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = EcoTextStyles.Title, color = EcoTitle)
        Text(description, style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

@Composable
fun CenteredBrand(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brand_leaf),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(stringResource(R.string.app_name), style = EcoTextStyles.Title, color = EcoPrimary)
        }
    }
}

@Composable
internal fun HaveAccountRow(onLoginClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.registration_have_account), style = EcoTextStyles.Subtitle, color = EcoTitle)
        EcoTextLink(
            text = stringResource(R.string.registration_login),
            onClick = onLoginClick,
        )
    }
}

@Composable
fun RegistrationTypeScreen(
    selectedType: RegistrationType?,
    onTypeSelected: (RegistrationType) -> Unit,
    onContinue: (RegistrationType) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_type_step),
        stageLabel = stringResource(R.string.registration_type_stage),
        progress = 0.25f,
        contentSpacing = 20.dp,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.registration_continue),
                onClick = { selectedType?.let(onContinue) },
                enabled = selectedType != null,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            HaveAccountRow(onLoginClick = onLoginClick)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_type_title),
            description = stringResource(R.string.registration_type_description),
        )
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RegistrationType.entries.forEach { type ->
                EcoSelectionCard(
                    title = stringResource(type.titleRes),
                    description = stringResource(type.descriptionRes),
                    iconRes = type.iconRes,
                    iconSize = if (type == RegistrationType.COMERCIO) 24.dp else 48.dp,
                    selected = type == selectedType,
                    onClick = { onTypeSelected(type) },
                )
            }
        }
    }
}

data class CitizenAccountActions(
    val onFullNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onGoogleClick: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onChangeRegistrationType: () -> Unit = {},
)

@Composable
fun CitizenAccountScreen(
    state: CitizenRegistrationUiState,
    actions: CitizenAccountActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        actions.onContinue()
    }

    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_1_of_2),
        stageLabel = stringResource(R.string.registration_stage_account),
        progress = 0.5f,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.registration_continue),
                onClick = submit,
                isLoading = state.isCheckingEmail,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            EcoTextLink(text = stringResource(R.string.registration_change_type), onClick = actions.onChangeRegistrationType)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_account_title),
            description = stringResource(R.string.registration_account_description),
        )
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
            Text(stringResource(R.string.registration_or_email), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
        }
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
    }
}

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

@Composable
fun CitizenEmailConfirmationScreen(
    email: String,
    isResending: Boolean,
    onResendClick: () -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoToLogin)
    }
    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_citizen),
        stageLabel = stringResource(R.string.registration_stage_confirmation),
        progress = 1f,
        contentSpacing = 20.dp,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.registration_go_to_login), onClick = onGoToLogin)
            EcoTextLink(
                text = stringResource(
                    if (isResending) R.string.registration_resending_email else R.string.registration_resend_email,
                ),
                onClick = { if (!isResending) onResendClick() },
            )
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_confirm_title),
            description = stringResource(R.string.registration_confirm_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.registration_delivery_email), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            Text(email, style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        Text(stringResource(R.string.registration_spam_hint), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        CenteredBrand(modifier = Modifier.padding(top = 48.dp))
    }
}

@Composable
fun CitizenAccessReadyScreen(
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onEnterClick)
    }
    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_citizen),
        stageLabel = stringResource(R.string.registration_stage_finished),
        progress = 1f,
        contentSpacing = 20.dp,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.registration_enter), onClick = onEnterClick)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_ready_title),
            description = stringResource(R.string.registration_ready_description),
        )
        CenteredBrand(modifier = Modifier.padding(top = 120.dp))
    }
}

@Preview(name = "Tipo de registro", widthDp = 393, heightDp = 852)
@Composable
private fun RegistrationTypePreview() {
    EcoRutaTheme {
        RegistrationTypeScreen(
            selectedType = RegistrationType.CIUDADANO,
            onTypeSelected = {},
            onContinue = {},
            onLoginClick = {},
        )
    }
}

@Preview(name = "Paso 1 Cuenta", widthDp = 393, heightDp = 852)
@Composable
private fun CitizenAccountPreview() {
    EcoRutaTheme {
        CitizenAccountScreen(
            state = CitizenRegistrationUiState(
                fullName = "Camila Andrea Gómez",
                email = "camila.gomez@ecoruta.co",
            ),
            actions = CitizenAccountActions(),
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

@Preview(name = "Confirmar correo", widthDp = 393, heightDp = 852)
@Composable
private fun CitizenEmailConfirmationPreview() {
    EcoRutaTheme {
        CitizenEmailConfirmationScreen(
            email = "camila.gomez@ecoruta.co",
            isResending = false,
            onResendClick = {},
            onGoToLogin = {},
        )
    }
}

@Preview(name = "Acceso listo", widthDp = 393, heightDp = 852)
@Composable
private fun CitizenAccessReadyPreview() {
    EcoRutaTheme {
        CitizenAccessReadyScreen(onEnterClick = {})
    }
}
