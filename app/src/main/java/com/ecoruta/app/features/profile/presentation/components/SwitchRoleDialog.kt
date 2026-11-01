package com.ecoruta.app.features.profile.presentation.components

// Diálogo composable para selección y cambio de perfil activo
// Muestra lista de perfiles habilitados y opción de postular a roles no activos

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoFocusLayout
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPdfField
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoError
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
import com.ecoruta.app.features.profile.presentation.ProfileUiState
import com.ecoruta.app.features.profile.presentation.RecyclerApplicationUiState
import com.ecoruta.app.features.profile.presentation.RecyclerModality
import com.ecoruta.app.features.profile.presentation.StoreApplicationUiState
import com.ecoruta.app.features.profile.presentation.StoreCategory
import com.ecoruta.app.features.profile.presentation.StoreIdType
import com.ecoruta.app.features.profile.presentation.UserProfile
import com.ecoruta.app.features.profile.presentation.UserRole

// Acciones y componentes para cambiar el perfil activo del usuario
data class ChangeProfileActions(
    val onBack: () -> Unit = {},
    val onSwitchRole: (UserRole) -> Unit = {},
    val onRequestRole: (UserRole) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

private val requestableRoles = listOf(UserRole.COMERCIO, UserRole.RECICLADOR)

@Composable
fun ChangeProfileScreen(state: ProfileUiState, actions: ChangeProfileActions) {
    EcoTabScreenLayout(selectedTab = CitizenTab.PROFILE, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.change_profile_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.change_profile_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when (state) {
            ProfileUiState.Loading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
            ProfileUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.profile_load_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            is ProfileUiState.Loaded -> ChangeProfileContent(state.profile, actions)
        }
    }
}

@Composable
private fun ChangeProfileContent(profile: UserProfile, actions: ChangeProfileActions) {
    val roles = listOf(profile.role) + UserRole.entries.filter { it != profile.role && it in profile.approvedRoles }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        roles.forEach { role ->
            RoleCard(role = role, isCurrent = role == profile.role, onClick = { actions.onSwitchRole(role) })
        }
    }
    val available = requestableRoles.filter { it !in profile.approvedRoles }
    if (available.isNotEmpty()) {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.change_profile_request_title), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            available.forEach { role ->
                RequestRow(role = role, pending = role in profile.pendingRoles, onClick = { actions.onRequestRole(role) })
            }
        }
    }
}

@Composable
private fun RoleCard(role: UserRole, isCurrent: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val info = role.info()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(1.5.dp, if (isCurrent) EcoGreenSecondary else EcoFieldBackground, shape)
            .clickable(enabled = !isCurrent, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCurrent) EcoGreenSecondary else EcoFieldBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(info.iconRes),
                    contentDescription = null,
                    tint = if (isCurrent) EcoWhite else EcoGreenSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(info.nameRes), style = EcoTextStyles.ButtonText, color = if (isCurrent) EcoTitle else EcoSubtitle)
                Text(
                    stringResource(if (isCurrent) R.string.change_profile_current else R.string.change_profile_tap_to_switch),
                    style = EcoTextStyles.FieldLabel,
                    color = if (isCurrent) EcoPrimary else EcoSubtitle,
                )
            }
            Icon(
                painterResource(if (isCurrent) R.drawable.ic_circle_check else R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = if (isCurrent) EcoGreenSecondary else EcoSubtitle,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(stringResource(info.descriptionRes), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

@Composable
private fun RequestRow(role: UserRole, pending: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(role.info().iconRes), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
        Text(
            stringResource(if (role == UserRole.COMERCIO) R.string.change_profile_register_store else R.string.change_profile_apply_recycler),
            style = EcoTextStyles.LinkText,
            color = EcoPrimary,
            modifier = Modifier.weight(1f),
        )
        if (pending) Text(stringResource(R.string.change_profile_in_review), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
    }
}

private data class RoleInfo(@StringRes val nameRes: Int, @StringRes val descriptionRes: Int, @DrawableRes val iconRes: Int)

private fun UserRole.info(): RoleInfo = when (this) {
    UserRole.CIUDADANO -> RoleInfo(R.string.role_citizen, R.string.change_profile_citizen_description, R.drawable.ic_leaf)
    UserRole.COMERCIO -> RoleInfo(R.string.change_profile_store_name, R.string.change_profile_store_description, R.drawable.ic_store)
    UserRole.RECICLADOR -> RoleInfo(R.string.role_recycler, R.string.change_profile_recycler_description, R.drawable.ic_recycle)
    UserRole.CHOFER -> RoleInfo(R.string.role_driver, R.string.change_profile_driver_description, R.drawable.ic_truck)
}

// Componentes compartidos para los formularios de solicitud de rol
@Composable
internal fun FieldIcon(@DrawableRes iconRes: Int) {
    Icon(painterResource(iconRes), contentDescription = null, tint = EcoSubtitle, modifier = Modifier.size(20.dp))
}

@Composable
internal fun <T> SelectField(
    label: String,
    placeholder: String,
    @DrawableRes iconRes: Int,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    errorMessage: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(EcoDimens.ControlCornerRadius)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoTitle)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(EcoFieldBackground)
                    .then(if (errorMessage != null) Modifier.border(1.5.dp, EcoError, shape) else Modifier)
                    .clickable(role = Role.DropdownList, onClickLabel = label) { expanded = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FieldIcon(iconRes)
                Text(
                    selected?.let { optionLabel(it) } ?: placeholder,
                    style = EcoTextStyles.Subtitle,
                    color = if (selected != null) EcoTitle else EcoSubtitle,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = EcoSubtitle,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(90f),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option), style = EcoTextStyles.Subtitle, color = EcoTitle) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        },
                    )
                }
            }
        }
        if (errorMessage != null) EcoErrorMessage(text = errorMessage)
    }
}

@Composable
internal fun SupportInfo(@DrawableRes iconRes: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(20.dp))
        Text(text, style = EcoTextStyles.Subtitle, color = EcoSubtitle, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ReadOnlyField(label: String, value: String, @DrawableRes iconRes: Int) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoTitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(EcoDimens.ControlCornerRadius))
                .background(EcoFieldBackground)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FieldIcon(iconRes)
            Text(value, style = EcoTextStyles.Subtitle, color = EcoSubtitle, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = EcoSubtitle, modifier = Modifier.size(20.dp))
        }
    }
}

// Flujo de registro de comercio aliado y pantalla de solicitud en revisión
data class StoreApplicationActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onCategorySelected: (StoreCategory) -> Unit = {},
    val onIdTypeSelected: (StoreIdType) -> Unit = {},
    val onIdNumberChange: (String) -> Unit = {},
    val onRutPicked: (PickedDocument) -> Unit = {},
    val onRutRemoved: () -> Unit = {},
    val onSubmit: () -> Unit = {},
)

@Composable
fun StoreApplicationScreen(state: StoreApplicationUiState, actions: StoreApplicationActions) {
    val focusManager = LocalFocusManager.current
    EcoFocusLayout(
        onBack = actions.onBack,
        bottomActions = {
            Text(
                stringResource(R.string.role_request_citizen_stays_active),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            EcoPrimaryButton(
                text = stringResource(R.string.store_application_submit),
                onClick = {
                    focusManager.clearFocus()
                    actions.onSubmit()
                },
                isLoading = state.isSubmitting,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.store_application_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.store_application_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Text(stringResource(R.string.role_request_all_required), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        EcoTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.store_application_name),
            placeholder = stringResource(R.string.store_application_name_placeholder),
            leadingIcon = { FieldIcon(R.drawable.ic_store) },
            errorMessage = state.nameError?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.address,
            onValueChange = actions.onAddressChange,
            label = stringResource(R.string.store_application_address),
            placeholder = stringResource(R.string.store_application_address_placeholder),
            leadingIcon = { FieldIcon(R.drawable.ic_map_pin) },
            errorMessage = state.addressError?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = stringResource(R.string.store_application_phone),
            placeholder = stringResource(R.string.store_application_phone_placeholder),
            leadingIcon = { FieldIcon(R.drawable.ic_phone) },
            errorMessage = state.phoneError?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        SelectField(
            label = stringResource(R.string.store_application_category),
            placeholder = stringResource(R.string.store_application_category_placeholder),
            iconRes = R.drawable.ic_tag,
            options = StoreCategory.entries,
            selected = state.category,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = actions.onCategorySelected,
            errorMessage = state.categoryError?.let { stringResource(it) },
        )
        EcoTextField(
            value = state.idNumber,
            onValueChange = actions.onIdNumberChange,
            label = stringResource(R.string.store_application_id),
            placeholder = stringResource(if (state.idType == StoreIdType.NIT) R.string.store_application_nit_placeholder else R.string.store_application_cedula_placeholder),
            leadingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldIcon(R.drawable.ic_id_card)
                    IdTypeSelector(selected = state.idType, onSelected = actions.onIdTypeSelected)
                }
            },
            errorMessage = state.idError?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        EcoPdfField(
            label = stringResource(R.string.store_application_rut),
            hint = stringResource(R.string.store_application_rut_hint),
            document = state.rut,
            onPicked = actions.onRutPicked,
            onRemove = actions.onRutRemoved,
            errorMessage = state.rutError?.let { stringResource(it) },
        )
        SupportInfo(iconRes = R.drawable.ic_shield_check, text = stringResource(R.string.store_application_review_info))
    }
}

@Composable
private fun IdTypeSelector(selected: StoreIdType, onSelected: (StoreIdType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.DropdownList, onClickLabel = stringResource(R.string.store_application_id_type)) { expanded = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(selected.shortLabelRes()), style = EcoTextStyles.FieldLabel.copy(fontSize = 13.sp, letterSpacing = 0.sp), color = EcoTitle)
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = EcoSubtitle,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(90f),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                StoreIdType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(stringResource(type.labelRes()), style = EcoTextStyles.Subtitle, color = EcoTitle) },
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
fun StoreRequestInReviewScreen(
    withoutBackend: Boolean,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.PROFILE,
        onBack = onBack,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.role_request_go_home), onClick = onGoHome, trailingIconRes = R.drawable.ic_arrow_right)
        },
    ) {
        Text(stringResource(R.string.store_review_title), style = EcoTextStyles.Title, color = EcoTitle)
        if (withoutBackend) EcoInfoHint(text = stringResource(R.string.role_request_design_mode))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(EcoGreenSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(24.dp))
            }
            Text(
                stringResource(R.string.store_review_status),
                style = EcoTextStyles.LinkText,
                color = EcoPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(EcoWhite)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(stringResource(R.string.store_review_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center)
        }
        SupportInfo(iconRes = R.drawable.ic_bell, text = stringResource(R.string.role_request_notification))
        Text(stringResource(R.string.role_request_meanwhile), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

private fun StoreCategory.labelRes(): Int = when (this) {
    StoreCategory.NEIGHBORHOOD_STORE -> R.string.store_category_neighborhood
    StoreCategory.RESTAURANT -> R.string.store_category_restaurant
    StoreCategory.CAFETERIA -> R.string.store_category_cafe
    StoreCategory.GROCERY -> R.string.store_category_market
    StoreCategory.STATIONERY_BOOKSTORE -> R.string.store_category_stationery
    StoreCategory.OTHER -> R.string.store_category_other
}

private fun StoreIdType.shortLabelRes(): Int = when (this) {
    StoreIdType.NATIONAL_ID -> R.string.store_id_cedula_short
    StoreIdType.NIT -> R.string.store_id_nit_short
}

private fun StoreIdType.labelRes(): Int = when (this) {
    StoreIdType.NATIONAL_ID -> R.string.store_id_cedula
    StoreIdType.NIT -> R.string.store_id_nit
}

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
    val isLinked = state.modality == RecyclerModality.COMPANY_LINKED
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
    RecyclerModality.INDEPENDENT -> R.string.recycler_modality_independent
    RecyclerModality.COMPANY_LINKED -> R.string.recycler_modality_linked
}

private val previewCompany = Company(
    id = "comp-1",
    name = "Asociación Recicladores del Sur",
    type = "Organización de recicladores autorizada · Pasto",
)

@Preview(name = "Cambiar Perfil", widthDp = 393, heightDp = 852)
@Composable
private fun ChangeProfilePreview() {
    EcoRutaTheme {
        ChangeProfileScreen(
            state = ProfileUiState.Loaded(
                profile = UserProfile(
                    fullName = "Camila Andrea Gómez",
                    email = "camila.gomez@ecoruta.co",
                    city = "Pasto",
                    role = UserRole.CIUDADANO,
                    approvedRoles = setOf(UserRole.CIUDADANO),
                ),
            ),
            actions = ChangeProfileActions(),
        )
    }
}

@Preview(name = "Postular Comercio", widthDp = 393, heightDp = 950)
@Composable
private fun StoreApplicationPreview() {
    EcoRutaTheme {
        StoreApplicationScreen(
            state = StoreApplicationUiState(
                name = "Café del Parque",
                address = "Cra. 25 #18-42",
                phone = "3154829103",
                category = StoreCategory.CAFETERIA,
                idNumber = "901482910",
            ),
            actions = StoreApplicationActions(),
        )
    }
}

@Preview(name = "Comercio en Revisión", widthDp = 393, heightDp = 852)
@Composable
private fun StoreRequestInReviewPreview() {
    EcoRutaTheme {
        StoreRequestInReviewScreen(
            withoutBackend = false,
            onBack = {},
            onGoHome = {},
            onTabClick = {},
        )
    }
}

@Preview(name = "Postular Reciclador", widthDp = 393, heightDp = 900)
@Composable
private fun RecyclerApplicationPreview() {
    EcoRutaTheme {
        RecyclerApplicationScreen(
            state = RecyclerApplicationUiState(
                fullName = "Camila Andrea Gómez",
                documentNumber = "1085324910",
                phone = "3154829103",
                modality = RecyclerModality.INDEPENDENT,
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
            state = RecyclerApplicationUiState(modality = RecyclerModality.INDEPENDENT),
            onBack = {},
            onGoHome = {},
        )
    }
}
