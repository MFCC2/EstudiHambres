package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que representa un establecimiento o restaurante aliado con geolocalización.
 *
 * @property id Identificador único del lugar.
 * @property name Nombre comercial del establecimiento.
 * @property category Categoría (Comida rápida, Cafetería, Menú ejecutivo, etc.).
 * @property address Dirección física del local.
 * @property latitude Coordenada de latitud.
 * @property longitude Coordenada de longitud.
 * @property rating Calificación promedio de estudiantes (1.0 a 5.0).
 * @property discountBadge Etiqueta visual del beneficio (ej. "2x1", "25% OFF").
 * @property discountDescription Detalle de la promoción universitaria aplicable.
 * @property distanceKm Distancia calculada en kilómetros respecto a la ubicación del usuario.
 */
data class Place(
    val id: String,
    val name: String,
    val category: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double = 4.5,
    val discountBadge: String = "PROMO",
    val discountDescription: String = "Descuento especial con carnet universitario CampusPass",
    val distanceKm: Double? = null
)
