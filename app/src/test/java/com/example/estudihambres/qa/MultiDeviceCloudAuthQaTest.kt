package com.example.estudihambres.qa

import com.example.estudihambres.data.repository.FirebaseAuthRepositoryImpl
import com.example.estudihambres.data.repository.FirebaseReceiptCollisionRepositoryImpl
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.PointTransactionType
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RedemptionMethod
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.SunatReceiptData
import com.example.estudihambres.domain.model.VerificationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Suite de Pruebas de Calidad (QA) para Autenticación en la Nube y Sincronización Multi-Dispositivo.
 *
 * Simula y valida el flujo completo de:
 * 1. Estudiante A iniciando sesión en el teléfono de su amigo (Estudiante B) con sus propias credenciales.
 * 2. Cierre de sesión seguro y purgado de datos efímeros sin dejar rastro ni fugar información personal.
 * 3. Hidratación de transacciones, saldo de StudiPuntos y vales redimidos desde Cloud Firestore.
 * 4. Integridad contable de los esquemas auditados por [firestore.rules].
 * 5. Prevención de colisión multi-usuario de comprobantes de pago SUNAT.
 */
class MultiDeviceCloudAuthQaTest {

    private lateinit var authRepository: FirebaseAuthRepositoryImpl
    private lateinit var rewardsRepository: MockRewardsRepositoryImpl
    private lateinit var collisionRepository: FirebaseReceiptCollisionRepositoryImpl

    @Before
    fun setUp() {
        authRepository = FirebaseAuthRepositoryImpl(
            auth = null,
            firestore = null,
            sessionManager = null
        )
        collisionRepository = FirebaseReceiptCollisionRepositoryImpl(
            firestore = null,
            sessionManager = null
        )
        rewardsRepository = MockRewardsRepositoryImpl(
            sessionManager = null,
            collisionRepository = collisionRepository,
            auth = null,
            firestore = null
        )
    }

    @Test
    fun testScenario01_FriendPhoneInitialUserLogoutPushesSessionToNull() = runBlocking {
        // En el teléfono del amigo (Phone 2), el amigo (User B) tiene su cuenta
        val friendUser = StudentUser(
            id = "usr-friend-02",
            fullName = "Rodrigo Amigo Alumno",
            email = "rodrigo.amigo@continental.edu.pe",
            dni = "73849501",
            university = "Universidad Continental",
            studentCode = "73849501",
            verificationStatus = VerificationStatus.VERIFIED,
            studiPuntos = 1200
        )
        val regResult = authRepository.register(friendUser, "claveAmigo123")
        assertTrue(regResult.isSuccess)

        // El amigo cierra su sesión para prestarle el celular a User A
        val logoutResult = authRepository.clearSession()
        assertTrue(logoutResult.isSuccess)

        // Verificar que no queda ningún usuario activo en memoria del teléfono
        val currentUser = authRepository.getCurrentUser().first()
        assertNull("Tras cerrar sesión en el teléfono del amigo, no debe existir ningún usuario activo", currentUser)
    }

    @Test
    fun testScenario02_UserALogsIntoFriendPhoneAndReceivesOwnProfileAndPoints() = runBlocking {
        // 1. Amigo previamente registrado
        val userB = StudentUser(
            id = "usr-b-friend",
            fullName = "Estudiante B Amigo",
            email = "estudiante.b@continental.edu.pe",
            dni = "74112233",
            university = "Universidad Continental",
            studentCode = "74112233",
            studiPuntos = 950
        )
        authRepository.register(userB, "passwordB123")
        authRepository.clearSession()

        // 2. Estudiante A se registra e inicia sesión en el teléfono del amigo
        val userA = StudentUser(
            id = "usr-a-owner",
            fullName = "Manuel Fabrizio Callañaupa",
            email = "alumno.a@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            career = "Ingeniería de Sistemas",
            studiPuntos = 400
        )
        val regAResult = authRepository.register(userA, "miClaveSeguraA456")
        assertTrue(regAResult.isSuccess)

        // 3. Estudiante A inicia sesión con sus credenciales en este teléfono
        val loginResult = authRepository.authenticate("alumno.a@continental.edu.pe", "miClaveSeguraA456")
        assertTrue("El estudiante A debe poder ingresar con sus credenciales en el teléfono de su amigo", loginResult.isSuccess)

        val activeUser = authRepository.getCurrentUser().first()
        assertNotNull(activeUser)
        assertEquals("alumno.a@continental.edu.pe", activeUser?.email)
        assertEquals("72945602", activeUser?.dni)
        assertEquals("Manuel Fabrizio Callañaupa", activeUser?.fullName)
        assertEquals(400, activeUser?.studiPuntos)
        assertFalse("El usuario A no debe tener datos del usuario B", activeUser?.email == userB.email)
    }

    @Test
    fun testScenario03_UserALogsOutAndFriendLogsBackInWithoutCrossContamination() = runBlocking {
        val userA = StudentUser(
            id = "usr-a-01",
            fullName = "Usuario A",
            email = "usua@pucp.edu.pe",
            dni = "71000001",
            university = "PUCP",
            studentCode = "20220001",
            studiPuntos = 500
        )
        val userB = StudentUser(
            id = "usr-b-02",
            fullName = "Usuario B Amigo",
            email = "usub@pucp.edu.pe",
            dni = "71000002",
            university = "PUCP",
            studentCode = "20220002",
            studiPuntos = 1500
        )

        authRepository.register(userA, "passA123")
        authRepository.register(userB, "passB123")

        // User A inicia sesión
        authRepository.authenticate("usua@pucp.edu.pe", "passA123")
        assertEquals("usua@pucp.edu.pe", authRepository.getCurrentUser().first()?.email)

        // User A sale
        authRepository.clearSession()
        assertNull(authRepository.getCurrentUser().first())

        // Amigo User B vuelve a entrar a su teléfono
        val loginBResult = authRepository.authenticate("usub@pucp.edu.pe", "passB123")
        assertTrue(loginBResult.isSuccess)

        val activeB = authRepository.getCurrentUser().first()
        assertEquals("usub@pucp.edu.pe", activeB?.email)
        assertEquals("Usuario B Amigo", activeB?.fullName)
        assertEquals(1500, activeB?.studiPuntos)
    }

    @Test
    fun testScenario04_CloudSubcollectionPointTransactionsMappingAndIntegrity() {
        val rawDoc = mapOf(
            "id" to "tx-test-123",
            "title" to "Canje: Combo Bembos",
            "description" to "Vale redimido para Bembos Real Plaza.",
            "delta" to -450,
            "pointsDelta" to -450,
            "type" to "REWARD_PURCHASE",
            "referenceId" to "reward-uuid-99",
            "previousHash" to "HASH_BLOCK_PREV_12345678901234567890123456789012345678901234567890123456",
            "txHash" to "a1b2c3d4e5f60718293a4b5c6d7e8f90123456789abcdef0123456789abcdef0",
            "timestampMillis" to 1730000000000.0
        )

        val tx = authRepository.documentToPointTransaction(rawDoc, "tx-test-123")
        assertNotNull(tx)
        assertEquals("tx-test-123", tx?.id)
        assertEquals("Canje: Combo Bembos", tx?.title)
        assertEquals(-450, tx?.pointsDelta)
        assertEquals(PointTransactionType.REWARD_PURCHASE, tx?.type)
        assertEquals("reward-uuid-99", tx?.referenceId)
        assertEquals(64, tx?.txHash?.length)
    }

    @Test
    fun testScenario05_CloudSubcollectionRedeemedRewardsMappingAndIntegrity() {
        val rawReward = mapOf(
            "id" to "rw-voucher-777",
            "rewardTitle" to "Café Americano 12oz",
            "pointsCost" to 300,
            "redemptionCode" to "STAR-9921",
            "redeemedAtFormatted" to "06 Nov 2026, 03:00 PM",
            "partnerName" to "Starbucks",
            "iconEmoji" to "☕",
            "isUsed" to false,
            "expiresAtMillis" to 1730500000000.0,
            "securityToken" to "882194",
            "officialUrl" to "https://starbucks.pe",
            "redemptionMethod" to "ONLINE_CODE"
        )

        val reward = authRepository.documentToRedeemedReward(rawReward, "rw-voucher-777")
        assertNotNull(reward)
        assertEquals("rw-voucher-777", reward?.id)
        assertEquals("Café Americano 12oz", reward?.rewardTitle)
        assertEquals(300, reward?.pointsCost)
        assertEquals("STAR-9921", reward?.redemptionCode)
        assertEquals("Starbucks", reward?.partnerName)
        assertEquals(false, reward?.isUsed)
        assertEquals(RedemptionMethod.ONLINE_CODE, reward?.redemptionMethod)
    }

    @Test
    fun testScenario06_CorruptOrMissingDataInFirestoreReturnsNullGracefully() {
        // Documento sin título ni tipo obligatorio
        val corruptDoc = mapOf<String, Any>(
            "foo" to "bar",
            "delta" to 50
        )
        val resultTx = authRepository.documentToPointTransaction(corruptDoc, "tx-corrupt")
        assertNull("Un documento sin los campos mínimos obligatorios debe ser descartado sin arrojar excepción", resultTx)

        val corruptReward = mapOf<String, Any>(
            "randomField" to 123
        )
        val resultRw = authRepository.documentToRedeemedReward(corruptReward, "rw-corrupt")
        assertNull("Un vale sin título debe retornar null de forma segura", resultRw)
    }

    @Test
    fun testScenario07_WrongPasswordFailsAuthenticationGracefully() = runBlocking {
        val user = StudentUser(
            id = "usr-sec-01",
            fullName = "Estudiante Seguridad",
            email = "seguridad@uni.edu.pe",
            dni = "79801234",
            university = "UNI",
            studentCode = "20260012"
        )
        authRepository.register(user, "claveCorrecta123")

        val failResult = authRepository.authenticate("seguridad@uni.edu.pe", "claveErrada999")
        assertTrue(failResult.isFailure)
        assertTrue(failResult.exceptionOrNull()?.message?.contains("Contraseña incorrecta") == true)
    }

    @Test
    fun testScenario08_OneWayRedemptionLatchIntegrity() = runBlocking {
        val rewardItem = RewardItem(
            id = "rew-01",
            title = "Promo Cineplanet",
            category = "Entretenimiento",
            pointsCost = 350,
            description = "Entrada regular 2D para salas Cineplanet a nivel nacional.",
            partnerName = "Cineplanet",
            iconEmoji = "🍿",
            sampleCode = "CINE-001"
        )
        val redeemResult = rewardsRepository.redeemReward(rewardItem)
        assertTrue(redeemResult is com.example.estudihambres.core.util.Resource.Success)

        val voucher = (redeemResult as com.example.estudihambres.core.util.Resource.Success).data
        assertNotNull(voucher)
        assertFalse(voucher!!.isUsed)

        // Marcar como usado en taquilla
        val markResult = rewardsRepository.markRewardAsUsed(voucher.id)
        assertTrue(markResult)

        val redeemedList = rewardsRepository.getRedeemedRewards().first()
        val updatedVoucher = redeemedList.find { it.id == voucher.id }
        assertNotNull(updatedVoucher)
        assertTrue("El vale debe quedar marcado como utilizado (isUsed = true)", updatedVoucher!!.isUsed)
    }

    @Test
    fun testScenario09_ReceiptScanningMultiUserDeduplicationInCloud() = runBlocking {
        val ruc = "20252391498"
        val serie = "B001"
        val correlativo = "00098765"
        val fp = SunatReceiptData.calculateFingerprint(ruc, serie, correlativo)

        // Estudiante A canjea la boleta
        val claimResultA = collisionRepository.claimReceipt(
            fingerprint = fp,
            userId = "usr-student-a",
            userEmail = "alumnoa@uni.edu.pe",
            partnerName = "Bembos",
            amount = 35.0
        )
        assertTrue("El primer reclamo debe tener éxito", claimResultA.isSuccess)

        // Estudiante B (amigo) intenta escanear la misma boleta desde su teléfono
        val claimResultB = collisionRepository.claimReceipt(
            fingerprint = fp,
            userId = "usr-student-b",
            userEmail = "alumnob@uni.edu.pe",
            partnerName = "Bembos",
            amount = 35.0
        )
        assertTrue("El segundo intento de reclamo de la misma boleta debe fallar por colisión", claimResultB.isFailure)
        assertTrue(
            "El mensaje de error debe indicar colisión detectada",
            claimResultB.exceptionOrNull()?.message?.contains("COLLISION_DETECTED") == true
        )
    }

    @Test
    fun testScenario10_ProfileUpdatePreservesImmutableSuneduFields() = runBlocking {
        val user = StudentUser(
            id = "usr-imm-01",
            fullName = "Pedro Castillo Pérez",
            email = "pedro.castillo@unmsm.edu.pe",
            dni = "78654321",
            university = "UNMSM",
            studentCode = "20241001",
            career = "Economía"
        )
        authRepository.register(user, "unmsm123")
        authRepository.authenticate("pedro.castillo@unmsm.edu.pe", "unmsm123")

        val updateResult = authRepository.updateProfile(
            fullName = "Pedro Juan Castillo Pérez",
            career = "Economía Internacional",
            studentCode = "20241001-A"
        )
        assertTrue(updateResult.isSuccess)

        val updated = authRepository.getCurrentUser().first()
        assertEquals("Pedro Juan Castillo Pérez", updated?.fullName)
        assertEquals("Economía Internacional", updated?.career)
        assertEquals("20241001-A", updated?.studentCode)
        // DNI y Universidad se preservan inmutables
        assertEquals("78654321", updated?.dni)
        assertEquals("UNMSM", updated?.university)
    }
}
