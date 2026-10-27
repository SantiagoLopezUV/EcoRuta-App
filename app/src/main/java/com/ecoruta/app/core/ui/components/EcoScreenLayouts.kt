package com.ecoruta.app.core.ui.components

// Estructuras base de diseño para pantallas del módulo ciudadano
// Incluye plantillas para flujos por pasos, vistas con pestañas inferiores y pantallas de enfoque

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.components.EcoBottomBar
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Contenedor para flujos guiados por pasos con indicador segmentado y acciones fijas en la parte inferior
@Composable
fun EcoWizardLayout(
    step: Int,
    totalSteps: Int,
    stageLabel: String,
    title: String,
    description: String,
    onBack: () -> Unit,
    bottomActions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EcoTopBar(onBackClick = onBack)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.wizard_step, step, totalSteps), style = EcoTextStyles.Subtitulo, color = EcoPrimary)
                    Text(stageLabel, style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(totalSteps) { index ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (index < step) EcoPrimary else EcoBorder),
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = EcoTextStyles.Titulo, color = EcoTitle)
                Text(description, style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
            }
            content()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = bottomActions,
        )
    }
}

// Contenedor para pantallas con barra de navegación inferior y contenido desplazable
@Composable
fun EcoTabScreenLayout(
    selectedTab: CitizenTab,
    onBack: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    topBarAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EcoTopBar(onBackClick = onBack, centerAction = topBarAction)
            content()
        }
        if (bottomActions != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = bottomActions,
            )
        }
        EcoBottomBar(
            items = CitizenTab.entries,
            selectedItem = selectedTab,
            onItemClick = onTabClick,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

// Recuadro informativo con fondo suave e icono de aviso a la izquierda
@Composable
fun EcoInfoHint(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EcoFieldBackground)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Text(text, style = EcoTextStyles.Subtitulo, color = EcoTitle, modifier = Modifier.weight(1f))
    }
}

// Etiqueta compacta con bordes redondeados para representar estados
@Composable
fun EcoStatusChip(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = EcoTextStyles.EtiquetaCampo,
        color = content,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// Etiqueta de texto secundario para encabezar valores informativos
@Composable
fun EcoDataLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = EcoTextStyles.EtiquetaCampo, color = EcoSubtitle, modifier = modifier)
}

// Contenedor para pantallas de enfoque sin barra inferior cuyas acciones suben con el teclado
@Composable
fun EcoFocusLayout(
    onBack: () -> Unit,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EcoTopBar(onBackClick = onBack)
            content()
        }
        if (bottomActions != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = bottomActions,
            )
        }
    }
}
