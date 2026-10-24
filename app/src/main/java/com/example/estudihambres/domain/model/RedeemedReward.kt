package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que representa un premio o tarjeta de regalo canjeada exitosamente por el estudiante.
 *
 * @property id Identificador único de la transacción de canje.
 * @property rewardTitle Nombre del artículo canjeado.
 * @property pointsCost Puntos descontados de la billetera.
 * @property redemptionCode Código alfanumérico único para ingresar en la app o web del aliado.
 * @property redeemedAtFormatted Fecha y hora legible en que se realizó el canje.
 * @property partnerName Emisor del beneficio.
 * @property iconEmoji Emoji visual del premio.
 * @property isUsed Indica si el vale ya fue presentado y marcado como utilizado en caja o en la web.
 * @property expiresAtMillis Momento en que expira la vigencia del vale (milisegundos).
 * @property securityToken Token rotativo efímero anti-captura de pantalla (anti-screenshot).
 * @property officialUrl Enlace oficial del aliado para canjear en línea.
 * @property redemptionMethod Método de canje (en tienda o digital).
 */
data class RedeemedReward(
    val id: String,
    val rewardTitle: String,
    val pointsCost: Int,
    val redemptionCode: String,
    val redeemedAtFormatted: String,
    val partnerName: String,
    val iconEmoji: String,
    val isUsed: Boolean = false,
    val expiresAtMillis: Long = System.currentTimeMillis() + (48L * 3600 * 1000),
    val securityToken: String = (100000..999999).random().toString(),
    val officialUrl: String = "https://estudihambres.pe",
    val redemptionMethod: RedemptionMethod = RedemptionMethod.ONLINE_CODE
)
