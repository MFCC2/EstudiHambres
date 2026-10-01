package com.example.estudihambres.domain.usecase

import com.example.estudihambres.domain.model.Commerce
import com.example.estudihambres.domain.repository.UserCoordinates
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Caso de uso que aplica el filtro de distancia para comercios afiliados.
 *
 * Utiliza la fórmula del Haversine para determinar la distancia esférica real entre el estudiante
 * y el comercio, asegurando que solo se listen comercios dentro del radio estipulado (por defecto 30 km).
 */
class FilterCommercesByDistanceUseCase {

    /**
     * Filtra y ordena los comercios que se encuentran a una distancia máxima de [maxDistanceKm]
     * respecto a las coordenadas del usuario.
     *
     * @param userLocation Coordenadas geográficas actuales del usuario.
     * @param commerces Lista de comercios candidatos.
     * @param maxDistanceKm Radio máximo permitido en kilómetros (30.0 km según PROJECT_RULES).
     * @return Lista de comercios dentro del radio con su [Commerce.distanceKm] calculado, ordenada de menor a mayor distancia.
     */
    operator fun invoke(
        userLocation: UserCoordinates,
        commerces: List<Commerce>,
        maxDistanceKm: Double = 30.0
    ): List<Commerce> {
        return commerces.mapNotNull { commerce ->
            val distance = calculateHaversineDistance(
                lat1 = userLocation.latitude,
                lon1 = userLocation.longitude,
                lat2 = commerce.latitude,
                lon2 = commerce.longitude
            )
            if (distance <= maxDistanceKm) {
                commerce.copy(distanceKm = distance)
            } else {
                null
            }
        }.sortedBy { it.distanceKm }
    }

    /**
     * Calcula la distancia en kilómetros entre dos pares de coordenadas geográficas usando Haversine.
     */
    fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val targetLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(originLatRad) * cos(targetLatRad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }
}
