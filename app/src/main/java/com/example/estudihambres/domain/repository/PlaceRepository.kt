package com.example.estudihambres.domain.repository

import com.example.estudihambres.domain.model.Place
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para la consulta de lugares y comercios afiliados.
 */
interface PlaceRepository {
    /**
     * Emite la lista completa de lugares afiliados a convenios universitarios.
     */
    fun getPlaces(): Flow<List<Place>>

    /**
     * Consulta un lugar específico por su identificador único.
     *
     * @param id Identificador único del lugar.
     */
    suspend fun getPlaceById(id: String): Place?
}
