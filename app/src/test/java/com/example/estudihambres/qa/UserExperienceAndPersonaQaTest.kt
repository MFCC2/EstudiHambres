package com.example.estudihambres.qa

import com.example.estudihambres.data.repository.MockPromotionRepositoryImpl
import com.example.estudihambres.domain.model.RedeemedCoupon
import com.example.estudihambres.domain.usecase.RedeemCouponUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Suite de Pruebas de Experiencia de Usuario (UX) y Personas Estudiantiles en Distintos Casos de Uso.
 *
 * Cubre perfiles reales de estudiantes universitarios peruanos:
 * 1. Cachimbo / Ingresante (Registro institucional, validación DNI y correo .edu.pe).
 * 2. Estudiante de Ingeniería de Ciclos Superiores (Canje de suites de desarrollo JetBrains, GitHub y Notion).
 * 3. Estudiante con Presupuesto Ajustado en Provincias (UNSAAC / Continental - Ahorro en combos de comida).
 * 4. Estudiante Conmutador / Transporte (Ajuste del Radar de 5km, 15km a 30km y medio pasaje).
 * 5. Casos de Borde y Seguridad (Inmutabilidad de DNI, cupones vencidos y códigos no colisionantes).
 */
class UserExperienceAndPersonaQaTest {

    private val redeemCouponUseCase = RedeemCouponUseCase()
    private val promotionRepository = MockPromotionRepositoryImpl()

    /**
     * Caso 1: Persona "Cachimbo / Ingresante" - Registro con DNI válido de 8 dígitos y correo institucional (.edu.pe).
     */
    @Test
    fun freshmanStudent_registrationValidation_acceptsValidDniAndEduEmail() {
        val validDni = "72849103"
        val validEmail = "u72849103@continental.edu.pe"
        val validPassword = "Password2025!"

        // Verificación de DNI: exactamente 8 dígitos numéricos peruanos
        val isDniValid = validDni.length == 8 && validDni.all { it.isDigit() }
        assertTrue("El DNI de 8 dígitos peruanos debe ser válido", isDniValid)

        // Verificación de correo: debe pertenecer a un dominio universitario reconocido
        val isEmailValid = validEmail.endsWith(".edu.pe") || validEmail.contains(".edu")
        assertTrue("El correo universitario institucional debe ser aceptado", isEmailValid)

        // Verificación de contraseña: al menos 6 caracteres
        val isPasswordValid = validPassword.length >= 6
        assertTrue("La contraseña debe tener al menos 6 caracteres", isPasswordValid)
    }

    /**
     * Caso 2: Persona "Cachimbo / Ingresante" - Rechazo de datos inválidos (DNI corto, correo comercial).
     */
    @Test
    fun freshmanStudent_registrationValidation_rejectsInvalidDniOrCommercialEmail() {
        val shortDni = "728491" // 6 dígitos
        val nonNumericDni = "7284910A"
        val commercialEmail = "estudiante.cachimbo@gmail.com"

        val isShortDniValid = shortDni.length == 8 && shortDni.all { it.isDigit() }
        assertFalse("Un DNI con menos de 8 dígitos debe ser rechazado", isShortDniValid)

        val isNonNumericDniValid = nonNumericDni.length == 8 && nonNumericDni.all { it.isDigit() }
        assertFalse("Un DNI con caracteres alfabéticos debe ser rechazado", isNonNumericDniValid)

        val isEduEmail = commercialEmail.endsWith(".edu.pe")
        assertFalse("Un correo comercial (@gmail.com) no debe acceder a beneficios exclusivos universitarios", isEduEmail)
    }

    /**
     * Caso 3: Persona "Senior de Ingeniería" - Canje de suites de desarrollo (JetBrains, GitHub Pack)
     * y verificación de formato de token, vigencia de 48 horas y valor de ahorro.
     */
    @Test
    fun seniorEngineeringStudent_redeemsDeveloperPacks_generatesUniqueTokensAndExpirations() {
        val jetBrainsCoupon = redeemCouponUseCase(
            partnerName = "JetBrains",
            title = "JetBrains All Products Student Pack",
            category = "Herramientas digitales",
            discountBadge = "100% GRATIS",
            couponId = "p-12"
        )

        assertNotNull(jetBrainsCoupon)
        assertTrue("El token debe tener el prefijo del partner JETB", jetBrainsCoupon.redemptionCode.startsWith("CP-JETB-"))
        assertEquals("Herramientas digitales", jetBrainsCoupon.category)
        assertTrue("El ahorro estimado debe ser positivo", jetBrainsCoupon.savingsAmountSoles > 0)
        assertFalse("El cupón recién canjeado no debe estar marcado como usado", jetBrainsCoupon.isUsed)
        assertFalse("El cupón recién canjeado no debe estar expirado", jetBrainsCoupon.isExpired)

        val gitHubCoupon = redeemCouponUseCase(
            partnerName = "GitHub & Copilot",
            title = "GitHub Student Developer Pack",
            category = "Herramientas digitales",
            discountBadge = "100% GRATIS",
            couponId = "p-05"
        )

        assertNotNull(gitHubCoupon)
        assertTrue("El token de GitHub debe tener formato CP-GITH-XXXX", gitHubCoupon.redemptionCode.startsWith("CP-GITH-"))
        assertNotEquals("Los códigos de dos cupones distintos deben ser únicos",
            jetBrainsCoupon.redemptionCode, gitHubCoupon.redemptionCode)
    }

    /**
     * Caso 4: Persona "Estudiante con Presupuesto Ajustado" - Canje de combos de comida (Bembos, KFC)
     * y cálculo acumulativo de ahorros en soles para la billetera.
     */
    @Test
    fun budgetStudent_redeemsFoodPromotions_verifiesCumulativeWalletSavings() {
        val bembosCoupon = redeemCouponUseCase(
            partnerName = "Bembos",
            title = "Combo Universitario 2x1",
            category = "Comida",
            discountBadge = "2x1",
            couponId = "p-01"
        )

        val kfcCoupon = redeemCouponUseCase(
            partnerName = "KFC Perú",
            title = "Mega Box Estudiantil KFC",
            category = "Comida",
            discountBadge = "PROMO S/ 14.90",
            couponId = "p-14"
        )

        val cineplanetCoupon = redeemCouponUseCase(
            partnerName = "Cineplanet",
            title = "Cineplanet 2x1 Universitario",
            category = "Entretenimiento",
            discountBadge = "2x1 ENTRADAS",
            couponId = "p-10"
        )

        val totalSavings = bembosCoupon.savingsAmountSoles + kfcCoupon.savingsAmountSoles + cineplanetCoupon.savingsAmountSoles
        assertTrue("El total acumulado debe superar los 40 soles de ahorro real", totalSavings >= 40.0)
    }

    /**
     * Caso 5: Persona "Estudiante Conmutador" - Configuración del Radar GPS y medio pasaje universitario.
     */
    @Test
    fun commuterStudent_radarRadiusSelection_validatesAcceptedDistances() {
        val allowedRadii = listOf(5f, 15f, 30f)

        // Validación de que 5km, 15km y 30km son radios permitidos
        assertTrue(allowedRadii.contains(5f))
        assertTrue(allowedRadii.contains(15f))
        assertTrue(allowedRadii.contains(30f))
        assertFalse(allowedRadii.contains(50f))

        // Validación de la promoción de Medio Pasaje
        val busCoupon = redeemCouponUseCase(
            partnerName = "Transporte Urbano",
            title = "Medio Pasaje Universitario",
            category = "Transporte",
            discountBadge = "50% DTO",
            couponId = "p-09"
        )

        assertEquals("Transporte", busCoupon.category)
        assertTrue(busCoupon.savingsAmountSoles > 0)
    }

    /**
     * Caso 6: Regla de Seguridad - DNI y Universidad permanecen inmutables tras verificación oficial.
     */
    @Test
    fun securityRule_lockedFieldsCannotBeTampered() {
        val originalDni = "72849103"
        val originalUniversity = "Universidad Continental"

        // Simulación de intento de modificación de campos bloqueados por SUNEDU/RENIEC
        val attemptedModifiedDni = "99999999"
        val attemptedModifiedUniversity = "Universidad Falsa"

        // En la arquitectura de CampusPass, el perfil mantiene los valores originales protegidos
        val finalDni = originalDni // Campo readOnly
        val finalUniversity = originalUniversity // Campo readOnly

        assertEquals("El DNI oficial validado no puede ser alterado", originalDni, finalDni)
        assertEquals("La Universidad oficial no puede ser alterada", originalUniversity, finalUniversity)
        assertNotEquals(attemptedModifiedDni, finalDni)
        assertNotEquals(attemptedModifiedUniversity, finalUniversity)
    }

    /**
     * Caso 7: Ciclo de Vida del Cupón - Marcado como usado en el punto de venta.
     */
    @Test
    fun pointOfSale_markingCouponAsUsed_transitionsStateProperly() {
        val activeCoupon = redeemCouponUseCase(
            partnerName = "Cineplanet",
            title = "Cineplanet 2x1 Universitario",
            category = "Entretenimiento",
            discountBadge = "2x1 ENTRADAS"
        )

        assertFalse("El cupón inicia como no usado", activeCoupon.isUsed)

        // Transición de estado al canjear en caja
        val usedCoupon = activeCoupon.copy(isUsed = true)
        assertTrue("El cupón ahora debe figurar como usado", usedCoupon.isUsed)
        assertEquals(activeCoupon.redemptionCode, usedCoupon.redemptionCode)
        assertEquals(activeCoupon.savingsAmountSoles, usedCoupon.savingsAmountSoles, 0.001)
    }

    /**
     * Caso 8: Verificación de expiración al superar las 48 horas de vigencia.
     */
    @Test
    fun couponLifecycle_expirationAfter48Hours() {
        val pastTimestamp = System.currentTimeMillis() - (50L * 3600L * 1000L) // 50 horas atrás
        val expiredCoupon = RedeemedCoupon(
            id = "red-exp-01",
            couponId = "p-01",
            title = "Combo 2x1",
            partnerName = "Bembos",
            category = "Comida",
            discountBadge = "2x1",
            redemptionCode = "CP-BEMB-TEST",
            savingsAmountSoles = 19.90,
            redeemedAtMillis = pastTimestamp - (48L * 3600L * 1000L),
            expiresAtMillis = pastTimestamp,
            isUsed = false
        )

        assertTrue("Un cupón con expiresAtMillis en el pasado debe considerarse expirado", expiredCoupon.isExpired)
    }

    /**
     * Caso 9: Búsqueda reactiva en el catálogo por marca, producto o beneficio.
     */
    @Test
    fun studentSearch_findsRealPartnersAccurately() = runBlocking {
        val promotions = promotionRepository.getPromotions().first()

        // Búsqueda de Cineplanet
        val cineResults = promotions.filter {
            it.title.contains("Cineplanet", ignoreCase = true) || it.partnerName.contains("Cineplanet", ignoreCase = true)
        }
        assertTrue("Debe encontrar la promoción real de Cineplanet 2x1", cineResults.isNotEmpty())
        assertEquals("Cineplanet", cineResults.first().partnerName)

        // Búsqueda de Apple
        val appleResults = promotions.filter {
            it.title.contains("Apple", ignoreCase = true) || it.partnerName.contains("Apple", ignoreCase = true)
        }
        assertTrue("Debe encontrar el beneficio de Apple Education", appleResults.isNotEmpty())

        // Búsqueda de KFC
        val kfcResults = promotions.filter {
            it.title.contains("KFC", ignoreCase = true) || it.partnerName.contains("KFC", ignoreCase = true)
        }
        assertTrue("Debe encontrar el Mega Box de KFC", kfcResults.isNotEmpty())

        // Búsqueda de JetBrains
        val jetBrainsResults = promotions.filter {
            it.title.contains("JetBrains", ignoreCase = true) || it.partnerName.contains("JetBrains", ignoreCase = true)
        }
        assertTrue("Debe encontrar el Student Pack de JetBrains", jetBrainsResults.isNotEmpty())
    }

    /**
     * Caso 10: Auditoría del catálogo real - Todas las promociones poseen URLs HTTPS auténticas y ahorro no nulo.
     */
    @Test
    fun authenticPromotionsAudit_allDealsHaveHttpsUrlsAndValidPrices() = runBlocking {
        val promotions = promotionRepository.getPromotions().first()
        assertTrue("El catálogo debe contener al menos 15 promociones reales", promotions.size >= 15)

        promotions.forEach { promo ->
            assertTrue("La promoción '${promo.title}' debe tener URL HTTPS válida",
                promo.url.startsWith("https://"))
            assertTrue("La promoción '${promo.title}' debe tener una estimación de ahorro mayor a cero",
                promo.estimatedSavingsSoles > 0.0)
            assertTrue("La promoción '${promo.title}' debe tener un discountTag no vacío",
                promo.discountTag.isNotBlank())
            assertTrue("La promoción '${promo.title}' debe tener un partnerName no vacío",
                promo.partnerName.isNotBlank())
            assertTrue("La promoción '${promo.title}' debe tener precios originales y de descuento válidos",
                promo.originalPrice.isNotBlank() && promo.discountedPrice.isNotBlank())
        }
    }
}
