package com.example.estudihambres.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Representa unas coordenadas geográficas simplificadas para la capa de dominio.
 *
 * @property latitude Latitud en grados decimales.
 * @property longitude Longitud en grados decimales.
 */
data class UserCoordinates(
    val latitude: Double,
    val longitude: Double
)

/**
 * Contrato de repositorio para obtener la ubicación física del dispositivo del estudiante.
 */
interface LocationRepository {
    /**
     * Emite la última ubicación conocida o la ubicación actual periódica del usuario.
     */
    fun getUserLocation(): Flow<UserCoordinates?>

    /**
     * Consulta puntual de la última ubicación registrada del estudiante.
     */
    suspend fun getLastKnownLocation(): UserCoordinates?
}
