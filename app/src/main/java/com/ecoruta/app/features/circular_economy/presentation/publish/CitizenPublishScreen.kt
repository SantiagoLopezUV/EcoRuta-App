package com.ecoruta.app.features.circular_economy.presentation.publish

// Componentes compartidos de reciclaje e inicio de publicaciones del ciudadano
// Incluye la vista para invitados; publicar, detalle y entrega están en archivos propios

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoFocusLayout
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.GuestServiceActions
import com.ecoruta.app.core.ui.components.GuestServiceContent
import com.ecoruta.app.core.ui.components.GuestServiceScreen
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SpanishColombia: Locale = Locale.forLanguageTag("es-CO")

internal fun formatPoints(points: Int): String = NumberFormat.getIntegerInstance(SpanishColombia).format(points)

@StringRes
fun MaterialType.labelRes(): Int = when (this) {
    MaterialType.PET -> R.string.material_pet
    MaterialType.CARTON -> R.string.material_cardboard
    MaterialType.VIDRIO -> R.string.material_glass
    MaterialType.OTRO -> R.string.material_other
}

@StringRes
fun MaterialType.hintRes(): Int = when (this) {
    MaterialType.PET -> R.string.material_hint_pet
    MaterialType.CARTON -> R.string.material_hint_cardboard
    MaterialType.VIDRIO -> R.string.material_hint_glass
    MaterialType.OTRO -> R.string.material_hint_other
}

@StringRes
fun MaterialVolume.labelRes(): Int = when (this) {
    MaterialVolume.BOLSA_PEQUENA -> R.string.volume_small_bag
    MaterialVolume.BOLSA_GRANDE -> R.string.volume_large_bag
    MaterialVolume.VOLUMINOSO -> R.string.volume_bulky
}

// Devuelve el nombre del material seleccionado o la descripción personalizada cuando se elige otro tipo
@Composable
fun RecyclingPublication.materialName(): String =
    otherMaterial?.takeIf { material == MaterialType.OTRO && it.isNotBlank() } ?: stringResource(material.labelRes())

// Etiqueta de estado para las publicaciones de reciclaje según su avance actual
@Composable
fun PublicationStatusChip(status: PublicationStatus, modifier: Modifier = Modifier) {
    val (label, background, content) = when (status) {
        PublicationStatus.DISPONIBLE -> Triple(R.string.publication_status_available, EcoSubtitle, EcoWhite)
        PublicationStatus.EN_CAMINO -> Triple(R.string.publication_status_on_the_way, EcoAccent, EcoBlack)
        PublicationStatus.ENTREGADA -> Triple(R.string.publication_status_delivered, EcoGreenSecondary, EcoWhite)
        PublicationStatus.CANCELADA -> Triple(R.string.publication_status_cancelled, EcoTitle, EcoWhite)
    }
    EcoStatusChip(stringResource(label), background, content, modifier)
}

// Formatea una marca de tiempo en fecha corta con día, mes abreviado y año
fun formatShort(millis: Long): String = SimpleDateFormat("d MMM yyyy", SpanishColombia).format(Date(millis))

// Formatea una marca de tiempo en fecha completa con día, mes y año
fun formatLong(millis: Long): String = SimpleDateFormat("d 'de' MMMM 'de' yyyy", SpanishColombia).format(Date(millis))

// Botón de selección única para elegir el tipo de material o el volumen estimado
@Composable
fun ChoiceOption(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(if (selected) EcoWhite else EcoFieldBackground)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) EcoAccent else EcoBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) EcoAccent else EcoSubtitle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(EcoAccent),
                )
            }
        }
        Text(text, style = EcoTextStyles.LinkText, color = EcoTitle)
    }
}

// Contenedor de enfoque sin barra de navegación inferior para el formulario y el código de entrega
@Composable
fun FocusLayout(
    onBack: () -> Unit,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = EcoFocusLayout(onBack = onBack, bottomActions = bottomActions, content = content)

// Indica si la publicación continúa activa para contabilizarla en el resumen superior
val RecyclingPublication.isOngoing: Boolean
    get() = status == PublicationStatus.DISPONIBLE || status == PublicationStatus.EN_CAMINO

@Composable
internal fun CenteredLoading() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = EcoPrimary) }
}

data class RecyclingHomeActions(
    val onBack: () -> Unit = {},
    val onPublish: () -> Unit = {},
    val onPublicationClick: (RecyclingPublication) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

// Pantalla principal del módulo de reciclaje con saldo de puntos y listado de publicaciones
@Composable
fun RecyclingHomeScreen(state: RecyclingHomeUiState, actions: RecyclingHomeActions) {
    EcoTabScreenLayout(selectedTab = CitizenTab.HOME, onBack = actions.onBack, onTabClick = actions.onTabClick) {
        val loaded = state as? RecyclingHomeUiState.Loaded
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp))
                .background(EcoPrimary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.service_recycling_title), style = EcoTextStyles.Title, color = EcoWhite)
            Text(stringResource(R.string.recycling_description), style = EcoTextStyles.Subtitle, color = EcoWhite)
            Row {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(loaded?.ecoPoints?.let(::formatPoints) ?: "—", style = EcoTextStyles.Numbers, color = EcoWhite)
                    Text(stringResource(R.string.recycling_current_points), style = EcoTextStyles.FieldLabel, color = EcoWhite)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(loaded?.publications?.count { it.isOngoing }?.toString() ?: "—", style = EcoTextStyles.Numbers, color = EcoWhite)
                    Text(stringResource(R.string.recycling_active_publications), style = EcoTextStyles.FieldLabel, color = EcoWhite)
                }
            }
        }
        EcoPrimaryButton(text = stringResource(R.string.recycling_publish), onClick = actions.onPublish, trailingIconRes = R.drawable.ic_arrow_right)
        Text(stringResource(R.string.recycling_my_publications), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.padding(top = 4.dp))
        when (state) {
            RecyclingHomeUiState.Loading -> CenteredLoading()
            RecyclingHomeUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EcoErrorMessage(text = stringResource(R.string.recycling_load_error))
                EcoTextLink(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            is RecyclingHomeUiState.Loaded -> {
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.recycling_sample_hint))
                if (state.publications.isEmpty()) {
                    Text(stringResource(R.string.recycling_empty), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.publications.forEach { publication ->
                        PublicationCard(publication, onClick = { actions.onPublicationClick(publication) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicationCard(publication: RecyclingPublication, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.recycling_view_detail), onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_recycle), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
            Text(publication.materialName(), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
            PublicationStatusChip(publication.status)
        }
        Text(stringResource(publication.volume.labelRes()), style = EcoTextStyles.Subtitle, color = EcoTitle)
        Row(verticalAlignment = Alignment.CenterVertically) {
            EcoDataLabel(
                stringResource(R.string.recycling_date_code, formatShort(publication.createdAtMillis), publication.code),
                Modifier.weight(1f),
            )
            Text(stringResource(R.string.recycling_view_detail_arrow), style = EcoTextStyles.LinkText, color = EcoPrimary)
        }
    }
}

// Vista informativa del servicio de reciclaje para usuarios que aún no han iniciado sesión
@Composable
fun RecyclingGuestScreen(actions: GuestServiceActions) {
    GuestServiceScreen(
        content = GuestServiceContent(
            banner = R.string.recycling_guest_banner,
            lockedSection = R.string.recycling_guest_locked,
            title = R.string.service_recycling_title,
            description = R.string.service_recycling_description,
            benefits = listOf(R.string.recycling_guest_can_1, R.string.recycling_guest_can_2, R.string.recycling_guest_can_3),
        ),
        actions = actions,
    )
}

internal val previewPublicationOnTheWay = RecyclingPublication(
    id = "pub-1",
    code = "ECO-REC-3041",
    material = MaterialType.PET,
    volume = MaterialVolume.BOLSA_GRANDE,
    address = "Cra. 22E #6-40",
    area = "Torobajo",
    status = PublicationStatus.EN_CAMINO,
    createdAtMillis = 1_744_800_000_000L,
    recycler = AssignedRecycler(
        name = "Luis Fernando Córdoba",
        affiliation = "Asociación Recicladores del Sur",
        phone = "3167204918",
    ),
)

internal val previewPublicationDelivered = previewPublicationOnTheWay.copy(
    id = "pub-2",
    code = "ECO-REC-2980",
    material = MaterialType.CARTON,
    status = PublicationStatus.ENTREGADA,
    deliveredAtMillis = 1_744_850_000_000L,
    pointsEarned = 80,
    balanceAfter = 1_330,
)

@Preview(name = " Inicio", widthDp = 393, heightDp = 900)
@Composable
private fun RecyclingHomePreview() {
    EcoRutaTheme {
        RecyclingHomeScreen(
            RecyclingHomeUiState.Loaded(
                ecoPoints = 1_250,
                publications = listOf(previewPublicationOnTheWay, previewPublicationDelivered),
                isSample = false,
            ),
            RecyclingHomeActions(),
        )
    }
}

@Preview(name = "Reciclaje sin sesión", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclingGuestPreview() {
    EcoRutaTheme {
        RecyclingGuestScreen(actions = GuestServiceActions())
    }
}
