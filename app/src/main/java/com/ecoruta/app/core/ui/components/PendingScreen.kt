package com.ecoruta.app.core.ui.components

// Vista temporal para pantallas que aún están en construcción

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

// Vista temporal utilizada para indicar pantallas que aún están en construcción
@Composable
fun PendingScreen(screenName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = EcoDimens.ScreenHorizontalPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = screenName, style = EcoTextStyles.Title, color = EcoTitle, textAlign = TextAlign.Center)
        Text(
            text = "Pantalla pendiente de implementar",
            style = EcoTextStyles.Subtitle,
            color = EcoSubtitle,
            textAlign = TextAlign.Center,
        )
    }
}
