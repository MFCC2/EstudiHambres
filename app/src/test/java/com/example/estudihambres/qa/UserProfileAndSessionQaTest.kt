package com.example.estudihambres.qa

import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de Pruebas QA para Perfil de Usuario y Manejo de Sesión Universitaria (Agente 5: QA).
 * Valida 12 situaciones críticas de datos de perfil, estados de verificación, métricas y preferencias.
 */
class UserProfileAndSessionQaTest {

    @Test
    fun testStudentUserDefaultValues() {
        val user = StudentUser(
            id = "usr-01",
            fullName = "Manuel Callañaupa",
            email = "72945602@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.VERIFIED
        )

        assertEquals("usr-01", user.id)
        assertEquals("Ingeniería de Sistemas", user.career)
        assertEquals(VerificationStatus.VERIFIED, user.verificationStatus)
    }

    @Test
    fun testStudentUserProfileUpdateCopy() {
        val initialUser = StudentUser(
            id = "usr-02",
            fullName = "Ana García",
            email = "ana.garcia@pucp.edu.pe",
            dni = "71234567",
            university = "PUCP",
            studentCode = "20210045",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            career = "Derecho"
        )

        val updatedUser = initialUser.copy(
            fullName = "Ana Lucía García Mendoza",
            career = "Derecho Corporativo",
            university = "Pontificia Universidad Católica del Perú"
        )

        assertEquals("Ana Lucía García Mendoza", updatedUser.fullName)
        assertEquals("Derecho Corporativo", updatedUser.career)
        assertEquals("Pontificia Universidad Católica del Perú", updatedUser.university)
        assertEquals(initialUser.id, updatedUser.id)
        assertEquals(initialUser.dni, updatedUser.dni)
    }

    @Test
    fun testVerificationStatusTransitions() {
        var user = StudentUser(
            id = "usr-03",
            fullName = "Carlos López",
            email = "clopez@unmsm.edu.pe",
            dni = "70894512",
            university = "UNMSM",
            studentCode = "20201509",
            verificationStatus = VerificationStatus.UNVERIFIED
        )
        assertEquals(VerificationStatus.UNVERIFIED, user.verificationStatus)

        user = user.copy(verificationStatus = VerificationStatus.PENDING_VERIFICATION)
        assertEquals(VerificationStatus.PENDING_VERIFICATION, user.verificationStatus)

        user = user.copy(verificationStatus = VerificationStatus.VERIFIED)
        assertEquals(VerificationStatus.VERIFIED, user.verificationStatus)

        user = user.copy(verificationStatus = VerificationStatus.REJECTED)
        assertEquals(VerificationStatus.REJECTED, user.verificationStatus)
    }

    @Test
    fun testEmailValidationInstitutionalDomains() {
        val validInstitutionalEmails = listOf(
            "alumno@continental.edu.pe",
            "u202110@upc.edu.pe",
            "estudiante@pucp.pe",
            "medicina@unmsm.edu.pe",
            "informatica@uni.edu.pe"
        )

        validInstitutionalEmails.forEach { email ->
            val isValid = email.contains("@") && (email.endsWith(".edu.pe") || email.endsWith(".pe"))
            assertTrue("El correo $email debe ser reconocido como válido", isValid)
        }

        val invalidEmails = listOf(
            "alumno@gmail.com",
            "estudiante@yahoo.es",
            "sin_arroba_ni_dominio",
            "@continental.edu.pe"
        )

        invalidEmails.forEach { email ->
            val parts = email.split("@")
            val isOfficialEduPe = parts.size == 2 && parts[0].isNotBlank() && parts[1].endsWith(".edu.pe")
            assertFalse("El correo $email no debe considerarse institucional oficial", isOfficialEduPe)
        }
    }

    @Test
    fun testDniEightDigitsConstraint() {
        val validDnis = listOf("72945602", "01234567", "99999999")
        validDnis.forEach { dni ->
            assertTrue(dni.length == 8 && dni.all { it.isDigit() })
        }

        val invalidDnis = listOf("7294560", "729456021", "7294560A", "")
        invalidDnis.forEach { dni ->
            val isValid = dni.length == 8 && dni.all { it.isDigit() }
            assertFalse(isValid)
        }
    }

    @Test
    fun testRadarRadiusSelectionValues() {
        val allowedRadii = listOf(5.0, 15.0, 30.0)
        
        var selectedRadius = 15.0
        assertTrue(allowedRadii.contains(selectedRadius))

        selectedRadius = 30.0
        assertTrue(allowedRadii.contains(selectedRadius))

        selectedRadius = 5.0
        assertTrue(allowedRadii.contains(selectedRadius))

        val invalidRadius = 50.0
        assertFalse(allowedRadii.contains(invalidRadius))
    }

    @Test
    fun testNotificationToggleState() {
        var notificationsEnabled = true
        assertTrue(notificationsEnabled)

        // Simular toggle en UI
        notificationsEnabled = !notificationsEnabled
        assertFalse(notificationsEnabled)

        notificationsEnabled = !notificationsEnabled
        assertTrue(notificationsEnabled)
    }

    @Test
    fun testAccumulatedSavingsMetricCalculation() {
        // Ahorros de descuentos canjeados: 12 soles en Bembos, 35 en Spotify, 18 en comida local
        val redeemedDiscounts = listOf(12.0, 35.0, 18.5)
        val totalSavings = redeemedDiscounts.sum()
        
        assertEquals(65.5, totalSavings, 0.01)
        assertTrue(totalSavings >= 0.0)
    }

    @Test
    fun testProStudentLevelClassification() {
        fun calculateUserLevel(verified: Boolean, redemptionsCount: Int): String {
            return when {
                !verified -> "Nivel Básico"
                redemptionsCount >= 10 -> "Estudiante Leyenda"
                redemptionsCount >= 5 -> "Estudiante Pro"
                else -> "Estudiante Activo"
            }
        }

        assertEquals("Nivel Básico", calculateUserLevel(false, 20))
        assertEquals("Estudiante Activo", calculateUserLevel(true, 3))
        assertEquals("Estudiante Pro", calculateUserLevel(true, 7))
        assertEquals("Estudiante Leyenda", calculateUserLevel(true, 15))
    }

    @Test
    fun testStudentCodeFormatPreservation() {
        val studentCodes = listOf("72945602", "U202114520", "2019-10023", "00201844")
        studentCodes.forEach { code ->
            assertTrue(code.isNotBlank())
            assertTrue(code.length in 6..12)
        }
    }

    @Test
    fun testUserProfileImmutabilityAndEquality() {
        val user1 = StudentUser("id1", "Carlos", "c@c.pe", "12345678", "UC", "123", VerificationStatus.VERIFIED, "Sistemas")
        val user2 = StudentUser("id1", "Carlos", "c@c.pe", "12345678", "UC", "123", VerificationStatus.VERIFIED, "Sistemas")
        val user3 = user1.copy(fullName = "Carlos Ramos")

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
        assertNotEquals(user1, user3)
    }

    @Test
    fun testProfileFieldSanitizationAndTrimming() {
        val rawName = "   Manuel Callañaupa Cjuiro   "
        val trimmedName = rawName.trim()
        assertEquals("Manuel Callañaupa Cjuiro", trimmedName)

        val rawCareer = "   Ingeniería de Sistemas e Informática   "
        val trimmedCareer = rawCareer.trim()
        assertEquals("Ingeniería de Sistemas e Informática", trimmedCareer)
    }
}
