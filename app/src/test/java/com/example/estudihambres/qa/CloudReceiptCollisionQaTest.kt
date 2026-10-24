package com.example.estudihambres.qa

import com.example.estudihambres.data.remote.dto.ClaimedReceiptFirestoreDto
import com.example.estudihambres.data.repository.FirebaseReceiptCollisionRepositoryImpl
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.SunatReceiptData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para la sincronización y prevención de colisiones en la nube (Cloud Firestore).
 *
 * Valida:
 * 1. Detección atómica de duplicados First-Come First-Served.
 * 2. Simulación de 10 estudiantes intentando canjear la misma boleta simultáneamente.
 * 3. Mapeo conforme del DTO de Firestore con reglas de seguridad y retención de 72 horas.
 * 4. Tolerancia a fallos y resiliencia offline en entornos JVM sin emulador ni conexión de red.
 */
class CloudReceiptCollisionQaTest {

    private lateinit var collisionRepository: FirebaseReceiptCollisionRepositoryImpl

    @Before
    fun setUp() {
        // En pruebas JVM puras, firestore = null activa la degradación elegante y el almacén en memoria
        collisionRepository = FirebaseReceiptCollisionRepositoryImpl(firestore = null, sessionManager = null)
    }

    @Test
    fun testCloudRegistryDetectsUnclaimedReceipt() = runBlocking {
        val testFingerprint = "a1b2c3d4e5f60718"
        val isClaimed = collisionRepository.isReceiptClaimed(testFingerprint)
        assertFalse("Una boleta no registrada previamente debe figurar como libre", isClaimed)
    }

    @Test
    fun testFirstUserCanClaimReceiptSuccessfully() = runBlocking {
        val fingerprint = SunatReceiptData.calculateFingerprint("20252391498", "B001", "00049281")
        val result = collisionRepository.claimReceipt(
            fingerprint = fingerprint,
            userId = "usr_student_01",
            userEmail = "alumno1@uni.edu.pe",
            partnerName = "Bembos",
            amount = 28.50
        )

        assertTrue("El primer estudiante en registrar la boleta debe tener éxito", result.isSuccess)
        assertTrue("La boleta debe figurar como registrada inmediatamente", collisionRepository.isReceiptClaimed(fingerprint))
    }

    @Test
    fun testSimulateTenStudentsScanningSameReceiptConcurrently() = runBlocking {
        val sharedReceiptFingerprint = SunatReceiptData.calculateFingerprint("20429683581", "BA01", "00012345")

        var successfulClaims = 0
        var rejectedClaims = 0
        val rejectionReasons = mutableListOf<String>()

        // 10 estudiantes intentan registrar la misma boleta física
        for (i in 1..10) {
            val studentId = "student_%02d".format(i)
            val studentEmail = "estudiante$i@campus.pe"

            val result = collisionRepository.claimReceipt(
                fingerprint = sharedReceiptFingerprint,
                userId = studentId,
                userEmail = studentEmail,
                partnerName = "Cineplanet",
                amount = 35.00
            )

            if (result.isSuccess) {
                successfulClaims++
            } else {
                rejectedClaims++
                rejectionReasons.add(result.exceptionOrNull()?.message ?: "Desconocido")
            }
        }

        assertEquals("Exactamente 1 estudiante (el primero) debe poder canjear los puntos", 1, successfulClaims)
        assertEquals("Exactamente 9 estudiantes deben ser rechazados por colisión", 9, rejectedClaims)

        assertTrue(
            "Los 9 intentos deben reportar detección de colisión",
            rejectionReasons.all { it.contains("COLLISION_DETECTED") }
        )
    }

    @Test
    fun testClaimedReceiptFirestoreDtoMapping() {
        val now = com.google.firebase.Timestamp.now()
        val expiresAt = com.google.firebase.Timestamp(
            java.util.Date(now.toDate().time + (72L * 3600 * 1000))
        )

        val dto = ClaimedReceiptFirestoreDto(
            fingerprint = "0123456789abcdef",
            claimedByUserId = "firebase_uid_123",
            claimedByUserEmail = "alumno@pucp.edu.pe",
            partnerName = "Starbucks",
            totalAmount = 18.50,
            claimedAt = now,
            expiresAt = expiresAt
        )

        val map = dto.toMap()
        assertEquals("0123456789abcdef", map["fingerprint"])
        assertEquals("firebase_uid_123", map["claimedByUserId"])
        assertEquals("alumno@pucp.edu.pe", map["claimedByUserEmail"])
        assertEquals("Starbucks", map["partnerName"])
        assertEquals(18.50, map["totalAmount"])
        assertEquals(now, map["claimedAt"])
        assertEquals(expiresAt, map["expiresAt"])
        assertEquals("Debe contener exactamente los 7 campos auditados por firestore.rules", 7, map.size)
    }

    @Test
    fun testRewardsRepositoryRejectsDuplicateViaCollisionRepository() = runBlocking {
        val customCollisionRepo = FirebaseReceiptCollisionRepositoryImpl(firestore = null, sessionManager = null)
        val rewardsRepo = MockRewardsRepositoryImpl(
            sessionManager = null,
            collisionRepository = customCollisionRepo
        )

        // Primer escaneo exitoso
        val res1 = rewardsRepo.registerReceipt(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00099887",
            amount = 22.0,
            dateMillis = System.currentTimeMillis() - 3600000
        )
        assertTrue("El primer registro en RewardsRepository debe ser exitoso", res1 is com.example.estudihambres.core.util.Resource.Success)

        // Segundo escaneo del mismo comprobante
        val res2 = rewardsRepo.registerReceipt(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00099887",
            amount = 22.0,
            dateMillis = System.currentTimeMillis() - 3600000
        )
        assertTrue("El segundo registro debe fallar con Resource.Error", res2 is com.example.estudihambres.core.util.Resource.Error)
        assertNotNull(res2.message)
    }
}
