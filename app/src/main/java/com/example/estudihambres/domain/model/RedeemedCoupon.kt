package com.example.estudihambres.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Entidad de dominio que modela un cupón efectivamente canjeado por el estudiante universitario.
 * Incluye el token único de validación (código de barra/alfanumérico), fecha de emisión,
 * vencimiento y el ahorro real estimado sumado a su billetera de ahorros.
 *
 * @property id Identificador único de la transacción de canje.
 * @property couponId Identificador del cupón o promoción de origen.
 * @property title Beneficio otorgado (ej. "2x1 Hamburguesas Clásicas").
 * @property partnerName Nombre del comercio o aliado oficial (ej. "Bembos", "Starbucks").
 * @property category Categoría del beneficio (ej. "Comida", "Cafetería", "Digital").
 * @property discountBadge Etiqueta visual de descuento (ej. "50% DCTO", "2x1").
 * @property redemptionCode Token oficial de canje (ej. "CP-BEMB-7294").
 * @property savingsAmountSoles Monto estimado ahorrado en Soles (ej. 15.0).
 * @property redeemedAtMillis Marca temporal en milisegundos del momento de canje.
 * @property expiresAtMillis Fecha límite de redención en caja o web (48h posteriores por defecto).
 * @property isUsed Indica si el cupón ya fue marcado como utilizado en caja.
 */
data class RedeemedCoupon(
    val id: String,
    val couponId: String = "",
    val title: String,
    val partnerName: String,
    val category: String,
    val discountBadge: String,
    val redemptionCode: String,
    val savingsAmountSoles: Double,
    val redeemedAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = redeemedAtMillis + (48L * 60 * 60 * 1000), // 48 horas
    val isUsed: Boolean = false
) {
    /**
     * Retorna verdadero si el cupón ha sobrepasado su ventana de vigencia.
     */
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAtMillis

    /**
     * Representación amigable de la fecha de canje (ej. "17 Oct 2026, 11:30 am").
     */
    val formattedRedemptionDate: String
        get() = try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.forLanguageTag("es-PE"))
            sdf.format(Date(redeemedAtMillis))
        } catch (_: Exception) {
            "Hoy"
        }

    /**
     * Representación amigable de la fecha de caducidad.
     */
    val formattedExpirationDate: String
        get() = try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.forLanguageTag("es-PE"))
            sdf.format(Date(expiresAtMillis))
        } catch (_: Exception) {
            "En 48 horas"
        }
}
