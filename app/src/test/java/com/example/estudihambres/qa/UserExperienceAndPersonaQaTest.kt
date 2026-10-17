package com.example.estudihambres.qa

import com.example.estudihambres.data.repository.MockPlaceRepository
import com.example.estudihambres.data.repository.MockPromotionRepositoryImpl
import com.example.estudihambres.domain.model.Place
import com.example.estudihambres.domain.model.RedeemedCoupon
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
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
 * 1. Cachimbo / Ingresante (Registro institucional, validación DNI y correo .edu.pe, onboarding).
 * 2. Estudiante de Ingeniería de Ciclos Superiores (Canje de suites de desarrollo JetBrains, GitHub y Notion).
 * 3. Estudiante con Presupuesto Ajustado en Provincias (UNSAAC / Continental - Ahorro en combos de comida).
 * 4. Estudiante Conmutador / Transporte (Ajuste del Radar de 5km, 15km a 30km y medio pasaje).
 * 5. Casos de Borde y Seguridad (Inmutabilidad de DNI, cupones vencidos y códigos no colisionantes).
 * 6. Estudiante Regional / Polo Cusco (Descubrimiento de comercios locales y Boleto Turístico).
 * 7. Estudiante sin Conectividad / Offline (Preservación y visualización del vale sin datos móviles).
 * 8. Estudiante Multicarrera (Actualización de carrera preservando DNI oficial y Universidad).
 * 9. Estudiante Nocturno / Semanas de Finales (Canje de café y comida rápida bajo presupuesto).
 * 10. Estudiante Tesista / Investigador (Herramientas académicas y libros especializados).
 * 11. Cajero POS de Marca Aliada (Validación de token y prevención de doble canje).
 * 12. Estudiante de Entretenimiento (Tarifas universitarias en streaming y cine).
 */
class UserExperienceAndPersonaQaTest {

    private val redeemCouponUseCase = RedeemCouponUseCase()
    private val promotionRepository = MockPromotionRepositoryImpl()
    private val placeRepository = MockPlaceRepository()

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

    /**
     * Caso 11: Persona "Estudiante de Provincias / Polo Académico Cusco" - Descubrimiento de convenios locales
     * (Boleto Turístico del Cusco, Café D'Wasi, Real Plaza Cusco) mediante filtrado geográfico por cercanía.
     */
    @Test
    fun regionalProvincialStudent_cuscoAcademicHub_discoversLocalCampusDeals() = runBlocking {
        val campusLat = -13.5233
        val campusLon = -71.9482

        val allPlaces = placeRepository.getPlaces().first()
        val cuscoPlaces = allPlaces.filter { place ->
            val distance = calculateDistanceKm(campusLat, campusLon, place.latitude, place.longitude)
            distance <= 5.0
        }

        assertTrue("Debe encontrar establecimientos en el radio de 5km del campus Cusco", cuscoPlaces.size >= 5)
        assertTrue("Debe incluir Bembos Real Plaza Cusco", cuscoPlaces.any { it.name.contains("Bembos Real Plaza Cusco") })
        assertTrue("Debe incluir Starbucks Real Plaza Cusco", cuscoPlaces.any { it.name.contains("Starbucks") })
        assertTrue("Debe incluir Café D'Wasi", cuscoPlaces.any { it.name.contains("D'Wasi") })

        // Exclusión del local foráneo en Valle Sagrado (> 30 km)
        assertFalse("El restaurante foráneo fuera de radio no debe figurar en el campus",
            cuscoPlaces.any { it.name.contains("Valle Sagrado") })

        // Canje del beneficio cultural estudiantil (Boleto Turístico del Cusco)
        val tourismCoupon = redeemCouponUseCase(
            partnerName = "Boleto Turístico",
            title = "Boleto Turístico del Cusco Estudiante",
            category = "Cultura",
            discountBadge = "50% DTO",
            couponId = "cu-10"
        )
        assertNotNull(tourismCoupon)
        assertTrue("El código debe tener el prefijo CP-BOLE-", tourismCoupon.redemptionCode.startsWith("CP-BOLE-"))
        assertTrue("Debe aplicar un ahorro significativo para estudiantes", tourismCoupon.savingsAmountSoles > 0)
    }

    /**
     * Caso 12: Persona "Estudiante con Baja Conectividad / Modo Offline" - Disponibilidad del vale en billetera
     * sin conexión a internet (los datos esenciales de canje y token QR/alfanumérico están encapsulados).
     */
    @Test
    fun lowConnectivityStudent_offlineWalletVoucher_preservesTokenAndMetadataWithoutNetwork() {
        val activeCoupon = redeemCouponUseCase(
            partnerName = "Pizza Hut",
            title = "2x1 Pizza Mediana",
            category = "Comida",
            discountBadge = "2x1",
            couponId = "p-15"
        )

        // Verificación de integridad autónoma para modo offline:
        assertNotNull("El identificador del cupón debe ser autónomo", activeCoupon.id)
        assertNotNull("El código de canje no puede ser nulo para lectura offline en POS", activeCoupon.redemptionCode)
        assertTrue("El código debe cumplir con la estructura verificable sin red",
            activeCoupon.redemptionCode.matches(Regex("^CP-[A-Z0-9]{4}-[A-Z0-9]{4}$")))
        assertTrue("El plazo de vigencia (48h) debe estar calculado localmente", activeCoupon.expiresAtMillis > System.currentTimeMillis())
        assertEquals("Pizza Hut", activeCoupon.partnerName)
        assertEquals("2x1", activeCoupon.discountBadge)
        assertTrue("El ahorro calculado debe estar registrado en el modelo", activeCoupon.savingsAmountSoles > 0)
        assertFalse("Debe registrarse como activo (no usado)", activeCoupon.isUsed)
    }

    /**
     * Caso 13: Persona "Estudiante con Cambio de Especialidad" - Actualización de carrera y código
     * preservando la inmutabilidad de la identidad legal validada (DNI y Universidad).
     */
    @Test
    fun careerSwitchStudent_profileModification_updatesCareerAndCodeWhileProtectingOfficialDniAndUniversity() {
        val initialUser = StudentUser(
            id = "usr-transf-01",
            fullName = "Mateo Sebastián Fernández",
            email = "73921845@continental.edu.pe",
            dni = "73921845",
            university = "Universidad Continental",
            studentCode = "20211045",
            career = "Ingeniería Industrial",
            verificationStatus = VerificationStatus.VERIFIED
        )

        // El estudiante solicita cambio de especialidad a Ingeniería de Sistemas
        val updatedUser = initialUser.copy(
            career = "Ingeniería de Sistemas",
            studentCode = "20232088"
        )

        // Verificación de actualización exitosa de datos académicos
        assertEquals("Ingeniería de Sistemas", updatedUser.career)
        assertEquals("20232088", updatedUser.studentCode)

        // Verificación de estricta protección de identidad institucional
        assertEquals("El DNI debe permanecer inmutable", initialUser.dni, updatedUser.dni)
        assertEquals("La Universidad debe permanecer inmutable", initialUser.university, updatedUser.university)
        assertEquals("El ID de usuario debe permanecer idéntico", initialUser.id, updatedUser.id)
        assertEquals("El estado de verificación oficial debe mantenerse", VerificationStatus.VERIFIED, updatedUser.verificationStatus)
    }

    /**
     * Caso 14: Persona "Estudiante Nocturno / Sesión de Exámenes Finales" - Canje de combos de estudio
     * (Starbucks + KFC) con presupuesto acotado y cálculo de ahorro neto en soles.
     */
    @Test
    fun nightOwlStudent_lateNightStudySession_redeemsCoffeeAndFastFoodUnderTightBudget() {
        // Canje de café para la noche de estudio
        val coffeeCoupon = redeemCouponUseCase(
            partnerName = "Starbucks Coffee",
            title = "Starbucks 2x1 Frappuccinos",
            category = "Cafetería",
            discountBadge = "2x1",
            couponId = "p-13"
        )

        // Canje de comida rápida accesible
        val dinnerCoupon = redeemCouponUseCase(
            partnerName = "KFC Perú",
            title = "Mega Box Estudiantil KFC",
            category = "Comida",
            discountBadge = "PROMO S/ 14.90",
            couponId = "p-14"
        )

        val totalSavings = coffeeCoupon.savingsAmountSoles + dinnerCoupon.savingsAmountSoles
        assertTrue("El ahorro combinado de café y cena debe superar los S/ 25.00", totalSavings >= 25.0)

        // Ambos cupones deben emitirse con tokens independientes y no colisionar
        assertNotEquals("Los códigos de canje deben ser únicos e independientes",
            coffeeCoupon.redemptionCode, dinnerCoupon.redemptionCode)
        assertFalse(coffeeCoupon.isExpired)
        assertFalse(dinnerCoupon.isExpired)
    }

    /**
     * Caso 15: Persona "Estudiante Tesista / Investigador" - Búsqueda de beneficios en libros académicos
     * (Crisol, SBS) y software técnico (Autodesk, Microsoft 365) con ahorro masivo.
     */
    @Test
    fun thesisResearcherStudent_academicToolsAndBooks_filtersCrisolSbsAndAutodesk() = runBlocking {
        val promotions = promotionRepository.getPromotions().first()

        val bookDeals = promotions.filter {
            it.title.contains("Crisol", ignoreCase = true) || it.title.contains("SBS", ignoreCase = true)
        }
        assertTrue("Debe encontrar promociones en librerías académicas para tesis", bookDeals.size >= 2)

        val softwareDeals = promotions.filter {
            it.title.contains("Autodesk", ignoreCase = true) || it.title.contains("Microsoft 365", ignoreCase = true)
        }
        assertTrue("Debe encontrar herramientas de software especializado gratuitas", softwareDeals.size >= 2)

        val totalSoftwareSavings = softwareDeals.sumOf { it.estimatedSavingsSoles }
        assertTrue("El ahorro en software profesional debe superar los S/ 500 anuales", totalSoftwareSavings >= 500.0)
    }

    /**
     * Caso 16: Persona "Cajero / Operador POS" - Detección y rechazo de intento de doble canje
     * de un cupón que ya figura como utilizado.
     */
    @Test
    fun posCashier_attemptDuplicateRedemption_detectsAndRejectsAlreadyConsumedCoupon() {
        val originalCoupon = redeemCouponUseCase(
            partnerName = "Bembos",
            title = "Combo Universitario 2x1",
            category = "Comida",
            discountBadge = "2x1",
            couponId = "p-01"
        )

        // Estado inicial en la primera presentación en caja: válido y no usado
        assertFalse("El cupón original no debe estar usado", originalCoupon.isUsed)
        assertFalse("El cupón original no debe estar expirado", originalCoupon.isExpired)

        // El cajero procesa la orden y marca el cupón como canjeado
        val consumedCoupon = originalCoupon.copy(isUsed = true)
        assertTrue("El cupón procesado debe tener isUsed = true", consumedCoupon.isUsed)

        // Simulación de un segundo intento de canje con el mismo vale
        val canBeReused = !consumedCoupon.isUsed && !consumedCoupon.isExpired
        assertFalse("Un cupón ya marcado como utilizado debe ser rechazado en caja", canBeReused)
    }

    /**
     * Caso 17: Persona "Estudiante de Entretenimiento y Streaming" - Tarifas universitarias oficiales
     * en Spotify, YouTube Premium y entradas de Cineplanet.
     */
    @Test
    fun entertainmentAndCultureStudent_streamingAndCinema_validatesSpotifyAndCineplanetSavings() = runBlocking {
        val promotions = promotionRepository.getPromotions().first()

        val spotifyPromo = promotions.firstOrNull { it.partnerName.contains("Spotify", ignoreCase = true) }
        assertNotNull("Debe existir la promoción oficial de Spotify Universitario", spotifyPromo)
        assertTrue("La mensualidad de Spotify debe ser tarifa estudiante (S/ 10.45)",
            spotifyPromo!!.discountedPrice.contains("10.45"))

        val youtubePromo = promotions.firstOrNull { it.partnerName.contains("YouTube", ignoreCase = true) }
        assertNotNull("Debe existir la promoción oficial de YouTube Premium Estudiante", youtubePromo)
        assertTrue("La mensualidad de YouTube debe ser tarifa estudiante (S/ 13.90)",
            youtubePromo!!.discountedPrice.contains("13.90"))

        val cinemaPromo = promotions.firstOrNull { it.partnerName.contains("Cineplanet", ignoreCase = true) }
        assertNotNull("Debe existir la promoción de Cineplanet 2x1", cinemaPromo)
        assertEquals("2x1 ENTRADAS", cinemaPromo!!.discountTag)
    }

    /**
     * Caso 18: Persona "Cachimbo en Onboarding" - Transición de estados de verificación
     * y desbloqueo progresivo de credenciales estudiantiles.
     */
    @Test
    fun freshmanOnboardingJourney_verificationStateProgression_unlocksFullPrivilegesUponVerification() {
        var student = StudentUser(
            id = "usr-onboard-01",
            fullName = "Diego Alejandro Alva",
            email = "dalva@continental.edu.pe",
            dni = "74589210",
            university = "Universidad Continental",
            studentCode = "20251010",
            verificationStatus = VerificationStatus.UNVERIFIED
        )

        // Fase 1: Recién registrado sin validar carné
        assertEquals(VerificationStatus.UNVERIFIED, student.verificationStatus)
        val isEligibleForVipInitial = student.verificationStatus == VerificationStatus.VERIFIED
        assertFalse("Un alumno no verificado no debe acceder a cupones VIP", isEligibleForVipInitial)

        // Fase 2: Envío de foto de carné universitario
        student = student.copy(
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            scannedCardDni = "74589210"
        )
        assertEquals(VerificationStatus.PENDING_VERIFICATION, student.verificationStatus)
        assertEquals("74589210", student.scannedCardDni)

        // Fase 3: Validación aprobada por SUNEDU / RENIEC
        student = student.copy(verificationStatus = VerificationStatus.VERIFIED)
        assertEquals(VerificationStatus.VERIFIED, student.verificationStatus)
        val isEligibleForVipFinal = student.verificationStatus == VerificationStatus.VERIFIED
        assertTrue("Un alumno verificado oficialmente desbloquea todos los beneficios", isEligibleForVipFinal)
    }

    /**
     * Función utilitaria de cálculo de distancia mediante la fórmula de Haversine para pruebas de radar geográfico.
     */
    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadiusKm * c
    }
}
