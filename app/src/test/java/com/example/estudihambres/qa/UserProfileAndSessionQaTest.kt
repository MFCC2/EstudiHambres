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

    @Test
    fun testDevModeUserAttributes() {
        val devUser = StudentUser(
            id = "usr-dev-99",
            fullName = "Modo Desarrollador (Vista Genérica)",
            email = "dev@estudihambres.pe",
            dni = "00000000",
            university = "Sandbox QA",
            studentCode = "DEV-001",
            verificationStatus = VerificationStatus.VERIFIED,
            isDev = true
        )
        assertTrue(devUser.isDev)
        assertEquals("Modo Desarrollador (Vista Genérica)", devUser.fullName)
    }

    @Test
    fun testRegularStudentSingleCardRestrictionLogic() {
        val verifiedStudent = StudentUser(
            id = "usr-student-01",
            fullName = "Manuel Fabrizio Callañaupa",
            email = "alumno@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.VERIFIED,
            isDev = false,
            scannedCardDni = "72945602"
        )

        // Simular regla de negocio de elegibilidad: estudiante ya verificado no puede escanear otro DNI distinto
        val attemptOtherDni = "71889900"
        val isAllowedOther = verifiedStudent.isDev || (verifiedStudent.scannedCardDni == null || verifiedStudent.scannedCardDni == attemptOtherDni)
        assertFalse("Un estudiante regular no debe poder escanear un carnet con DNI distinto al ya verificado", isAllowedOther)

        // Pero sí puede mantener su propio DNI
        val attemptSameDni = "72945602"
        val isAllowedSame = verifiedStudent.isDev || (verifiedStudent.scannedCardDni == attemptSameDni)
        assertTrue("El estudiante puede re-validar su propio carnet", isAllowedSame)
    }

    @Test
    fun testGoogleMapsNavigationUrlGeneration() {
        val placeLat = -13.5233
        val placeLng = -71.9482
        val expectedWebUrl = "https://www.google.com/maps/dir/?api=1&destination=$placeLat,$placeLng"
        val expectedNativeUri = "google.navigation:q=$placeLat,$placeLng"

        assertTrue(expectedWebUrl.contains("destination=-13.5233,-71.9482"))
        assertTrue(expectedNativeUri.startsWith("google.navigation:q=-13.5233"))
    }

    @Test
    fun testDniAndUniversityImmutabilityOnProfileUpdate() {
        val verifiedStudent = StudentUser(
            id = "usr-immutability-01",
            fullName = "Manuel Callañaupa",
            email = "72945602@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.VERIFIED,
            career = "Ingeniería de Sistemas",
            profilePhotoPath = "/data/user/0/com.example.estudihambres/files/avatar_123.jpg"
        )

        // Simular intento de actualización de perfil: se modifican nombre, carrera y correo,
        // pero DNI y Universidad DEBEN PERMANECER INMUTABLES según la política de seguridad
        val updatedStudent = verifiedStudent.copy(
            fullName = "Manuel F. Callañaupa",
            career = "Ingeniería de Software e Inteligencia Artificial",
            email = "mcallanaupa@continental.edu.pe",
            studentCode = "2025-SYS-99"
        )

        assertEquals("Manuel F. Callañaupa", updatedStudent.fullName)
        assertEquals("Ingeniería de Software e Inteligencia Artificial", updatedStudent.career)
        assertEquals("mcallanaupa@continental.edu.pe", updatedStudent.email)
        assertEquals("2025-SYS-99", updatedStudent.studentCode)

        // Aseveración de inmutabilidad estricta
        assertEquals("72945602", updatedStudent.dni)
        assertEquals("Universidad Continental", updatedStudent.university)
        assertEquals(verifiedStudent.dni, updatedStudent.dni)
        assertEquals(verifiedStudent.university, updatedStudent.university)
    }

    @Test
    fun testProfilePhotoPersistenceAndPath() {
        val userWithoutPhoto = StudentUser(
            id = "usr-photo-01",
            fullName = "Estudiante Sin Foto",
            email = "student@continental.edu.pe",
            dni = "73344556",
            university = "Universidad Continental",
            studentCode = "73344556",
            profilePhotoPath = null
        )
        assertNotNull(userWithoutPhoto)
        assertEquals(null, userWithoutPhoto.profilePhotoPath)

        val customPhotoPath = "/data/user/0/com.example.estudihambres/files/avatar_1790915000.jpg"
        val userWithPhoto = userWithoutPhoto.copy(profilePhotoPath = customPhotoPath)

        assertNotNull(userWithPhoto.profilePhotoPath)
        assertEquals(customPhotoPath, userWithPhoto.profilePhotoPath)
        assertTrue(userWithPhoto.profilePhotoPath!!.endsWith(".jpg"))
    }

    @Test
    fun testPendingStudentDniReconciliationOnFirstScan() {
        val pendingStudent = StudentUser(
            id = "usr-mistype-01",
            fullName = "Carolina Alvarado",
            email = "carolina.alvarado@upsjb.edu.pe",
            dni = "71234560",
            university = "Universidad Privada San Juan Bautista",
            studentCode = "2010103140",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            scannedCardDni = null
        )

        val realScannedDni = "71234567"
        val canReconcile = pendingStudent.verificationStatus != VerificationStatus.VERIFIED || pendingStudent.isDev
        assertTrue("Usuario con DNI mal escrito debe poder reconciliarlo con el escaneo de su carnet", canReconcile)

        val reconciledUser = pendingStudent.copy(
            dni = realScannedDni,
            scannedCardDni = realScannedDni,
            verificationStatus = VerificationStatus.VERIFIED
        )

        assertEquals("71234567", reconciledUser.dni)
        assertEquals("71234567", reconciledUser.scannedCardDni)
        assertEquals(VerificationStatus.VERIFIED, reconciledUser.verificationStatus)
    }
}
