package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que representa un lugar o establecimiento físico (restaurante, cafetería, librería).
 *
 * @property id Identificador único del lugar.
 * @property name Nombre comercial del establecimiento.
 * @property category Categoría del comercio (ej. Comida rápida, Menú ejecutivo, Cafetería).
 * @property address Dirección física del local.
 * @property latitude Coordenada de latitud.
 * @property longitude Coordenada de longitud.
 * @property rating Calificación promedio de estudiantes (1.0 a 5.0).
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
    val distanceKm: Double? = null
)
