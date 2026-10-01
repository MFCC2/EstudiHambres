package com.example.estudihambres.domain.usecase

import com.example.estudihambres.core.util.Constants
import com.example.estudihambres.domain.model.Place
import com.example.estudihambres.domain.repository.UserCoordinates
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Caso de uso que aplica el filtro esférico Haversine sobre la lista de lugares afiliados,
 * garantizando que solo se visualicen comercios dentro del radar de hasta 30 km (Agente 4: Geolocalización).
 */
class FilterPlacesByDistanceUseCase {

    /**
     * Filtra los lugares dentro del radio máximo permitido y los ordena por cercanía.
     *
     * @param userLocation Coordenadas de referencia del usuario (o ubicación por defecto del campus).
     * @param places Lista de lugares candidatos.
     * @param maxDistanceKm Radio máximo en kilómetros (por defecto 30 km).
     * @return Lista de lugares dentro del radio de 30 km con su [Place.distanceKm] calculado.
     */
    operator fun invoke(
        userLocation: UserCoordinates,
        places: List<Place>,
        maxDistanceKm: Double = Constants.MAX_DISTANCE_KM
    ): List<Place> {
        return places.mapNotNull { place ->
            val distance = calculateDistanceKm(
                lat1 = userLocation.latitude,
                lon1 = userLocation.longitude,
                lat2 = place.latitude,
                lon2 = place.longitude
            )
            if (distance <= maxDistanceKm) {
                place.copy(distanceKm = distance)
            } else {
                null
            }
        }.sortedBy { it.distanceKm }
    }

    /**
     * Calcula la distancia en kilómetros entre dos coordenadas usando la fórmula del Haversine.
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val targetLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(originLatRad) * cos(targetLatRad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return Constants.EARTH_RADIUS_KM * c
    }
}
