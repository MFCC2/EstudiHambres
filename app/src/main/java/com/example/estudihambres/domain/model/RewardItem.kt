package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que modela un artículo o tarjeta de regalo canjeable en la Tienda de StudiPuntos (Premios).
 *
 * @property id Identificador único del premio.
 * @property title Título visible de la recompensa (ej. "Roblox 400 Robux Card").
 * @property category Categoría de la recompensa ("Gaming", "Música", "Comida", "Cine", "Educación").
 * @property pointsCost Costo en StudiPuntos para efectuar el canje.
 * @property description Instrucciones o descripción del beneficio.
 * @property partnerName Marca o empresa emisora del premio.
 * @property iconEmoji Emoji representativo del premio.
 * @property sampleCode Código o formato de cupón generado.
 * @property isAvailable Disponibilidad de inventario para canje.
 */
data class RewardItem(
    val id: String,
    val title: String,
    val category: String,
    val pointsCost: Int,
    val description: String,
    val partnerName: String,
    val iconEmoji: String,
    val sampleCode: String,
    val isAvailable: Boolean = true
)
