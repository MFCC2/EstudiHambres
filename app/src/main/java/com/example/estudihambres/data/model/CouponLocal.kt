package com.example.estudihambres.data.model

import com.example.estudihambres.domain.model.Coupon

/**
 * Modelo de datos local para la persistencia o representación en memoria de un cupón.
 *
 * @property id Identificador del cupón.
 * @property title Nombre del beneficio.
 * @property description Términos de la oferta.
 * @property discountPercentage Porcentaje de rebaja.
 * @property placeId Identificador del local.
 * @property placeName Nombre del comercio emisor.
 * @property expirationDate Fecha de expiración.
 * @property isAvailable Disponibilidad de canje.
 */
data class CouponLocal(
    val id: String,
    val title: String,
    val description: String,
    val discountPercentage: Int?,
    val placeId: String,
    val placeName: String,
    val expirationDate: String,
    val isAvailable: Boolean = true
) {
    /**
     * Transforma el modelo de datos local a la entidad pura de dominio [Coupon].
     */
    fun toDomain(): Coupon = Coupon(
        id = id,
        title = title,
        description = description,
        discountPercentage = discountPercentage,
        placeId = placeId,
        placeName = placeName,
        expirationDate = expirationDate,
        isAvailable = isAvailable
    )
}
