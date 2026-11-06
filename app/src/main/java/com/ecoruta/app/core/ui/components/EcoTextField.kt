package com.ecoruta.app.core.ui.components

// Campo de texto estandarizado para formularios y mensajes de validación
// Soporta etiqueta superior, iconos laterales, texto de ayuda e indicador de error

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

// Campo de formulario con etiqueta superior, entrada de texto y mensaje de error o ayuda opcional
@Composable
fun EcoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    errorMessage: String? = null,
    isError: Boolean = errorMessage != null,
    // Texto de ayuda secundario ubicado debajo del campo cuando no hay errores
    supportingText: String? = null,
    enabled: Boolean = true,
    // Muestra el valor sin permitir escribirlo, útil cuando otro control elige el valor
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val shape = RoundedCornerShape(EcoDimens.ControlCornerRadius)
    // Muestra el icono de alerta dentro del campo cuando existe un error y no hay otro icono a la derecha
    val showErrorIcon = trailingIcon == null && errorMessage != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, style = EcoTextStyles.FieldLabel, color = EcoTitle)

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            // El texto escrito utiliza el color de título y se atenúa con el de subtítulo cuando el campo está deshabilitado
            textStyle = EcoTextStyles.Subtitle.copy(color = if (enabled) EcoTitle else EcoSubtitle),
            cursorBrush = SolidColor(EcoTitle),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (errorMessage != null) error(errorMessage) },
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EcoDimens.ControlHeight)
                        .background(EcoFieldBackground, shape)
                        .border(
                            width = if (isError) 1.5.dp else 1.dp,
                            color = if (isError) EcoError else EcoBorder,
                            shape = shape,
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (leadingIcon != null) {
                        // Contenedor para el icono o selector ubicado al inicio del campo
                        Box(contentAlignment = Alignment.Center) { leadingIcon() }
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(text = placeholder, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Box(contentAlignment = Alignment.Center) { trailingIcon() }
                    } else if (showErrorIcon) {
                        Icon(
                            painter = painterResource(R.drawable.ic_alert_circle),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(EcoDimens.InputIconSize),
                        )
                    }
                }
            },
        )

        if (errorMessage != null) {
            EcoErrorMessage(text = errorMessage)
        } else if (supportingText != null) {
            Text(text = supportingText, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
    }
}

// Fila con icono de alerta y mensaje descriptivo para indicar errores de validación
@Composable
fun EcoErrorMessage(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.ic_alert_circle,
    iconSize: DpSize = DpSize(16.dp, 16.dp),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(iconSize),
        )
        Text(text = text, style = EcoTextStyles.Subtitle, color = EcoError)
    }
}

@Preview(name = "Campos de texto", widthDp = 354)
@Composable
private fun EcoTextFieldPreview() {
    EcoRutaTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EcoTextField(
                value = "",
                onValueChange = {},
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_email_placeholder),
                leadingIcon = { EcoInputIcon(R.drawable.ic_mail) },
            )
            EcoTextField(value = "ana.torres@example.com", onValueChange = {}, label = stringResource(R.string.login_email_label))
            EcoTextField(
                value = "ana.torres",
                onValueChange = {},
                label = stringResource(R.string.login_email_label),
                errorMessage = stringResource(R.string.registration_error_field_required),
            )
            EcoTextField(
                value = "Cooperativa EcoVida",
                onValueChange = {},
                label = stringResource(R.string.recycler_edit_profile_company_label),
                supportingText = stringResource(R.string.recycler_edit_profile_company_hint),
                enabled = false,
            )
        }
    }
}

@Preview(name = "Mensaje de error", widthDp = 354)
@Composable
private fun EcoErrorMessagePreview() {
    EcoRutaTheme { EcoErrorMessage(text = stringResource(R.string.registration_error_field_required)) }
}
