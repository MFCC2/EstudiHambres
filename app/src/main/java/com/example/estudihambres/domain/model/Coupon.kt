package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que modela un cupón o descuento disponible para universitarios.
 *
 * @property id Identificador único del cupón.
 * @property title Título o beneficio del cupón (ej. "2x1 en Pizzas Medianas").
 * @property description Términos o detalle del beneficio.
 * @property discountPercentage Porcentaje de descuento aplicable, si aplica.
 * @property placeId Identificador del lugar o comercio asociado.
 * @property placeName Nombre del comercio emisor.
 * @property expirationDate Fecha límite de validez.
 * @property isAvailable Indica si el cupón está activo y puede ser canjeado.
 */
data class Coupon(
    val id: String,
    val title: String,
    val description: String,
    val discountPercentage: Int? = null,
    val placeId: String,
    val placeName: String,
    val expirationDate: String,
    val isAvailable: Boolean = true
)
