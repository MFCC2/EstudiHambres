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
 */
data class RedeemedReward(
    val id: String,
    val rewardTitle: String,
    val pointsCost: Int,
    val redemptionCode: String,
    val redeemedAtFormatted: String,
    val partnerName: String,
    val iconEmoji: String
)
