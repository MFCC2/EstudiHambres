package com.example.estudihambres.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [ParseStudentCardOcrUseCase] (Agente 5: QA).
 * Valida la búsqueda de palabras clave SUNEDU, UNIVERSIDAD y el año de vigencia.
 */
class ParseStudentCardOcrUseCaseTest {

    private lateinit var parser: ParseStudentCardOcrUseCase

    @Before
    fun setUp() {
        parser = ParseStudentCardOcrUseCase()
    }

    @Test
    fun `valid student card with SUNEDU, UNIVERSIDAD and year should be verified`() {
        val sampleOcrText = """
            REPÚBLICA DEL PERÚ
            SUNEDU
            CARNET UNIVERSITARIO
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            FACULTAD DE INGENIERÍA DE SISTEMAS
            CÓDIGO: 20231234
            VIGENCIA HASTA: 2026
        """.trimIndent()

        val result = parser(sampleOcrText)

        assertTrue(result.isVerified)
        assertTrue(result.hasSuneduKeyword)
        assertTrue(result.hasUniversityKeyword)
        assertEquals("2026", result.validityYearDetected)
        assertNotNull(result.universityName)
    }

    @Test
    fun `card with only university and year should be verified`() {
        val sampleOcrText = """
            UNIVERSIDAD DE INGENIERÍA
            CARNET ESTUDIANTIL
            VÁLIDO 2025
        """.trimIndent()

        val result = parser(sampleOcrText)

        assertTrue(result.isVerified)
        assertTrue(result.hasUniversityKeyword)
        assertEquals("2025", result.validityYearDetected)
    }

    @Test
    fun `random text without university keywords should not be verified`() {
        val supermarketReceipt = """
            SUPERMERCADO METRO
            BOLETA DE VENTA ELECTRÓNICA
            LECHE GLORIA 1L S/ 4.50
            PAN DE MOLDE S/ 6.00
            TOTAL: S/ 10.50
            GRACIAS POR SU COMPRA
        """.trimIndent()

        val result = parser(supermarketReceipt)

        assertFalse(result.isVerified)
        assertFalse(result.hasSuneduKeyword)
        assertFalse(result.hasUniversityKeyword)
    }

    @Test
    fun `empty text should return unverified`() {
        val result = parser("")
        assertFalse(result.isVerified)
    }

    @Test
    fun `continental university card with accents and student keyword should be verified`() {
        val continentalCard = """
            REPÚBLICA DEL PERÚ
            SUPERINTENDENCIA NACIONAL DE EDUCACIÓN
            UNIVERSIDAD CONTINENTAL
            ESTUDIANTE REGULAR
            CÓDIGO: 72945602
            VIGENCIA 2026
        """.trimIndent()

        val result = parser(continentalCard)

        assertTrue(result.isVerified)
        assertTrue(result.hasSuneduKeyword)
        assertTrue(result.hasUniversityKeyword)
        assertEquals("2026", result.validityYearDetected)
    }

    @Test
    fun `actual student card Manuel Fabrizio Callanaupa is verified and extracts all fields`() {
        val userCardText = """
            República del Perú
            Superintendencia Nacional de Educación Superior Universitaria
            UNIVERSIDAD CONTINENTAL
            Código: 72945602
            DNI: 72945602
            Apellidos: CALLAÑAUPA CJUIRO
            Nombres: MANUEL FABRIZIO
            Facultad: INGENIERÍA
            Carrera: ING. DE SISTEMAS E INFORMÁTICA
            Expira 02 07 27
            CARNÉ UNIVERSITARIO 2026
        """.trimIndent()

        val result = parser(userCardText)

        assertTrue(result.isVerified)
        assertTrue(result.hasSuneduKeyword)
        assertTrue(result.hasUniversityKeyword)
        assertEquals("72945602", result.studentDni)
        assertEquals("Manuel Fabrizio Callañaupa Cjuiro", result.studentName)
        assertEquals("Universidad Continental", result.universityName)
    }
}
