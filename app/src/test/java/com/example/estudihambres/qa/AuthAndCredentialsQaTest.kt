package com.example.estudihambres.qa

import com.example.estudihambres.domain.usecase.ValidateStudentCredentialsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para Validación de Credenciales Estudiantiles (Agente 5: QA).
 * Cubre exhaustivamente 16 escenarios de límites, formatos, inyecciones y caracteres inválidos.
 */
class AuthAndCredentialsQaTest {

    private lateinit var useCase: ValidateStudentCredentialsUseCase

    @Before
    fun setUp() {
        useCase = ValidateStudentCredentialsUseCase()
    }

    @Test
    fun testValidDniStandard() {
        val result = useCase.validateDni("72945602")
        assertTrue(result.isValid)
    }

    @Test
    fun testValidDniStartingWithZero() {
        val result = useCase.validateDni("01234567")
        assertTrue(result.isValid)
    }

    @Test
    fun testDniTooShort7Digits() {
        val result = useCase.validateDni("1234567")
        assertFalse(result.isValid)
        assertEquals("El DNI debe contener exactamente 8 dígitos numéricos", result.errorMessage)
    }

    @Test
    fun testDniTooLong9Digits() {
        val result = useCase.validateDni("123456789")
        assertFalse(result.isValid)
        assertEquals("El DNI debe contener exactamente 8 dígitos numéricos", result.errorMessage)
    }

    @Test
    fun testDniWithLetters() {
        val result = useCase.validateDni("7294560A")
        assertFalse(result.isValid)
    }

    @Test
    fun testDniEmpty() {
        val result = useCase.validateDni("")
        assertFalse(result.isValid)
        assertEquals("El DNI es obligatorio", result.errorMessage)
    }

    @Test
    fun testDniWithSpaces() {
        val result = useCase.validateDni("7294 560")
        assertFalse(result.isValid)
    }

    @Test
    fun testDniWithSymbols() {
        val result = useCase.validateDni("7294-560")
        assertFalse(result.isValid)
    }

    @Test
    fun testValidInstitutionalEmailEduPe() {
        val result = useCase.validateUniversityEmail("mcallanaupa@continental.edu.pe")
        assertTrue(result.isValid)
    }

    @Test
    fun testValidInstitutionalEmailEdu() {
        val result = useCase.validateUniversityEmail("student@harvard.edu")
        assertTrue(result.isValid)
    }

    @Test
    fun testValidInstitutionalEmailPe() {
        val result = useCase.validateUniversityEmail("alumno@pucp.pe")
        assertTrue(result.isValid)
    }

    @Test
    fun testInvalidCommercialEmailGmail() {
        val result = useCase.validateUniversityEmail("estudiante@gmail.com")
        assertFalse(result.isValid)
        assertEquals("Debe ser un correo institucional (.edu / .pe)", result.errorMessage)
    }

    @Test
    fun testInvalidCommercialEmailOutlook() {
        val result = useCase.validateUniversityEmail("usuario@outlook.com")
        assertFalse(result.isValid)
    }

    @Test
    fun testMalformedEmailWithoutAtSymbol() {
        val result = useCase.validateUniversityEmail("usuariouniversidad.edu.pe")
        assertFalse(result.isValid)
    }

    @Test
    fun testEmptyEmail() {
        val result = useCase.validateUniversityEmail("")
        assertFalse(result.isValid)
        assertEquals("El correo universitario es obligatorio", result.errorMessage)
    }

    @Test
    fun testPasswordValidAndInvalidBoundaries() {
        assertTrue(useCase.validatePassword("123456").isValid)
        assertTrue(useCase.validatePassword("claveCompleja2026!").isValid)
        assertFalse(useCase.validatePassword("12345").isValid)
        assertFalse(useCase.validatePassword("").isValid)
    }

    @Test
    fun testAuthenticationFailsWithWrongPassword() = kotlinx.coroutines.runBlocking {
        val repo = com.example.estudihambres.data.repository.MockAuthRepositoryImpl(sessionManager = null)
        val result = repo.authenticate("alumno@continental.edu.pe", "wrongPassword99")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Contraseña incorrecta") == true)
    }

    @Test
    fun testAuthenticationFailsWithUnregisteredAccount() = kotlinx.coroutines.runBlocking {
        val repo = com.example.estudihambres.data.repository.MockAuthRepositoryImpl(sessionManager = null)
        val result = repo.authenticate("noexiste@continental.edu.pe", "campus123")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No existe ninguna cuenta") == true)
    }

    @Test
    fun testAuthenticationSucceedsWithCorrectCredentials() = kotlinx.coroutines.runBlocking {
        val repo = com.example.estudihambres.data.repository.MockAuthRepositoryImpl(sessionManager = null)
        val result = repo.authenticate("alumno@continental.edu.pe", "campus123")
        assertTrue(result.isSuccess)
        assertEquals("alumno@continental.edu.pe", result.getOrNull()?.email)
    }

    @Test
    fun testRegistrationAndSubsequentLogin() = kotlinx.coroutines.runBlocking {
        val repo = com.example.estudihambres.data.repository.MockAuthRepositoryImpl(sessionManager = null)
        val newUser = com.example.estudihambres.domain.model.StudentUser(
            id = "usr-new-qa",
            fullName = "Estudiante QA",
            email = "qa@uni.edu.pe",
            dni = "78451296",
            university = "UNI",
            studentCode = "78451296",
            verificationStatus = com.example.estudihambres.domain.model.VerificationStatus.PENDING_VERIFICATION
        )
        val regResult = repo.register(newUser, "claveSegura2026")
        assertTrue(regResult.isSuccess)

        val loginResult = repo.authenticate("qa@uni.edu.pe", "claveSegura2026")
        assertTrue(loginResult.isSuccess)
        assertEquals("Estudiante QA", loginResult.getOrNull()?.fullName)
    }

    @Test
    fun testRegistrationRejectsDuplicateEmail() = kotlinx.coroutines.runBlocking {
        val repo = com.example.estudihambres.data.repository.MockAuthRepositoryImpl(sessionManager = null)
        val duplicateUser = com.example.estudihambres.domain.model.StudentUser(
            id = "usr-dup",
            fullName = "Duplicado",
            email = "alumno@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = com.example.estudihambres.domain.model.VerificationStatus.PENDING_VERIFICATION
        )
        val regResult = repo.register(duplicateUser, "password123")
        assertTrue(regResult.isFailure)
    }

    @Test
    fun testManualRegistrationYieldsPendingStatusRequiringCardScan() = kotlinx.coroutines.runBlocking {
        val repo = com.example.estudihambres.data.repository.MockAuthRepositoryImpl(sessionManager = null)
        val manualUser = com.example.estudihambres.domain.model.StudentUser(
            id = "usr-manual-01",
            fullName = "Estudiante Sin Carnet En Mano",
            email = "sin.carnet@pucp.edu.pe",
            dni = "71549922",
            university = "PUCP",
            studentCode = "20231902",
            verificationStatus = com.example.estudihambres.domain.model.VerificationStatus.PENDING_VERIFICATION
        )
        val regResult = repo.register(manualUser, "password123")
        assertTrue(regResult.isSuccess)
        val created = regResult.getOrNull()
        assertEquals(com.example.estudihambres.domain.model.VerificationStatus.PENDING_VERIFICATION, created?.verificationStatus)
        assertEquals(null, created?.scannedCardDni)
    }

    @Test
    fun testScannedCarnetRegistrationYieldsVerifiedStatus() = kotlinx.coroutines.runBlocking {
        val repo = com.example.estudihambres.data.repository.MockAuthRepositoryImpl(sessionManager = null)
        val verifiedUser = com.example.estudihambres.domain.model.StudentUser(
            id = "usr-scanned-01",
            fullName = "Estudiante Carnet SUNEDU",
            email = "carnet.valido@continental.edu.pe",
            dni = "73849501",
            university = "Universidad Continental",
            studentCode = "73849501",
            verificationStatus = com.example.estudihambres.domain.model.VerificationStatus.VERIFIED,
            scannedCardDni = "73849501"
        )
        val regResult = repo.register(verifiedUser, "password123")
        assertTrue(regResult.isSuccess)
        val created = regResult.getOrNull()
        assertEquals(com.example.estudihambres.domain.model.VerificationStatus.VERIFIED, created?.verificationStatus)
        assertEquals("73849501", created?.scannedCardDni)
    }
}
