package com.example.estudihambres.data.model

import com.example.estudihambres.domain.model.Place

/**
 * Modelo de datos local para la representación de lugares o comercios guardados localmente.
 *
 * @property id Identificador del local.
 * @property name Nombre comercial.
 * @property category Categoría del local.
 * @property address Dirección física.
 * @property latitude Latitud.
 * @property longitude Longitud.
 * @property rating Calificación promedio.
 */
data class PlaceLocal(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double
) {
    /**
     * Convierte el modelo local a la entidad de dominio [Place].
     */
    fun toDomain(): Place = Place(
        id = id,
        name = name,
        category = category,
        address = address,
        latitude = latitude,
        longitude = longitude,
        rating = rating
    )
}
