package com.ecoruta.app.features.profile.presentation.components

// Pantallas de solicitud para registrar un comercio aliado
// Incluye el formulario de solicitud y el estado de revisión

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoFocusLayout
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPdfField
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
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
import com.ecoruta.app.features.profile.presentation.StoreApplicationUiState
import com.ecoruta.app.features.profile.presentation.StoreCategory
import com.ecoruta.app.features.profile.presentation.StoreIdType

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
    StoreCategory.TIENDA_DE_BARRIO -> R.string.store_category_neighborhood
    StoreCategory.RESTAURANTE -> R.string.store_category_restaurant
    StoreCategory.CAFETERIA -> R.string.store_category_cafe
    StoreCategory.MERCADO -> R.string.store_category_market
    StoreCategory.PAPELERIA_LIBRERIA -> R.string.store_category_stationery
    StoreCategory.OTRO -> R.string.store_category_other
}

private fun StoreIdType.shortLabelRes(): Int = when (this) {
    StoreIdType.CEDULA -> R.string.store_id_cedula_short
    StoreIdType.NIT -> R.string.store_id_nit_short
}

private fun StoreIdType.labelRes(): Int = when (this) {
    StoreIdType.CEDULA -> R.string.store_id_cedula
    StoreIdType.NIT -> R.string.store_id_nit
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
