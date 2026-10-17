package com.example.estudihambres.qa

import com.example.estudihambres.domain.model.RedeemedCoupon
import com.example.estudihambres.domain.usecase.RedeemCouponUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de Pruebas QA para la funcionalidad de Canje de Cupones, Tokens Oficiales y Ahorro Acumulado.
 * Valida 10 situaciones críticas de negocio sobre generación de vales, algoritmos de ahorro y ciclo de vida.
 */
class FunctionalRedemptionAndSavingsQaTest {

    private val redeemCouponUseCase = RedeemCouponUseCase()

    @Test
    fun testRedeemCouponUseCaseGeneratesValidFormatToken() {
        val coupon = redeemCouponUseCase(
            partnerName = "Bembos",
            title = "Combo Universitario 2x1 en hamburguesas clásicas",
            category = "Comida",
            discountBadge = "50% DCTO",
            couponId = "cu-01"
        )

        assertNotNull(coupon.id)
        assertTrue(coupon.id.startsWith("red-"))
        assertEquals("Bembos", coupon.partnerName)
        assertEquals("50% DCTO", coupon.discountBadge)

        // Validación estricta del patrón de código de canje CP-BEMB-XXXX
        val codePattern = Regex("^CP-[A-Z0-9]{4}-[A-Z0-9]{4}$")
        assertTrue("El código '${coupon.redemptionCode}' debe coincidir con el formato CP-XXXX-YYYY",
            coupon.redemptionCode.matches(codePattern))
        assertTrue(coupon.redemptionCode.startsWith("CP-BEMB-"))
    }

    @Test
    fun testRedeemCouponCalculatesPercentageSavingsCorrectly() {
        val coupon50 = redeemCouponUseCase(
            partnerName = "Spotify",
            title = "Plan Estudiante",
            category = "Digital",
            discountBadge = "50% DCTO"
        )
        // 50% de S/ 32.00 = S/ 16.00
        assertEquals(16.0, coupon50.savingsAmountSoles, 0.01)

        val coupon30 = redeemCouponUseCase(
            partnerName = "KFC",
            title = "Mega Box",
            category = "Comida",
            discountBadge = "30% OFF"
        )
        // 30% de S/ 32.00 = S/ 9.60
        assertEquals(9.60, coupon30.savingsAmountSoles, 0.01)
    }

    @Test
    fun testRedeemCouponCalculatesTwoForOneSavingsCorrectly() {
        val coupon = redeemCouponUseCase(
            partnerName = "Starbucks",
            title = "2x1 en Frappuccinos",
            category = "Cafetería",
            discountBadge = "2x1 PROMO"
        )
        assertEquals(16.50, coupon.savingsAmountSoles, 0.01)
    }

    @Test
    fun testRedeemCouponCalculatesFreeItemsSavingsCorrectly() {
        val coupon = redeemCouponUseCase(
            partnerName = "Café Cultural",
            title = "Café americano gratis con consumo mínimo",
            category = "Cafetería",
            discountBadge = "GRATIS"
        )
        assertEquals(14.00, coupon.savingsAmountSoles, 0.01)
    }

    @Test
    fun testRedeemCouponCalculatesFixedPriceSavingsCorrectly() {
        val coupon = redeemCouponUseCase(
            partnerName = "Pizzería La Previa",
            title = "Pizza personal + bebida",
            category = "Pizzería",
            discountBadge = "S/ 11.90"
        )
        // Precio regular S/ 24.00 - S/ 11.90 = S/ 12.10
        assertEquals(12.10, coupon.savingsAmountSoles, 0.01)
    }

    @Test
    fun testRedeemedCouponExpirationTime() {
        val now = System.currentTimeMillis()
        val coupon = redeemCouponUseCase(
            partnerName = "Cineplanet",
            title = "Medio precio en entradas",
            category = "Entretenimiento",
            discountBadge = "50% DCTO"
        )

        val expected48hMillis = 48L * 60 * 60 * 1000
        val actualDuration = coupon.expiresAtMillis - coupon.redeemedAtMillis
        assertEquals(expected48hMillis, actualDuration)
        assertFalse(coupon.isExpired)
        assertFalse(coupon.isUsed)
    }

    @Test
    fun testRedeemedCouponExpiredCondition() {
        val expiredCoupon = RedeemedCoupon(
            id = "red-exp-01",
            title = "Promoción Pasada",
            partnerName = "Papa John's",
            category = "Pizzería",
            discountBadge = "2x1",
            redemptionCode = "CP-PAPA-1234",
            savingsAmountSoles = 15.0,
            redeemedAtMillis = System.currentTimeMillis() - (50L * 60 * 60 * 1000),
            expiresAtMillis = System.currentTimeMillis() - (2L * 60 * 60 * 1000)
        )

        assertTrue(expiredCoupon.isExpired)
    }

    @Test
    fun testRedeemedCouponFormatters() {
        val coupon = redeemCouponUseCase(
            partnerName = "GitHub",
            title = "Student Developer Pack",
            category = "Digital",
            discountBadge = "GRATIS"
        )

        val redemptionDate = coupon.formattedRedemptionDate
        val expirationDate = coupon.formattedExpirationDate

        assertNotNull(redemptionDate)
        assertTrue(redemptionDate.isNotBlank())
        assertNotNull(expirationDate)
        assertTrue(expirationDate.isNotBlank())
        assertNotEquals(redemptionDate, expirationDate)
    }

    @Test
    fun testPartnerPrefixHandlingShortAndSpecialCharacters() {
        // Nombre con caracteres especiales y espacios
        val code1 = redeemCouponUseCase.generateRedemptionCode("Papa John's Pizza")
        assertTrue(code1.startsWith("CP-PAPA-"))

        // Nombre con tilde y símbolo &
        val code2 = redeemCouponUseCase.generateRedemptionCode("Café & Cowork")
        assertTrue(code2.startsWith("CP-CAFE-") || code2.startsWith("CP-CAF-") || code2.startsWith("CP-C"))

        // Nombre corto (menor a 4 letras) -> rellenado con 'X'
        val code3 = redeemCouponUseCase.generateRedemptionCode("KFC")
        assertTrue(code3.startsWith("CP-KFCX-"))
    }

    @Test
    fun testRedeemCouponUniqueTokens() {
        val generatedCodes = mutableSetOf<String>()
        val iterations = 50

        for (i in 1..iterations) {
            val coupon = redeemCouponUseCase(
                partnerName = "Bembos",
                title = "Promo $i",
                category = "Comida",
                discountBadge = "2x1"
            )
            generatedCodes.add(coupon.redemptionCode)
        }

        // Cada código generado debe ser 100% único
        assertEquals(iterations, generatedCodes.size)
    }
}
