package com.example.estudihambres.data.remote.dto

import com.example.estudihambres.domain.model.Promotion

/**
 * Data Transfer Object (DTO) que representa una promoción recibida desde la API REST de convenios universitarios.
 *
 * @property id Identificador remoto de la oferta.
 * @property title Título oficial del descuento o beneficio.
 * @property description Condiciones de canje y restricciones.
 * @property category Clasificación temática.
 * @property discountTag Insignia de descuento (ej. "2x1", "50% DCTO").
 * @property partnerName Razón social o marca aliada.
 * @property estimatedSavings Monto estimado de ahorro en Soles.
 * @property isFeatured Indicador de beneficio destacado en cabecera.
 * @property officialUrl Enlace HTTPS directo a la página de la promoción.
 * @property isValidated Indicador de verificación por backend o servicio de auditoría de convenios.
 * @property verificationSource Proveedor de verificación (ej. "SheerID", "Portal Oficial Cineplanet", "Convenio SUNEDU").
 * @property originalPrice Precio base.
 * @property discountedPrice Precio para estudiantes.
 * @property distanceText Distancia aproximada en campus o indicador "Digital".
 */
data class PromotionDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val discountTag: String,
    val partnerName: String,
    val estimatedSavings: Double,
    val isFeatured: Boolean = false,
    val officialUrl: String,
    val isValidated: Boolean = true,
    val verificationSource: String = "Portal Oficial",
    val originalPrice: String = "",
    val discountedPrice: String = "",
    val distanceText: String = ""
) {
    /**
     * Mapea el DTO remoto al modelo de dominio [Promotion].
     */
    fun toDomain(): Promotion {
        return Promotion(
            id = id,
            title = title,
            description = description,
            category = category,
            discountTag = discountTag,
            partnerName = partnerName,
            estimatedSavingsSoles = estimatedSavings,
            isFeatured = isFeatured,
            url = officialUrl,
            distanceText = distanceText,
            originalPrice = originalPrice,
            discountedPrice = discountedPrice,
            isValidated = isValidated,
            verificationSource = verificationSource,
            officialOfferUrl = officialUrl
        )
    }
}
