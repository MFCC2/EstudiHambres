package com.example.estudihambres.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [ValidateStudentCredentialsUseCase] (Agente 5: QA).
 * Valida formato de DNI de 8 dígitos y correo institucional universitario según PROJECT_RULES.md.
 */
class ValidateStudentCredentialsUseCaseTest {

    private lateinit var useCase: ValidateStudentCredentialsUseCase

    @Before
    fun setUp() {
        useCase = ValidateStudentCredentialsUseCase()
    }

    @Test
    fun `validateDni returns true only for exactly 8 numeric digits`() {
        assertTrue(useCase.validateDni("12345678").isValid)
        assertTrue(useCase.validateDni("70809010").isValid)

        assertFalse(useCase.validateDni("1234567").isValid)   // 7 dígitos
        assertFalse(useCase.validateDni("123456789").isValid) // 9 dígitos
        assertFalse(useCase.validateDni("1234567A").isValid) // No numérico
        assertFalse(useCase.validateDni("").isValid)          // Vacío
    }

    @Test
    fun `validateUniversityEmail returns true for valid university domains`() {
        assertTrue(useCase.validateUniversityEmail("alumno@unmsm.edu.pe").isValid)
        assertTrue(useCase.validateUniversityEmail("estudiante@pucp.pe").isValid)
        assertTrue(useCase.validateUniversityEmail("usuario@universidad.edu").isValid)

        assertFalse(useCase.validateUniversityEmail("personal@gmail.com").isValid)
        assertFalse(useCase.validateUniversityEmail("invalid-email").isValid)
        assertFalse(useCase.validateUniversityEmail("").isValid)
    }

    @Test
    fun `validatePassword requires at least 6 characters`() {
        assertTrue(useCase.validatePassword("123456").isValid)
        assertTrue(useCase.validatePassword("claveSegura123").isValid)

        assertFalse(useCase.validatePassword("12345").isValid)
        assertFalse(useCase.validatePassword("").isValid)
    }
}
