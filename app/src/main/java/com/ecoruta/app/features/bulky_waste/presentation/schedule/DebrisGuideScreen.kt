package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Paso 2 del retiro de escombros: guía ambiental de preparación

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoWizardLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

@Composable
fun DebrisGuideScreen(selectedType: DebrisType?, onContinue: () -> Unit, onBack: () -> Unit) {
    EcoWizardLayout(
        step = 2,
        totalSteps = DEBRIS_STEPS,
        stageLabel = stringResource(R.string.debris_stage_guide),
        title = stringResource(R.string.debris_guide_title),
        description = stringResource(R.string.debris_guide_description),
        onBack = onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.debris_choose_date),
                onClick = onContinue,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        val bulkySelected = selectedType == DebrisType.MUEBLES || selectedType == DebrisType.COLCHONES
        GuideCard(
            iconRes = R.drawable.ic_sofa,
            title = stringResource(R.string.debris_guide_bulky_title),
            category = stringResource(R.string.debris_guide_bulky_category),
            body = stringResource(R.string.debris_guide_bulky_body),
            highlighted = bulkySelected,
        )
        GuideCard(
            iconRes = R.drawable.ic_bricks,
            title = stringResource(R.string.debris_type_rubble),
            category = stringResource(R.string.debris_guide_rcd_category),
            body = stringResource(R.string.debris_guide_rcd_body),
            highlighted = selectedType == DebrisType.ESCOMBROS_REMODELACION,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoPrimary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_shield_check), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.debris_guide_care_title), style = EcoTextStyles.ButtonText, color = EcoWhite)
            }
            Text(stringResource(R.string.debris_guide_care_body), style = EcoTextStyles.Subtitle, color = EcoWhite)
        }
    }
}

@Composable
private fun GuideCard(iconRes: Int, title: String, category: String, body: String, highlighted: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) EcoWhite else EcoFieldBackground)
            .border(if (highlighted) 2.dp else 0.dp, if (highlighted) EcoAccent else Color.Transparent, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(iconRes), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
            Text(title, style = EcoTextStyles.ButtonText, color = EcoTitle)
        }
        Text(
            text = if (highlighted) stringResource(R.string.debris_guide_your_selection, category) else category,
            style = EcoTextStyles.FieldLabel,
            color = if (highlighted) EcoPrimary else EcoSubtitle,
        )
        Text(body, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

// Tercer paso del flujo para elegir la fecha preferida dentro del calendario semanal disponible

@Preview(name = "Paso 2 Guía", widthDp = 393, heightDp = 900)
@Composable
private fun DebrisGuidePreview() {
    EcoRutaTheme {
        DebrisGuideScreen(
            selectedType = DebrisType.MUEBLES,
            onContinue = {},
            onBack = {},
        )
    }
}
