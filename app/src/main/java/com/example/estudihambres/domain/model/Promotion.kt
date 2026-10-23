package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que modela un beneficio, convenio o promoción activa para estudiantes universitarios.
 *
 * @property id Identificador único de la promoción.
 * @property title Nombre del beneficio (ej. "Bembos 2x1 en Hamburguesas").
 * @property description Términos y detalles del canje.
 * @property category Categoría temática (Comida, Herramientas digitales, Transporte, Cine, Educación).
 * @property discountTag Etiqueta llamativa del descuento (ej. "2x1", "-50%", "GRATIS").
 * @property partnerName Nombre de la marca o comercio aliado.
 * @property estimatedSavingsSoles Monto estimado de ahorro en Soles (PEN).
 * @property isFeatured Indica si la oferta debe resaltarse en el catálogo principal.
 * @property url Enlace web oficial verificado de la oferta o términos de la promoción.
 * @property distanceText Distancia aproximada o disponibilidad digital.
 * @property originalPrice Precio referencial sin descuento.
 * @property discountedPrice Precio final para estudiantes universitarios.
 * @property isValidated Indica si la oferta ha sido validada mediante API institucional o portal oficial.
 * @property verificationSource Entidad o método que valida la oferta (ej. "Portal Oficial SheerID", "Convenio SUNEDU").
 * @property officialOfferUrl URL garantizada para redirección externa al sitio del aliado.
 */
data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val discountTag: String,
    val partnerName: String,
    val estimatedSavingsSoles: Double,
    val isFeatured: Boolean = false,
    val url: String = "https://estudihambres.pe",
    val distanceText: String = "",
    val originalPrice: String = "",
    val discountedPrice: String = "",
    val isValidated: Boolean = true,
    val verificationSource: String = "Portal Oficial Verificado",
    val officialOfferUrl: String = url
)
