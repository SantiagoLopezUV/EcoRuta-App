package com.ecoruta.app.features.driver.presentation.active_route

// Estado inmutable de la ruta en curso del chofer
// Secuencia de puntos restantes, parada actual, transmisión GPS y progreso total

import androidx.annotation.StringRes
import com.ecoruta.app.R
import com.ecoruta.app.features.driver.presentation.orders.DriverOrderStatus

// Tipo de ruta asignada, la ordinaria sigue un recorrido fijo y la dinámica atiende reportes aprobados
enum class DriverRouteType(@StringRes val labelRes: Int) {
    ORDINARY(R.string.driver_order_kind_ordinary),
    DYNAMIC(R.string.driver_order_kind_cleanup),
}

// Motivo del reporte que originó un punto de una ruta dinámica
enum class RoutePointReason(@StringRes val labelRes: Int) {
    FULL_CONTAINER(R.string.driver_point_full_container),
    ILLEGAL_DUMP(R.string.driver_point_illegal_dump),
}

// Punto de la ruta con su dirección, posición en el mapa ilustrativo e indicaciones para llegar
data class DriverRoutePoint(
    val name: String,
    val address: String,
    val mapX: Float,
    val mapY: Float,
    val reason: RoutePointReason? = null,
    val distanceAway: String = "",
    val travelTime: String = "",
    val directions: String = "",
    val visitedAt: String? = null,
)

// Ruta asignada al chofer con su tipo, zona, horario y secuencia de puntos
data class DriverRoute(
    val code: String,
    val type: DriverRouteType,
    val zone: String,
    val schedule: String,
    val distance: String,
    val status: DriverOrderStatus,
    val points: List<DriverRoutePoint>,
)

// Estado compartido por el detalle, la ruta en curso, el punto visitado y la ruta finalizada
data class ActiveRouteUiState(
    val route: DriverRoute? = null,
    val city: String = "",
    val visitedCount: Int = 0,
    val locationConsent: Boolean = false,
    val isSharingLocation: Boolean = false,
    val completedSchedule: String = "",
    val duration: String = "",
) {
    // Punto que el chofer debe atender a continuación, nulo cuando ya visitó todos
    val nextPoint: DriverRoutePoint? get() = route?.points?.getOrNull(visitedCount)

    // Último punto marcado como visitado, nulo si aún no visita ninguno
    val lastVisitedPoint: DriverRoutePoint? get() = route?.points?.getOrNull(visitedCount - 1)
}

// Texto que acompaña a cada punto, el motivo del reporte en rutas dinámicas o el tipo de ruta en las ordinarias
@StringRes
fun DriverRoute.pointLabelRes(point: DriverRoutePoint): Int = point.reason?.labelRes ?: type.labelRes

// Ruta dinámica ilustrativa de limpieza en el centro de Tuluá
val SampleCleanupRoute = DriverRoute(
    code = "R-014",
    type = DriverRouteType.DYNAMIC,
    zone = "Centro",
    schedule = "08:00–09:00",
    distance = "2,4 km",
    status = DriverOrderStatus.PENDING,
    points = listOf(
        DriverRoutePoint(
            name = "Punto Centro A",
            address = "Calle 25 # 26-18",
            mapX = 0.237f,
            mapY = 0.294f,
            reason = RoutePointReason.FULL_CONTAINER,
            distanceAway = "300 m",
            travelTime = "2 min",
            directions = "Continúa por la carrera 26 hasta calle 25.",
            visitedAt = "08:12",
        ),
        DriverRoutePoint(
            name = "Punto Centro B",
            address = "Carrera 26 # 25-42",
            mapX = 0.477f,
            mapY = 0.494f,
            reason = RoutePointReason.ILLEGAL_DUMP,
            distanceAway = "700 m",
            travelTime = "5 min",
            directions = "Gira a la derecha por la carrera 26.",
        ),
        DriverRoutePoint(
            name = "Punto Centro C",
            address = "Calle 27 # 26-35",
            mapX = 0.237f,
            mapY = 0.814f,
            reason = RoutePointReason.FULL_CONTAINER,
            distanceAway = "450 m",
            travelTime = "3 min",
            directions = "Sigue derecho hasta la calle 27.",
        ),
    ),
)

// Ruta ordinaria ilustrativa del sector norte de Tuluá
val SampleOrdinaryRoute = DriverRoute(
    code = "R-013",
    type = DriverRouteType.ORDINARY,
    zone = "Norte",
    schedule = "06:00–07:00",
    distance = "3,1 km",
    status = DriverOrderStatus.PENDING,
    points = listOf(
        DriverRoutePoint(
            name = "Punto Norte A",
            address = "Calle 30 # 28-14",
            mapX = 0.237f,
            mapY = 0.294f,
            distanceAway = "300 m",
            travelTime = "2 min",
            directions = "Continúa por la carrera 26 hasta calle 25.",
            visitedAt = "06:12",
        ),
        DriverRoutePoint(
            name = "Punto Norte B",
            address = "Carrera 27 # 31-20",
            mapX = 0.477f,
            mapY = 0.494f,
            distanceAway = "700 m",
            travelTime = "5 min",
            directions = "Gira a la derecha por la carrera 27.",
        ),
        DriverRoutePoint(
            name = "Punto Norte C",
            address = "Calle 32 # 27-06",
            mapX = 0.237f,
            mapY = 0.814f,
            distanceAway = "450 m",
            travelTime = "3 min",
            directions = "Sigue derecho hasta la calle 32.",
        ),
    ),
)
