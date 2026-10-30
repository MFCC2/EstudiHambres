package com.example.estudihambres.qa

import com.example.estudihambres.core.legal.LegalConstants
import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.PointTransactionType
import com.example.estudihambres.domain.model.RedemptionMethod
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.usecase.ValidateSunatReceiptUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para la Tienda de StudiPuntos (Premios), Billetera de Lealtad Universitaria,
 * Validación SUNAT, Protocolo Anti-Doble Canje y Cumplimiento Regulatorio SBS / Ley N° 29733.
 */
class RewardsStoreQaTest {

    private lateinit var repository: MockRewardsRepositoryImpl
    private lateinit var sunatValidator: ValidateSunatReceiptUseCase

    @Before
    fun setUp() {
        repository = MockRewardsRepositoryImpl()
        sunatValidator = ValidateSunatReceiptUseCase()
    }

    @Test
    fun testRewardsCatalogContainsOfficialVerifiedDeals() = runBlocking {
        val rewards = repository.getAvailableRewards().first()
        assertTrue("El catálogo debe contener al menos 6 premios oficiales", rewards.size >= 6)

        assertTrue(rewards.any { it.title.contains("Cineplanet") && it.pointsCost == 1100 && it.redemptionMethod == RedemptionMethod.IN_STORE_QR })
        assertTrue(rewards.any { it.title.contains("Bembos") && it.pointsCost == 950 && it.redemptionMethod == RedemptionMethod.IN_STORE_QR })
        assertTrue(rewards.any { it.title.contains("Starbucks") && it.pointsCost == 850 && it.redemptionMethod == RedemptionMethod.IN_STORE_QR })
        assertTrue(rewards.any { it.title.contains("Spotify") && it.pointsCost == 1200 && it.redemptionMethod == RedemptionMethod.ONLINE_CODE })
        assertTrue(rewards.any { it.title.contains("Crisol") && it.pointsCost == 2500 })
        assertTrue(rewards.any { it.title.contains("Tambo") && it.pointsCost == 450 })
        assertTrue(rewards.any { it.title.contains("JetBrains") && it.pointsCost == 1800 })
    }

    @Test
    fun testRedeemRewardSucceedsWithSufficientPoints() = runBlocking {
        val reward = RewardItem(
            id = "test-rew-01",
            title = "Spotify Premium Estudiante",
            category = "Música",
            pointsCost = 1200,
            description = "1 mes de música sin interrupciones",
            partnerName = "Spotify",
            iconEmoji = "🎧",
            sampleCode = "SPOT-UNIV-4411",
            redemptionMethod = RedemptionMethod.ONLINE_CODE
        )

        val result = repository.redeemReward(reward)
        assertTrue("El canje debe ser exitoso con saldo suficiente", result is Resource.Success)
        val redeemed = (result as Resource.Success).data
        assertNotNull(redeemed)
        assertEquals("Spotify Premium Estudiante", redeemed?.rewardTitle)
        assertEquals(1200, redeemed?.pointsCost)
        assertTrue("El código generado debe contener el prefijo del aliado", redeemed?.redemptionCode?.startsWith("SPOT") == true)
        assertNotNull(redeemed?.redeemedAtFormatted)
        assertFalse("Un vale recién emitido debe tener estado no utilizado (false)", redeemed?.isUsed == true)
        assertNotNull("Debe poseer token de seguridad", redeemed?.securityToken)
    }

    @Test
    fun testRedeemRewardFailsWhenInsufficientPoints() = runBlocking {
        val expensiveReward = RewardItem(
            id = "test-rew-exp",
            title = "Viaje de Estudios Internacional",
            category = "Educación",
            pointsCost = 999999,
            description = "Premio exclusivo fuera del alcance actual",
            partnerName = "Global Education",
            iconEmoji = "✈️",
            sampleCode = "TRIP-MAX-9999"
        )

        val result = repository.redeemReward(expensiveReward)
        assertTrue("El canje debe fallar por saldo insuficiente", result is Resource.Error)
        assertTrue("El mensaje debe advertir sobre saldo insuficiente", result.message?.contains("insuficiente") == true)
    }

    @Test
    fun testEarnPointsFromReceiptAdds80Points() = runBlocking {
        val initialPoints = repository.getUserStudiPuntos().first()
        val result = repository.earnPointsFromReceipt()
        assertTrue(result is Resource.Success)
        val newPoints = (result as Resource.Success).data
        assertEquals(initialPoints + 80, newPoints)
    }

    @Test
    fun testMultiUserReceiptCollisionRejection() {
        // Simulación: El Estudiante 1 escanea la boleta de Bembos
        val claimedReceipts = mutableSetOf<String>()
        val now = System.currentTimeMillis()

        val ruc = "20252391498" // Bembos
        val serie = "B001"
        val correlativo = "00049281"
        val monto = 24.50

        // 1. Escaneo del Estudiante 1 -> Exitoso
        val res1 = sunatValidator(
            ruc = ruc,
            serie = serie,
            correlativo = correlativo,
            montoTotal = monto,
            fechaEmisionMillis = now,
            isFingerprintAlreadyClaimed = { claimedReceipts.contains(it) }
        )
        assertTrue("El primer escaneo debe ser válido", res1.isValid)
        assertNotNull(res1.receiptData)
        assertEquals(80, res1.pointsAwarded)

        // Registrar la huella en el registro global
        claimedReceipts.add(res1.receiptData!!.fingerprint)

        // 2. Escaneo del Estudiante 2 (amigo con la misma boleta) -> Rechazo inmediato
        val res2 = sunatValidator(
            ruc = ruc,
            serie = serie,
            correlativo = correlativo,
            montoTotal = monto,
            fechaEmisionMillis = now,
            isFingerprintAlreadyClaimed = { claimedReceipts.contains(it) }
        )
        assertFalse("El segundo intento con la misma boleta debe ser rechazado", res2.isValid)
        assertTrue(
            "El motivo debe advertir sobre comprobante duplicado",
            res2.rejectionReason?.contains("duplicado") == true || res2.rejectionReason?.contains("previamente") == true
        )
    }

    @Test
    fun testSunatReceiptValidationRules() {
        val now = System.currentTimeMillis()

        // RUC inválido (no 11 dígitos)
        val resInvalidRuc = sunatValidator("12345", "B001", "123", 15.0, now)
        assertFalse(resInvalidRuc.isValid)
        assertTrue(resInvalidRuc.rejectionReason?.contains("RUC") == true)

        // Serie inválida (no inicia con B)
        val resInvalidSerie = sunatValidator("20100107214", "F001", "123", 15.0, now)
        assertFalse(resInvalidSerie.isValid)
        assertTrue(resInvalidSerie.rejectionReason?.contains("Serie") == true)

        // Boleta de más de 72 horas (antigua)
        val fourDaysAgo = now - (96L * 3600 * 1000)
        val resOld = sunatValidator("20100107214", "B001", "123", 15.0, fourDaysAgo)
        assertFalse(resOld.isValid)
        assertTrue(resOld.rejectionReason?.contains("72 horas") == true)

        // Monto menor a S/ 5.00
        val resLowAmount = sunatValidator("20100107214", "B001", "123", 2.50, now)
        assertFalse(resLowAmount.isValid)
        assertTrue(resLowAmount.rejectionReason?.contains("mínimo") == true)
    }

    @Test
    fun testSunatQrFormatParsing() {
        val todayStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ROOT).format(java.util.Date())
        val qrRaw = "20100107214|03|B001|00049281|3.33|21.90|$todayStr|1|72945602|DIGEST_HASH_ABC123==|"
        val result = sunatValidator.parseSunatQr(qrRaw)
        assertTrue("El QR de SUNAT debe ser parseado exitosamente", result.isValid)
        assertEquals("20100107214", result.receiptData?.ruc)
        assertEquals("B001", result.receiptData?.serie)
        assertEquals("00049281", result.receiptData?.correlativo)
        assertEquals(21.90, result.receiptData?.montoTotal ?: 0.0, 0.01)
    }

    @Test
    fun testCryptographicLedgerHashChaining() {
        val welcomeTx = PointTransaction(
            id = "tx-01",
            title = "Bono de Bienvenida",
            description = "Bienvenido a EstudiHambres",
            pointsDelta = 200,
            type = PointTransactionType.WELCOME_BONUS,
            timestampMillis = 1000000L,
            previousHash = "GENESIS_BLOCK"
        )
        assertNotNull(welcomeTx.txHash)
        assertEquals(64, welcomeTx.txHash.length) // SHA-256 es 64 hex chars

        val ocrTx = PointTransaction(
            id = "tx-02",
            title = "Carnet SUNEDU",
            description = "Verificación de carnet",
            pointsDelta = 300,
            type = PointTransactionType.ID_VERIFICATION,
            timestampMillis = 2000000L,
            previousHash = welcomeTx.txHash
        )
        assertEquals(welcomeTx.txHash, ocrTx.previousHash)
        assertEquals(64, ocrTx.txHash.length)
        assertTrue("Hashes distintos para bloques distintos", welcomeTx.txHash != ocrTx.txHash)
    }

    @Test
    fun testMarkRewardAsUsedInStore() = runBlocking {
        val reward = RewardItem(
            id = "test-rew-used",
            title = "Cineplanet Entrada 2D Regular",
            category = "Cine & Ocio",
            pointsCost = 1100,
            description = "Boleto válido en taquilla",
            partnerName = "Cineplanet",
            iconEmoji = "🎬",
            sampleCode = "CINE-TEST-8812"
        )

        val redeemResult = repository.redeemReward(reward)
        assertTrue(redeemResult is Resource.Success)
        val redeemed = (redeemResult as Resource.Success).data!!

        val markSuccess = repository.markRewardAsUsed(redeemed.id)
        assertTrue("Marcar como usado debe retornar true", markSuccess)

        val updatedList = repository.getRedeemedRewards().first()
        val found = updatedList.find { it.id == redeemed.id }
        assertNotNull(found)
        assertTrue("El estado debe ser utilizado (isUsed = true)", found?.isUsed == true)
    }

    @Test
    fun testLegalAndRegulatoryComplianceStatements() {
        // 1. SBS (Ley N° 29985): Verificar que el disclaimer prohíbe el dinero en efectivo y transferencias
        val disclaimer = LegalConstants.SBS_LEGAL_DISCLAIMER
        assertTrue("Debe declarar no convertibilidad a efectivo", disclaimer.contains("dinero en efectivo"))
        assertTrue("Debe mencionar no transferibilidad", disclaimer.contains("no son transferibles"))
        assertTrue("Debe referenciar a la SBS", disclaimer.contains("SBS"))

        // 2. Protección de Datos (Ley N° 29733): Verificar declaración de procesamiento on-device sin almacenamiento de fotos
        val privacy = LegalConstants.DATA_PROTECTION_POLICY
        assertTrue("Debe referenciar Ley N° 29733", privacy.contains("Ley N° 29733"))
        assertTrue("Debe declarar procesamiento on-device local", privacy.contains("local"))
        assertTrue("Debe garantizar que no almacena fotos permanentes", privacy.contains("NO almacena imágenes"))
    }

    @Test
    fun testConversionRateCalculation() {
        val points = 2450
        val solesValue = points / 100.0
        assertEquals(24.50, solesValue, 0.01)

        val points500 = 500
        val soles500 = points500 / 100.0
        assertEquals(5.00, soles500, 0.01)
    }
}
