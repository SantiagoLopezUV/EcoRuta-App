package com.ecoruta.app.features.routes.presentation.map

// Estado inmutable del mapa de rutas y horarios
// Contiene barrio seleccionado, coordenadas del trazado, horarios y errores

import androidx.annotation.StringRes
import java.text.Normalizer

data class Neighborhood(
    val id: String,
    val name: String,
)

enum class RouteLiveStatus {
    NO_INICIADA,
    EN_CURSO,
    FINALIZADA,
}

data class CollectionRoute(
    val neighborhood: Neighborhood,
    val days: List<Int>,
    val startMinute: Int,
    val endMinute: Int,
    val liveStatus: RouteLiveStatus = RouteLiveStatus.NO_INICIADA,
)

enum class NotificationKind {
    PROXIMITY,
    DEBRIS_REQUEST,
    REPORT,
}

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val label: String,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val read: Boolean = false,
    val referenceId: String? = null,
)

data class NeighborhoodSelectionUiState(
    val isLoading: Boolean = true,
    val neighborhoods: List<Neighborhood> = emptyList(),
    val isSample: Boolean = false,
    @param:StringRes val loadError: Int? = null,
    val query: String = "",
    val selectedId: String? = null,
    @param:StringRes val selectionError: Int? = null,
) {
    // Filtra la lista de barrios ignorando tildes y diferencias de mayúsculas
    val visibleNeighborhoods: List<Neighborhood>
        get() = if (query.isBlank()) neighborhoods else neighborhoods.filter { normalize(it.name).contains(normalize(query.trim())) }
}

fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

sealed interface RouteUiState {
    data object Loading : RouteUiState
    data class Loaded(val route: CollectionRoute, val isSample: Boolean) : RouteUiState
    data object NotFound : RouteUiState
    data object Error : RouteUiState
}

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Loaded(val notifications: List<AppNotification>, val isSample: Boolean) : NotificationsUiState
    data object Error : NotificationsUiState
}
