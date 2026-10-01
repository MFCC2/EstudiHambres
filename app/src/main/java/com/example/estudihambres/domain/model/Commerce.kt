package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que modela un comercio o restaurante afiliado a convenios universitarios.
 *
 * @property id Identificador único del comercio.
 * @property name Nombre comercial del establecimiento.
 * @property description Breve descripción del negocio o tipo de comida/servicio.
 * @property category Categoría (por ejemplo: Comida rápida, Menú del día, Cafetería, Fotocopias).
 * @property discountDescription Detalle del beneficio o descuento para estudiantes con CampusPass.
 * @property latitude Coordenada de latitud para geolocalización.
 * @property longitude Coordenada de longitud para geolocalización.
 * @property address Dirección física del local.
 * @property imageUrl URL o recurso visual representativo del comercio.
 * @property distanceKm Distancia calculada en kilómetros respecto a la ubicación del estudiante.
 */
data class Commerce(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val discountDescription: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val imageUrl: String = "",
    val distanceKm: Double? = null
)
