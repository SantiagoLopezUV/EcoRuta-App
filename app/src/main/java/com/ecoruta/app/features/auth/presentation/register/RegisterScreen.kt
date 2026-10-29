package com.ecoruta.app.features.auth.presentation.register

// Componentes compartidos del registro y selección del tipo de cuenta
// Los pasos del registro ciudadano están en archivos propios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoBottomActionBar
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSelectionCard
import com.ecoruta.app.core.ui.components.EcoStepHeader
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

@Composable
fun RegistrationLayout(
    stepLabel: String,
    stageLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
    contentSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    onBackClick: (() -> Unit)? = null,
    bottomActions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        EcoStepHeader(stepLabel = stepLabel, stageLabel = stageLabel, progress = progress, onBackClick = onBackClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
        EcoBottomActionBar(content = bottomActions)
    }
}

@Composable
fun RegistrationTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = EcoTextStyles.Title, color = EcoTitle)
        Text(description, style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

@Composable
fun CenteredBrand(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brand_leaf),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(stringResource(R.string.app_name), style = EcoTextStyles.Title, color = EcoPrimary)
        }
    }
}

@Composable
internal fun HaveAccountRow(onLoginClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.registration_have_account), style = EcoTextStyles.Subtitle, color = EcoTitle)
        EcoTextLink(
            text = stringResource(R.string.registration_login),
            onClick = onLoginClick,
        )
    }
}

@Composable
fun RegistrationTypeScreen(
    selectedType: RegistrationType?,
    onTypeSelected: (RegistrationType) -> Unit,
    onContinue: (RegistrationType) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_type_step),
        stageLabel = stringResource(R.string.registration_type_stage),
        progress = 0.25f,
        contentSpacing = 20.dp,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.registration_continue),
                onClick = { selectedType?.let(onContinue) },
                enabled = selectedType != null,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            HaveAccountRow(onLoginClick = onLoginClick)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_type_title),
            description = stringResource(R.string.registration_type_description),
        )
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RegistrationType.entries.forEach { type ->
                EcoSelectionCard(
                    title = stringResource(type.titleRes),
                    description = stringResource(type.descriptionRes),
                    iconRes = type.iconRes,
                    iconSize = if (type == RegistrationType.COMERCIO) 24.dp else 48.dp,
                    selected = type == selectedType,
                    onClick = { onTypeSelected(type) },
                )
            }
        }
    }
}

@Preview(name = "Tipo de registro", widthDp = 393, heightDp = 852)
@Composable
private fun RegistrationTypePreview() {
    EcoRutaTheme {
        RegistrationTypeScreen(
            selectedType = RegistrationType.CIUDADANO,
            onTypeSelected = {},
            onContinue = {},
            onLoginClick = {},
        )
    }
}
