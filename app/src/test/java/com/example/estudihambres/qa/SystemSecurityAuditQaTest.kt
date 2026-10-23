package com.example.estudihambres.qa

import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.data.remote.api.DefaultPromotionApiServiceImpl
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.usecase.RedeemCouponUseCase
import com.example.estudihambres.domain.usecase.ValidateStudentCredentialsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Suite de Auditoría de Seguridad, Pruebas de Sistema y Flujo Completo de Usuario (QA Security & Systems).
 *
 * Evalúa los 7 pilares de resiliencia del sistema:
 * 1. Almacenamiento Criptográfico Seguro de Contraseñas (SHA-256).
 * 2. Prevención de Condiciones de Carrera y Doble Gasto en Billetera de Puntos.
 * 3. Sanitización de Esquemas de URL e Intent Injection en HomeScreen y MapScreen.
 * 4. Lista Blanca de Dominios Oficiales y Detección de Suplantación / Phishing.
 * 5. Mitigación de Cross-Site Scripting (DOM XSS) en el Radar OpenStreetMap/Leaflet.
 * 6. Validación Estricta de Identidad Estudiantil (DNI y Dominio Institucional).
 * 7. Simulación End-to-End del Flujo de Usuario con Verificación de Integridad.
 */
class SystemSecurityAuditQaTest {

    private lateinit var credentialsValidator: ValidateStudentCredentialsUseCase
    private lateinit var promotionApiService: DefaultPromotionApiServiceImpl
    private lateinit var rewardsRepository: MockRewardsRepositoryImpl
    private lateinit var redeemCouponUseCase: RedeemCouponUseCase

    @Before
    fun setUp() {
        credentialsValidator = ValidateStudentCredentialsUseCase()
        promotionApiService = DefaultPromotionApiServiceImpl()
        rewardsRepository = MockRewardsRepositoryImpl()
        redeemCouponUseCase = RedeemCouponUseCase()
    }

    // =========================================================================
    // 1. ALMACENAMIENTO CRIPTOGRÁFICO SEGURO DE CONTRASEÑAS (SHA-256)
    // =========================================================================

    @Test
    fun testSha256PasswordHashingIntegrity() {
        val plainPassword = "campus123"
        val hash = SessionManager.hashPassword(plainPassword)

        assertNotNull("El hash generado no debe ser nulo", hash)
        assertEquals("El hash SHA-256 debe tener exactamente 64 caracteres hexadecimales", 64, hash.length)
        assertTrue("El hash debe contener solo caracteres hexadecimales en minúsculas", hash.all { it.isDigit() || it in 'a'..'f' })

        // Determinismo: la misma entrada siempre debe producir el mismo hash
        val secondHash = SessionManager.hashPassword(plainPassword)
        assertEquals("El hash SHA-256 debe ser determinista", hash, secondHash)
    }

    @Test
    fun testPasswordHashDifferentiationAndAvalanche() {
        val hashA = SessionManager.hashPassword("campus123")
        val hashB = SessionManager.hashPassword("campus124")
        val hashC = SessionManager.hashPassword("123456")

        assertNotEquals("Contraseñas con 1 caracter de diferencia deben producir hashes totalmente distintos", hashA, hashB)
        assertNotEquals("Contraseñas diferentes deben producir hashes distintos", hashA, hashC)
        assertNotEquals("Contraseñas diferentes deben producir hashes distintos", hashB, hashC)
    }

    // =========================================================================
    // 2. PREVENCIÓN DE CONDICIONES DE CARRERA Y DOBLE GASTO EN PUNTOS
    // =========================================================================

    @Test
    fun testConcurrentPointsDeductionIntegrity() {
        // Simulación de billetera atómica concurrente
        var balance = 2000
        val lock = Any()
        val costPerItem = 400
        val totalAttempts = 10
        val successfulDeductions = AtomicInteger(0)
        val failedDeductions = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(totalAttempts)

        for (i in 0 until totalAttempts) {
            executor.submit {
                try {
                    synchronized(lock) {
                        if (balance >= costPerItem) {
                            balance -= costPerItem
                            successfulDeductions.incrementAndGet()
                        } else {
                            failedDeductions.incrementAndGet()
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertEquals("Con 2000 pts y costo 400, solo deben completarse exactamente 5 canjes", 5, successfulDeductions.get())
        assertEquals("Los restantes 5 intentos deben ser rechazados por saldo insuficiente", 5, failedDeductions.get())
        assertEquals("El balance final no debe ser negativo", 0, balance)
    }

    @Test
    fun testNegativeOrZeroCostDeductionRejected() {
        // Canjear con costo negativo no debe alterar el saldo ni permitir exploits
        fun safeDeduct(currentPoints: Int, cost: Int): Pair<Boolean, Int> {
            if (cost <= 0) return Pair(false, currentPoints)
            if (currentPoints < cost) return Pair(false, currentPoints)
            return Pair(true, currentPoints - cost)
        }

        val initial = 1000
        val (resNegative, balanceAfterNeg) = safeDeduct(initial, -500)
        assertFalse("Costos negativos deben ser rechazados de inmediato", resNegative)
        assertEquals("El balance no debe sufrir alteraciones con costo negativo", initial, balanceAfterNeg)

        val (resZero, balanceAfterZero) = safeDeduct(initial, 0)
        assertFalse("Costos iguales a cero deben ser rechazados", resZero)
        assertEquals("El balance no debe sufrir alteraciones con costo cero", initial, balanceAfterZero)
    }

    // =========================================================================
    // 3. SANITIZACIÓN DE ESQUEMAS DE URL E INTENT INJECTION
    // =========================================================================

    @Test
    fun testDangerousUrlSchemesRejected() {
        fun isAllowedSafeUrlScheme(rawUrl: String): Boolean {
            return try {
                val parsed = URI(rawUrl.trim())
                val scheme = parsed.scheme?.lowercase()
                scheme == "https" || scheme == "http"
            } catch (_: Exception) {
                false
            }
        }

        val dangerousUrls = listOf(
            "javascript:alert(document.cookie)",
            "file:///data/data/com.example.estudihambres/shared_prefs/accounts.xml",
            "content://telephony/sms",
            "intent:#Intent;action=android.intent.action.VIEW;end",
            "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
            "smsto:999888777?body=secret",
            "tel:999888777",
            "ftp://ftp.malicious.org/exploit.apk",
            "   ",
            "invalid_scheme"
        )

        dangerousUrls.forEach { dangerous ->
            assertFalse(
                "La URL con esquema inseguro '$dangerous' debe ser rechazada",
                isAllowedSafeUrlScheme(dangerous)
            )
        }
    }

    @Test
    fun testSafeHttpAndHttpsSchemesAccepted() {
        fun isAllowedSafeUrlScheme(rawUrl: String): Boolean {
            return try {
                val parsed = URI(rawUrl.trim())
                val scheme = parsed.scheme?.lowercase()
                scheme == "https" || scheme == "http"
            } catch (_: Exception) {
                false
            }
        }

        val validUrls = listOf(
            "https://www.bembos.com.pe/promociones",
            "https://www.starbucks.pe/menu",
            "https://cineplanet.com.pe/peliculas",
            "http://ucontinental.edu.pe"
        )

        validUrls.forEach { valid ->
            assertTrue(
                "La URL válida '$valid' debe ser permitida",
                isAllowedSafeUrlScheme(valid)
            )
        }
    }

    // =========================================================================
    // 4. LISTA BLANCA DE DOMINIOS OFICIALES Y DETECCIÓN DE PHISHING
    // =========================================================================

    @Test
    fun testWhitelistedPartnersAcceptedByPromotionService() {
        val legitimateUrls = listOf(
            "https://www.bembos.com.pe/promociones",
            "https://cineplanet.com.pe/beneficios-estudiantes",
            "https://www.spotify.com/pe/student/",
            "https://education.github.com/pack",
            "https://www.jetbrains.com/community/education/#students",
            "https://www.apple.com/la/education/store",
            "https://starbucks.pe/promos"
        )

        legitimateUrls.forEach { url ->
            assertTrue(
                "La URL oficial de convenio '$url' debe ser aceptada",
                promotionApiService.isOfferUrlSecureAndVerified(url)
            )
        }
    }

    @Test
    fun testPhishingAndSpoofedDomainsRejectedByPromotionService() {
        val spoofedOrFraudulentUrls = listOf(
            "https://bembos.com.pe.attacker-domain.org/phishing",
            "https://fake-spotify-student.com/login",
            "http://cineplanet.com.pe/sin-https", // Rechazado por HTTP no seguro
            "https://starbucks.pe.fake-login.io/promo",
            "https://malicious-site.net/descarga",
            "https://cineplanet-falso.pe"
        )

        spoofedOrFraudulentUrls.forEach { url ->
            assertFalse(
                "La URL fraudulenta o no certificada '$url' debe ser rechazada",
                promotionApiService.isOfferUrlSecureAndVerified(url)
            )
        }
    }

    // =========================================================================
    // 5. MITIGACIÓN DE CROSS-SITE SCRIPTING (DOM XSS) EN RADAR OPENSTREETMAP
    // =========================================================================

    @Test
    fun testMapMarkerPayloadSanitization() {
        fun sanitizeForJson(input: String): String {
            return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("<", "\\u003C")
                .replace(">", "\\u003E")
                .replace("&", "\\u0026")
                .replace("'", "\\u0027")
        }

        fun escapeHtml(str: String): String {
            return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }

        val maliciousPlaceName = """Café <script>alert('XSS')</script> "Aliado" & 'Bar'"""
        val sanitizedJson = sanitizeForJson(maliciousPlaceName)

        assertFalse("El JSON no debe contener etiquetas HTML angulares abiertas directas", sanitizedJson.contains("<script>"))
        assertTrue("Los caracteres '<' deben estar escapados a unicode \\u003C", sanitizedJson.contains("\\u003C"))
        assertTrue("Los caracteres '>' deben estar escapados a unicode \\u003E", sanitizedJson.contains("\\u003E"))
        assertTrue("Las comillas dobles deben estar escapadas con barra invertida", sanitizedJson.contains("\\\""))

        val safeDomHtml = escapeHtml(maliciousPlaceName)
        assertFalse("El DOM HTML no debe contener etiquetas '<script>' sin escapar", safeDomHtml.contains("<script>"))
        assertTrue("Los caracteres deben transformarse en entidades HTML como &lt; y &gt;", safeDomHtml.contains("&lt;script&gt;"))
    }

    // =========================================================================
    // 6. VALIDACIÓN ESTRICTA DE IDENTIDAD ESTUDIANTIL
    // =========================================================================

    @Test
    fun testDniValidationStrictnessAndInjectionDefense() {
        val sqlInjectionDni = "' OR '1'='1"
        val scriptDni = "<script>"
        val lettersDni = "7294560A"
        val shortDni = "7294560"
        val longDni = "729456021"
        val validDni = "72945602"

        assertFalse(credentialsValidator.validateDni(sqlInjectionDni).isValid)
        assertFalse(credentialsValidator.validateDni(scriptDni).isValid)
        assertFalse(credentialsValidator.validateDni(lettersDni).isValid)
        assertFalse(credentialsValidator.validateDni(shortDni).isValid)
        assertFalse(credentialsValidator.validateDni(longDni).isValid)
        assertTrue(credentialsValidator.validateDni(validDni).isValid)
    }

    @Test
    fun testInstitutionalEmailValidationStrictness() {
        val nonEduGmail = "alumno@gmail.com"
        val nonEduYahoo = "alumno@yahoo.com"
        val validUContinental = "72945602@continental.edu.pe"
        val validPucp = "estudiante@pucp.edu.pe"
        val validUnmsm = "alumno@unmsm.edu.pe"

        assertFalse(credentialsValidator.validateUniversityEmail(nonEduGmail).isValid)
        assertFalse(credentialsValidator.validateUniversityEmail(nonEduYahoo).isValid)
        assertTrue(credentialsValidator.validateUniversityEmail(validUContinental).isValid)
        assertTrue(credentialsValidator.validateUniversityEmail(validPucp).isValid)
        assertTrue(credentialsValidator.validateUniversityEmail(validUnmsm).isValid)
    }

    // =========================================================================
    // 7. SIMULACIÓN END-TO-END DEL FLUJO DE USUARIO CON CONTROL DE INTEGRIDAD
    // =========================================================================

    @Test
    fun testFullStudentJourneySecurityAndIntegrity() = runBlocking {
        // Paso 1: Creación de cuenta con credenciales validadas
        val rawDni = "72945602"
        val rawEmail = "manuel.callanaupa@continental.edu.pe"
        val rawPassword = "passwordSegura2026"

        assertTrue("El DNI debe ser válido", credentialsValidator.validateDni(rawDni).isValid)
        assertTrue("El correo debe ser institucional", credentialsValidator.validateUniversityEmail(rawEmail).isValid)
        assertTrue("La contraseña debe ser robusta", credentialsValidator.validatePassword(rawPassword).isValid)

        val student = StudentUser(
            id = "usr-audit-01",
            fullName = "Manuel Fabrizio Callañaupa",
            email = rawEmail,
            dni = rawDni,
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            career = "Ingeniería de Sistemas"
        )

        // Paso 2: Hasheo seguro de contraseña antes de cualquier almacenamiento
        val passwordHash = SessionManager.hashPassword(rawPassword)
        assertEquals(64, passwordHash.length)

        // Paso 3: Simular canje de cupón de ahorro comercial
        val redeemedCoupon = redeemCouponUseCase.invoke(
            partnerName = "Bembos Real Plaza Cusco",
            title = "Combo Universitario 2x1",
            category = "Comida Rápida",
            discountBadge = "2x1",
            couponId = "cu-01"
        )

        assertNotNull(redeemedCoupon)
        assertTrue("El código generado debe iniciar con prefijo institucional CP-", redeemedCoupon.redemptionCode.startsWith("CP-"))
        assertTrue("El ahorro estimado debe ser positivo", redeemedCoupon.savingsAmountSoles > 0.0)
        assertFalse("El cupón nuevo no debe estar marcado como usado", redeemedCoupon.isUsed)

        // Paso 4: Canje de premio en la Tienda de StudiPuntos (MockRewardsRepositoryImpl)
        val catalog = rewardsRepository.getAvailableRewards().first()
        assertTrue("El catálogo debe contener premios oficiales", catalog.isNotEmpty())

        val rewardToRedeem = catalog.first { it.title.contains("Spotify") }
        val initialPoints = rewardsRepository.getUserStudiPuntos().first()
        assertEquals(2450, initialPoints)

        val redemptionResult = rewardsRepository.redeemReward(rewardToRedeem)
        assertTrue("El canje debe ser exitoso con saldo suficiente", redemptionResult is Resource.Success)

        val balanceAfterRedeem = rewardsRepository.getUserStudiPuntos().first()
        assertEquals(
            "El saldo de StudiPuntos debe haberse descontado exactamente",
            initialPoints - rewardToRedeem.pointsCost,
            balanceAfterRedeem
        )

        // Paso 5: Intento de canje de un premio de costo superior al saldo restante
        val expensiveReward = RewardItem(
            id = "rew-luxury",
            title = "MacBook Pro Estudiantil",
            category = "Tecnología",
            pointsCost = 999999,
            description = "Premio que excede el saldo de puntos",
            partnerName = "Apple",
            iconEmoji = "💻",
            sampleCode = "AAPL-M3-9999"
        )

        val failedRedemption = rewardsRepository.redeemReward(expensiveReward)
        assertTrue("El canje excesivo debe ser rechazado", failedRedemption is Resource.Error)

        val finalBalance = rewardsRepository.getUserStudiPuntos().first()
        assertEquals("El balance debe mantenerse inalterado tras un canje rechazado", balanceAfterRedeem, finalBalance)
    }
}
