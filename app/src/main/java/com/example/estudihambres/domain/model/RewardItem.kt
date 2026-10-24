package com.example.estudihambres.domain.model

/**
 * Método operativo para efectuar el canje del beneficio en el mundo real.
 */
enum class RedemptionMethod {
    IN_STORE_QR,   // Canje presencial en caja mostrando código de barras o QR oficial
    ONLINE_CODE,   // Canje digital ingresando el cupón alfanumérico en la web o app del aliado
    DIRECT_LINK    // Reclamación directa mediante verificación en el portal institucional del aliado
}

/**
 * Entidad de dominio que modela un artículo, beneficio o tarjeta de regalo canjeable en la Tienda de StudiPuntos.
 *
 * @property id Identificador único del premio.
 * @property title Título visible de la recompensa (ej. "Cineplanet Entrada 2D Regular").
 * @property category Categoría de la recompensa ("Cine & Ocio", "Comida", "Música", "Educación").
 * @property pointsCost Costo en StudiPuntos para efectuar el canje.
 * @property description Instrucciones o descripción del beneficio.
 * @property partnerName Marca o empresa emisora del premio.
 * @property iconEmoji Emoji representativo del premio.
 * @property sampleCode Código o formato de cupón generado.
 * @property officialUrl Enlace seguro HTTPS hacia los términos oficiales o web del aliado.
 * @property terms Condiciones de uso, vigencia horaria y restricciones en locales.
 * @property redemptionMethod Modo operativo de canje en el mundo real.
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
    val officialUrl: String = "https://estudihambres.pe",
    val terms: String = "Válido a nivel nacional en locales autorizados. Sujeto a stock.",
    val redemptionMethod: RedemptionMethod = RedemptionMethod.ONLINE_CODE,
    val isAvailable: Boolean = true
)
