package com.ecoruta.app.features.auth.presentation.register

// Pantallas del registro del comercio aliado
// Incluye la cuenta del gerente, la confirmación por correo, los datos del comercio y el estado de la solicitud

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ecoruta.app.core.ui.components.EcoCheckboxRow
import com.ecoruta.app.core.ui.components.EcoPdfField
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.profile.presentation.StoreCategory
import com.ecoruta.app.features.profile.presentation.StoreIdType
import com.ecoruta.app.features.profile.presentation.components.IdTypeSelector
import com.ecoruta.app.features.profile.presentation.components.SelectField
import com.ecoruta.app.features.profile.presentation.components.labelRes

// Acciones disponibles en el paso de cuenta del gerente
data class StoreOwnerAccountActions(
    val onFullNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

// Primer paso del registro donde el gerente crea la cuenta con su nombre y correo
@Composable
fun StoreOwnerAccountScreen(
    state: StoreOwnerAccountUiState,
    actions: StoreOwnerAccountActions,
    modifier: Modifier = Modifier,
) {
    if (state.isOffline) {
        OfflineScreen(onRetry = actions.onRetry)
        return
    }
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        actions.onContinue()
    }

    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_1_of_2),
        stageLabel = stringResource(R.string.store_registration_stage_owner),
        progress = 0.5f,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.registration_continue),
                onClick = submit,
                isLoading = state.isSubmitting,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            EcoTextLink(text = stringResource(R.string.registration_back), onClick = actions.onBack)
        },
    ) {
        // Con errores el título invita a revisar los datos sin perder lo que ya se escribió
        RegistrationTitle(
            title = stringResource(if (state.hasErrors) R.string.store_registration_owner_review_title else R.string.store_registration_owner_title),
            description = stringResource(
                if (state.hasErrors) R.string.store_registration_owner_review_description else R.string.store_registration_owner_description,
            ),
        )
        EcoTextField(
            value = state.fullName,
            onValueChange = actions.onFullNameChange,
            label = stringResource(R.string.store_registration_owner_name_label),
            placeholder = stringResource(R.string.store_registration_owner_name_placeholder),
            errorMessage = state.fullNameErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.email,
            onValueChange = actions.onEmailChange,
            label = stringResource(R.string.login_email_label),
            placeholder = stringResource(R.string.store_registration_email_placeholder),
            errorMessage = state.emailErrorRes?.let { stringResource(it) },
            supportingText = stringResource(R.string.store_registration_email_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        Text(
            stringResource(if (state.hasErrors) R.string.store_registration_complete_required else R.string.store_registration_owner_info),
            style = EcoTextStyles.Subtitle,
            color = EcoTitle,
        )
    }
}

// Pantalla que pide al gerente abrir el enlace enviado a su correo
@Composable
fun StoreEmailConfirmationScreen(
    state: StoreEmailConfirmationUiState,
    onResendClick: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecyclerEmailConfirmationScreen(
        type = RegistrationType.STORE,
        email = state.email,
        onResendClick = onResendClick,
        onExit = onExit,
        modifier = modifier,
        resendStatus = state.resendStatus,
    )
}

// Pantalla que confirma el correo del gerente y lo lleva a los datos del comercio
@Composable
fun StoreEmailConfirmedScreen(
    email: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecyclerEmailConfirmedScreen(
        type = RegistrationType.STORE,
        email = email,
        onContinue = onContinue,
        modifier = modifier,
    )
}

// Acciones disponibles en el paso de datos del comercio
data class StoreDetailsActions(
    val onNameChange: (String) -> Unit = {},
    val onIdTypeChange: (StoreIdType) -> Unit = {},
    val onIdNumberChange: (String) -> Unit = {},
    val onCategoryChange: (StoreCategory) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onPickPdf: () -> Unit = {},
    val onRemovePdf: () -> Unit = {},
    val onDataPolicyChange: (Boolean) -> Unit = {},
    val onViewPolicyClick: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

// Segundo paso del registro con los datos del establecimiento en Tuluá
@Composable
fun StoreDetailsScreen(
    state: StoreDetailsUiState,
    actions: StoreDetailsActions,
    modifier: Modifier = Modifier,
) {
    if (state.isOffline) {
        OfflineScreen(onRetry = actions.onRetry)
        return
    }
    val focusManager = LocalFocusManager.current

    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_2_of_2),
        stageLabel = stringResource(R.string.store_registration_stage_details),
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
            // La solicitud solo se puede enviar después de aceptar la política de datos
            EcoPrimaryButton(
                text = stringResource(R.string.store_registration_submit),
                onClick = {
                    focusManager.clearFocus()
                    actions.onSubmit()
                },
                enabled = state.acceptedDataPolicy,
                isLoading = state.isSubmitting,
            )
            EcoTextLink(text = stringResource(R.string.registration_back), onClick = actions.onBack)
        },
    ) {
        RegistrationTitle(
            title = stringResource(if (state.hasErrors) R.string.store_registration_details_review_title else R.string.store_registration_details_title),
            description = stringResource(
                if (state.hasErrors) R.string.store_registration_details_review_description else R.string.store_registration_details_description,
            ),
        )
        EcoTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.store_registration_name_label),
            placeholder = stringResource(R.string.store_registration_name_placeholder),
            errorMessage = state.nameErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.idNumber,
            onValueChange = actions.onIdNumberChange,
            label = stringResource(R.string.store_registration_id_label),
            placeholder = stringResource(R.string.store_registration_id_placeholder),
            leadingIcon = { IdTypeSelector(selected = state.idType, onSelected = actions.onIdTypeChange) },
            errorMessage = state.idErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
        SelectField(
            label = stringResource(R.string.store_registration_category_label),
            placeholder = stringResource(R.string.store_application_category_placeholder),
            iconRes = R.drawable.ic_tag,
            options = StoreCategory.entries,
            selected = state.category,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = actions.onCategoryChange,
            errorMessage = state.categoryErrorRes?.let { stringResource(it) },
        )
        Text(stringResource(R.string.store_registration_category_hint), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        EcoTextField(
            value = state.address,
            onValueChange = actions.onAddressChange,
            label = stringResource(R.string.store_registration_address_label),
            placeholder = stringResource(R.string.store_registration_address_placeholder),
            errorMessage = state.addressErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = stringResource(R.string.store_registration_phone_label),
            placeholder = stringResource(R.string.store_registration_phone_placeholder),
            errorMessage = state.phoneErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        // El selector de archivos vive en la navegación, aquí solo se muestra el RUT elegido
        EcoPdfField(
            label = stringResource(R.string.store_application_rut),
            hint = stringResource(R.string.store_application_rut_hint),
            fileName = state.rutFileName,
            onPickClick = actions.onPickPdf,
            onRemove = actions.onRemovePdf,
            errorMessage = state.rutErrorRes?.let { stringResource(it) },
        )
        Text(
            stringResource(if (state.hasErrors) R.string.store_registration_details_review_info else R.string.store_registration_details_info),
            style = EcoTextStyles.Subtitle,
            color = EcoTitle,
        )
    }
}

// Pantalla con el estado de la solicitud del comercio en revisión, aprobada o rechazada
@Composable
fun StoreRequestStatusScreen(
    state: StoreRequestStatusUiState,
    onLoginClick: () -> Unit,
    onGoHome: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoHome)
    }
    when (state) {
        StoreRequestStatusUiState.Loading -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = EcoPrimary) }
        StoreRequestStatusUiState.Offline -> OfflineScreen(onRetry = onRetry)
        StoreRequestStatusUiState.Error -> EcoSystemStateScreen(
            title = stringResource(R.string.store_status_error_title),
            subtitle = stringResource(R.string.store_status_error_subtitle),
            illustrationRes = R.drawable.ill_state_offline,
            heading = stringResource(R.string.store_status_error_heading),
            body = stringResource(R.string.store_status_error_body),
            actionText = stringResource(R.string.action_retry),
            onAction = onRetry,
            modifier = modifier,
        )
        is StoreRequestStatusUiState.InReview -> StoreStatusLayout(
            stageLabel = stringResource(R.string.store_status_stage_review),
            title = stringResource(R.string.store_review_title),
            description = stringResource(R.string.store_status_review_description),
            onGoHome = onGoHome,
            modifier = modifier,
        ) {
            StoreStatusCard(
                iconRes = R.drawable.ic_clock,
                title = stringResource(R.string.store_status_review_card_title),
                body = stringResource(R.string.store_status_review_card_body),
                highlightColor = EcoGreenSecondary,
            )
            StoreStatusCard(
                iconRes = R.drawable.ic_store,
                title = state.storeName ?: stringResource(R.string.store_registration_name_placeholder),
                body = stringResource(R.string.store_status_store_location),
            )
            Text(stringResource(R.string.store_status_review_notice), style = EcoTextStyles.Subtitle, color = EcoTitle)
            StoreStatusCard(
                iconRes = R.drawable.ic_lock,
                title = stringResource(R.string.store_status_access_pending_title),
                body = stringResource(R.string.store_status_access_pending_body),
            )
        }
        is StoreRequestStatusUiState.Approved -> StoreStatusLayout(
            stageLabel = stringResource(R.string.store_status_stage_approved),
            title = stringResource(R.string.store_status_approved_title),
            description = stringResource(R.string.store_status_approved_description),
            onGoHome = onGoHome,
            onLoginClick = onLoginClick,
            modifier = modifier,
        ) {
            StoreStatusCard(
                iconRes = R.drawable.ic_circle_check,
                title = stringResource(R.string.store_status_approved_card_title),
                body = stringResource(
                    R.string.store_status_store_with_location,
                    state.storeName ?: stringResource(R.string.store_registration_name_placeholder),
                ),
                highlightColor = EcoGreenSecondary,
            )
            StoreStatusCard(
                iconRes = R.drawable.ic_mail,
                title = stringResource(R.string.store_status_email_notice_title),
                body = stringResource(R.string.store_status_approved_email_body),
            )
            Text(stringResource(R.string.store_status_approved_body), style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        is StoreRequestStatusUiState.Rejected -> StoreStatusLayout(
            stageLabel = stringResource(R.string.store_status_stage_rejected),
            title = stringResource(R.string.store_status_rejected_title),
            description = stringResource(R.string.store_status_rejected_description),
            onGoHome = onGoHome,
            modifier = modifier,
        ) {
            // Si el administrador no dejó un motivo se muestra un texto por defecto
            StoreStatusCard(
                iconRes = null,
                title = stringResource(R.string.store_status_rejected_card_title),
                body = state.reason?.takeIf { it.isNotBlank() } ?: stringResource(R.string.store_status_rejected_no_reason),
                label = stringResource(R.string.store_status_rejected_reason_label),
                highlightColor = EcoError,
                titleColor = EcoError,
            )
            StoreStatusCard(
                iconRes = R.drawable.ic_mail,
                title = stringResource(R.string.store_status_rejected_email_title),
                body = stringResource(R.string.store_status_rejected_email_body),
            )
            Text(stringResource(R.string.store_status_rejected_body), style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
    }
}

// Estructura común de los estados de la solicitud con cabecera sin progreso y acciones inferiores
@Composable
private fun StoreStatusLayout(
    stageLabel: String,
    title: String,
    description: String,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
    onLoginClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    RegistrationLayout(
        stepLabel = stringResource(R.string.store_status_flow),
        stageLabel = stageLabel,
        progress = 0f,
        showProgress = false,
        modifier = modifier,
        bottomActions = {
            // Solo la solicitud aprobada ofrece iniciar sesión
            if (onLoginClick != null) {
                EcoPrimaryButton(text = stringResource(R.string.login_submit), onClick = onLoginClick)
            }
            EcoTextLink(text = stringResource(R.string.store_status_go_home), onClick = onGoHome)
        },
    ) {
        RegistrationTitle(title = title, description = description)
        content()
    }
}

// Tarjeta informativa con icono opcional, título y texto que puede resaltarse con un borde de color
@Composable
private fun StoreStatusCard(
    @DrawableRes iconRes: Int?,
    title: String,
    body: String,
    label: String? = null,
    highlightColor: Color? = null,
    titleColor: Color = EcoTitle,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(if (highlightColor != null) 1.5.dp else 1.dp, highlightColor ?: EcoBorder, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (iconRes != null) {
                Icon(painterResource(iconRes), contentDescription = null, tint = highlightColor ?: EcoSubtitle, modifier = Modifier.size(20.dp))
            }
            Text(title, style = EcoTextStyles.ButtonText, color = titleColor)
        }
        if (label != null) Text(label, style = EcoTextStyles.Subtitle, color = EcoTitle)
        Text(body, style = EcoTextStyles.Subtitle, color = if (label != null) EcoSubtitle else EcoTitle)
    }
}

@Preview(name = "Comercio · Cuenta del dueño", widthDp = 393, heightDp = 852)
@Composable
private fun StoreOwnerAccountPreview() {
    EcoRutaTheme {
        StoreOwnerAccountScreen(state = StoreOwnerAccountUiState(), actions = StoreOwnerAccountActions())
    }
}

@Preview(name = "Comercio · Cuenta del dueño con validación", widthDp = 393, heightDp = 852)
@Composable
private fun StoreOwnerAccountValidationPreview() {
    EcoRutaTheme {
        StoreOwnerAccountScreen(
            state = StoreOwnerAccountUiState(
                email = "contacto@",
                fullNameErrorRes = R.string.store_registration_error_owner_name,
                emailErrorRes = R.string.store_registration_error_email,
            ),
            actions = StoreOwnerAccountActions(),
        )
    }
}

@Preview(name = "Comercio · Cuenta sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun StoreOwnerAccountOfflinePreview() {
    EcoRutaTheme {
        StoreOwnerAccountScreen(state = StoreOwnerAccountUiState(isOffline = true), actions = StoreOwnerAccountActions())
    }
}

@Preview(name = "Comercio · Confirmación de correo pendiente", widthDp = 393, heightDp = 852)
@Composable
private fun StoreEmailConfirmationPreview() {
    EcoRutaTheme {
        StoreEmailConfirmationScreen(state = StoreEmailConfirmationUiState(email = "gerente@example.com"), onResendClick = {}, onExit = {})
    }
}

@Preview(name = "Comercio · Reenviando enlace", widthDp = 393, heightDp = 852)
@Composable
private fun StoreEmailResendingPreview() {
    EcoRutaTheme {
        StoreEmailConfirmationScreen(
            state = StoreEmailConfirmationUiState(email = "gerente@example.com", resendStatus = ResendLinkStatus.SENDING),
            onResendClick = {},
            onExit = {},
        )
    }
}

@Preview(name = "Comercio · Enlace reenviado", widthDp = 393, heightDp = 852)
@Composable
private fun StoreEmailResentPreview() {
    EcoRutaTheme {
        StoreEmailConfirmationScreen(
            state = StoreEmailConfirmationUiState(email = "gerente@example.com", resendStatus = ResendLinkStatus.SENT),
            onResendClick = {},
            onExit = {},
        )
    }
}

@Preview(name = "Comercio · Correo validado", widthDp = 393, heightDp = 852)
@Composable
private fun StoreEmailConfirmedPreview() {
    EcoRutaTheme {
        StoreEmailConfirmedScreen(email = "gerente@example.com", onContinue = {})
    }
}

@Preview(name = "Comercio · Datos del comercio", widthDp = 393, heightDp = 1000)
@Composable
private fun StoreDetailsPreview() {
    EcoRutaTheme {
        StoreDetailsScreen(state = StoreDetailsUiState(), actions = StoreDetailsActions())
    }
}

@Preview(name = "Comercio · Datos del comercio con validación", widthDp = 393, heightDp = 1100)
@Composable
private fun StoreDetailsValidationPreview() {
    EcoRutaTheme {
        StoreDetailsScreen(
            state = StoreDetailsUiState(
                name = "Papelería El Lápiz",
                category = StoreCategory.STATIONERY_BOOKSTORE,
                idErrorRes = R.string.store_registration_error_id,
                addressErrorRes = R.string.store_registration_error_address,
                phoneErrorRes = R.string.store_registration_error_phone,
            ),
            actions = StoreDetailsActions(),
        )
    }
}

@Preview(name = "Comercio · Datos con RUT cargado", widthDp = 393, heightDp = 1100)
@Composable
private fun StoreDetailsRutLoadedPreview() {
    EcoRutaTheme {
        StoreDetailsScreen(
            state = StoreDetailsUiState(rutFileName = "rut_papeleria_el_lapiz.pdf"),
            actions = StoreDetailsActions(),
        )
    }
}

@Preview(name = "Comercio · Datos con RUT pendiente", widthDp = 393, heightDp = 1100)
@Composable
private fun StoreDetailsRutErrorPreview() {
    EcoRutaTheme {
        StoreDetailsScreen(
            state = StoreDetailsUiState(rutErrorRes = R.string.store_application_error_rut),
            actions = StoreDetailsActions(),
        )
    }
}

@Preview(name = "Comercio · Solicitud en revisión", widthDp = 393, heightDp = 852)
@Composable
private fun StoreRequestInReviewPreview() {
    EcoRutaTheme {
        StoreRequestStatusScreen(
            state = StoreRequestStatusUiState.InReview(storeName = "Papelería El Lápiz"),
            onLoginClick = {},
            onGoHome = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Comercio · Solicitud aprobada", widthDp = 393, heightDp = 852)
@Composable
private fun StoreRequestApprovedPreview() {
    EcoRutaTheme {
        StoreRequestStatusScreen(
            state = StoreRequestStatusUiState.Approved(storeName = "Papelería El Lápiz"),
            onLoginClick = {},
            onGoHome = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Comercio · Solicitud rechazada", widthDp = 393, heightDp = 852)
@Composable
private fun StoreRequestRejectedPreview() {
    EcoRutaTheme {
        StoreRequestStatusScreen(
            state = StoreRequestStatusUiState.Rejected(reason = "El RUT no coincide con el nombre comercial registrado."),
            onLoginClick = {},
            onGoHome = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Comercio · Solicitud rechazada sin motivo", widthDp = 393, heightDp = 852)
@Composable
private fun StoreRequestRejectedEmptyPreview() {
    EcoRutaTheme {
        StoreRequestStatusScreen(
            state = StoreRequestStatusUiState.Rejected(),
            onLoginClick = {},
            onGoHome = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Comercio · Estado de solicitud con error", widthDp = 393, heightDp = 852)
@Composable
private fun StoreRequestErrorPreview() {
    EcoRutaTheme {
        StoreRequestStatusScreen(state = StoreRequestStatusUiState.Error, onLoginClick = {}, onGoHome = {}, onRetry = {})
    }
}
