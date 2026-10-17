package com.example.estudihambres.domain.usecase

import com.example.estudihambres.domain.model.RedeemedCoupon
import java.util.UUID

/**
 * Caso de uso responsable de procesar la redención de cupones y beneficios estudiantiles.
 * Genera tokens de canje únicos con formato oficial "CP-[ALIADO]-[TOKEN]", calcula
 * el ahorro estimado en Soles (PEN) y establece la ventana de validez institucional (48 horas).
 */
class RedeemCouponUseCase {

    /**
     * Procesa y genera un nuevo vale canjeado.
     *
     * @param partnerName Nombre del comercio o marca aliada (ej. "Bembos", "Starbucks").
     * @param title Descripción del beneficio otorgado (ej. "2x1 Hamburguesas Clásicas").
     * @param category Categoría del beneficio (ej. "Comida", "Cafetería").
     * @param discountBadge Etiqueta de descuento (ej. "50% DCTO", "2x1").
     * @param couponId Identificador de origen opcional.
     * @return Instancia inmutable de [RedeemedCoupon] con token único de validación.
     */
    operator fun invoke(
        partnerName: String,
        title: String,
        category: String,
        discountBadge: String,
        couponId: String = ""
    ): RedeemedCoupon {
        val uniqueId = "red-${UUID.randomUUID().toString().take(8)}"
        val code = generateRedemptionCode(partnerName)
        val savings = calculateEstimatedSavings(discountBadge)

        return RedeemedCoupon(
            id = uniqueId,
            couponId = couponId.ifBlank { uniqueId },
            title = title.trim(),
            partnerName = partnerName.trim(),
            category = category.trim(),
            discountBadge = discountBadge.trim(),
            redemptionCode = code,
            savingsAmountSoles = savings,
            redeemedAtMillis = System.currentTimeMillis()
        )
    }

    /**
     * Genera un código token seguro y legible para presentación física en caja o portal web.
     * Formato: CP-{PREFIJO_ALIADO_4LETRAS}-{TOKEN_ALEATORIO_4HEX}
     * Ejemplo: "CP-BEMB-8F2A"
     */
    fun generateRedemptionCode(partnerName: String): String {
        val cleanPartner = partnerName
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .uppercase()
        val prefix = when {
            cleanPartner.length >= 4 -> cleanPartner.take(4)
            cleanPartner.isNotEmpty() -> cleanPartner.padEnd(4, 'X')
            else -> "CAMP"
        }
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(4).uppercase()
        return "CP-$prefix-$randomSuffix"
    }

    /**
     * Calcula una estimación conservadora del ahorro en Soles (PEN) para el balance de la cuenta.
     */
    fun calculateEstimatedSavings(discountBadge: String): Double {
        val cleanBadge = discountBadge.uppercase().trim()

        // 1. Detección de porcentaje: e.g. "50% DCTO", "40% OFF"
        val percentRegex = Regex("(\\d+)%")
        val match = percentRegex.find(cleanBadge)
        if (match != null) {
            val percent = match.groupValues[1].toDoubleOrNull() ?: 30.0
            val baseOrderAmount = 32.0 // Valor medio de compra universitaria en Soles
            return (baseOrderAmount * (percent / 100.0) * 100.0).toLong() / 100.0
        }

        // 2. Detección de promociones 2x1
        if (cleanBadge.contains("2X1")) {
            return 16.50
        }

        // 3. Beneficios gratuitos (ej. bebida gratis, café, postre)
        if (cleanBadge.contains("GRATIS") || cleanBadge.contains("FREE")) {
            return 14.00
        }

        // 4. Precios fijos especiales: e.g. "S/ 11.90" (asumiendo precio original S/ 22.00)
        val solPriceRegex = Regex("S/\\s*([0-9]+(?:\\.[0-9]+)?)")
        val solMatch = solPriceRegex.find(cleanBadge)
        if (solMatch != null) {
            val specialPrice = solMatch.groupValues[1].toDoubleOrNull() ?: 12.0
            val regularPrice = 24.00
            val diff = regularPrice - specialPrice
            if (diff > 0) return diff
        }

        return 12.50
    }
}
