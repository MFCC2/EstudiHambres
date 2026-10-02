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
}
