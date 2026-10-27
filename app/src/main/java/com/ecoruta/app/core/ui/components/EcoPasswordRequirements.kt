package com.ecoruta.app.core.ui.components

// Indicador visual de requisitos de seguridad para contraseñas
// Evalúa en tiempo real la longitud mínima y la presencia de letras y números

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

// Lista de condiciones que se resaltan en verde a medida que la contraseña escrita las cumple
@Composable
fun EcoPasswordRequirements(password: String, modifier: Modifier = Modifier) {
    val requirements = listOf(
        R.string.reset_requirement_length to (password.length >= 8),
        R.string.reset_requirement_letter to password.any { it.isLetter() },
        R.string.reset_requirement_number to password.any { it.isDigit() },
    )
    val metLabel = stringResource(R.string.password_requirement_met)
    val pendingLabel = stringResource(R.string.password_requirement_pending)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reset_requirements_title), style = EcoTextStyles.EtiquetaCampo, color = EcoTitle)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            requirements.forEach { (textRes, met) ->
                val color by animateColorAsState(if (met) EcoGreenSecondary else EcoSubtitle, label = "requisito")
                Row(
                    modifier = Modifier.semantics { stateDescription = if (met) metLabel else pendingLabel },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Text(stringResource(textRes), style = EcoTextStyles.Subtitulo, color = color)
                }
            }
        }
    }
}
