package com.example.estudihambres.data.model

import com.example.estudihambres.domain.model.Place

/**
 * Modelo de datos local para lugares y restaurantes con ofertas.
 */
data class PlaceLocal(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val discountBadge: String = "DESCUENTO",
    val discountDescription: String = "Beneficio exclusivo mostrando CampusPass",
    val url: String = "https://campuspass.pe"
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
        rating = rating,
        discountBadge = discountBadge,
        discountDescription = discountDescription,
        url = url
    )
}
