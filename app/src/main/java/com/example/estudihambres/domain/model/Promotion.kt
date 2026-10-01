package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que modela un beneficio, convenio o promoción activa para estudiantes universitarios.
 *
 * @property id Identificador único de la promoción.
 * @property title Nombre del beneficio (ej. "Bembos 2x1 en Hamburguesas").
 * @property description Términos y detalles del canje.
 * @property category Categoría tematica (Comida, Herramientas digitales, Transporte).
 * @property discountTag Etiqueta llamativa del descuento (ej. "2x1", "-50%", "GRATIS").
 * @property partnerName Nombre de la marca o comercio aliado.
 * @property estimatedSavingsSoles Monto estimado de ahorro en Soles (PEN).
 * @property isFeatured Indica si la oferta debe resaltarse en el catálogo principal.
 */
data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val discountTag: String,
    val partnerName: String,
    val estimatedSavingsSoles: Double,
    val isFeatured: Boolean = false
)
