package com.ecoruta.app.core.ui.components

// Recuadro compartido para las pantallas que todavía no tienen datos que mostrar
// Combina una ilustración, un título y una explicación centrados sobre fondo suave

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

// Recuadro con ilustración, título y explicación para las pestañas que aún no tienen datos
@Composable
fun EcoEmptyStateCard(@DrawableRes illustrationRes: Int, heading: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Image(painterResource(illustrationRes), contentDescription = null, modifier = Modifier.size(104.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(heading, style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(body, style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
