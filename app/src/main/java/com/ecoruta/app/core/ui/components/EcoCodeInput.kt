package com.ecoruta.app.core.ui.components

// Campo de entrada segmentado para códigos numéricos de verificación
// Renderiza casillas individuales para cada dígito y resalta la posición activa

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoCodeBorder
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoTextStyles

// Entrada única de texto que muestra cada dígito del código dentro de su propia casilla visual
@Composable
fun EcoCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onDone: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(length)) },
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityLabel },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val isCurrent = isFocused && index == value.length.coerceAtMost(length - 1)
                    val shape = RoundedCornerShape(11.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .border(
                                width = if (isCurrent && !isError) 1.5.dp else 1.dp,
                                color = when {
                                    isError -> EcoError
                                    isCurrent -> EcoPrimary
                                    else -> EcoCodeBorder
                                },
                                shape = shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = value.getOrNull(index)?.toString().orEmpty(),
                            style = EcoTextStyles.Numeros,
                            color = EcoBlack,
                        )
                    }
                }
            }
        },
    )
}
