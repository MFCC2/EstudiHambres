package com.example.estudihambres.qa

import com.example.estudihambres.domain.usecase.ParseStudentCardOcrUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de Pruebas QA para Detección y Parseo OCR de Carnet Universitario (Agente 5: QA).
 * Valida 12 situaciones reales con carnet peruano oficial, variantes, códigos y texto ruidoso.
 */
class StudentCardOcrQaTest {

    private lateinit var parser: ParseStudentCardOcrUseCase

    @Before
    fun setUp() {
        parser = ParseStudentCardOcrUseCase()
    }

    @Test
    fun testOcrSuneduCard2026Official() {
        val ocr = """
            REPÚBLICA DEL PERÚ
            SUNEDU
            CARNET UNIVERSITARIO
            UNIVERSIDAD CONTINENTAL
            ESTUDIANTE: CALLAÑAUPA CJUIRO, MANUEL FABRIZIO
            CÓDIGO: 72945602
            CARRERA: ING. DE SISTEMAS E INFORMÁTICA
            VÁLIDO HASTA: 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
        assertTrue(result.hasSuneduKeyword)
        assertTrue(result.hasUniversityKeyword)
        assertEquals("2026", result.validityYearDetected)
        assertEquals("Universidad Continental", result.universityName)
        assertEquals("72945602", result.studentCode)
    }

    @Test
    fun testOcrSuneduCard2027FutureValidity() {
        val ocr = """
            SUNEDU - PERÚ
            UNIVERSIDAD NACIONAL DE SAN AGUSTÍN
            VIGENCIA: 2027
            CÓDIGO: 20240102
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
        assertEquals("2027", result.validityYearDetected)
    }

    @Test
    fun testOcrCaseInsensitiveKeywords() {
        val ocr = """
            sunedu
            carnet universitario
            universidad tecnológica del perú
            vigencia 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
        assertTrue(result.hasSuneduKeyword)
        assertTrue(result.hasUniversityKeyword)
    }

    @Test
    fun testOcrUNSAACUniversityCusco() {
        val ocr = """
            UNIVERSIDAD NACIONAL DE SAN ANTONIO ABAD DEL CUSCO
            UNSAAC
            CARNET UNIVERSITARIO
            CÓDIGO: 194502
            VIGENCIA 2025
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
        assertTrue(result.universityName?.contains("San Antonio Abad") == true || result.universityName?.contains("UNSAAC") == true)
    }

    @Test
    fun testOcrSanMarcosUNMSM() {
        val ocr = """
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            DECANO DE AMÉRICA
            CÓDIGO: 20220912
            SUNEDU
            VIGENCIA 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
        assertTrue(result.hasSuneduKeyword)
    }

    @Test
    fun testOcrPUCPUniversity() {
        val ocr = """
            PONTIFICIA UNIVERSIDAD CATÓLICA DEL PERÚ
            CARNET DE ESTUDIANTE
            VÁLIDO 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
    }

    @Test
    fun testOcrStudentCodeExtractionWithPrefix() {
        val ocr = """
            UNIVERSIDAD CONTINENTAL
            CÓD. 72945602
            VIGENCIA 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
        assertEquals("72945602", result.studentCode)
    }

    @Test
    fun testOcrFullNameExtractionFromOfficialFormat() {
        val ocr = """
            SUNEDU
            UNIVERSIDAD CONTINENTAL
            APELLIDOS Y NOMBRES: CALLAÑAUPA CJUIRO, MANUEL
            VIGENCIA 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue(result.isVerified)
        assertNotNull(result.studentName)
    }

    @Test
    fun testOcrExpiredYearHandling() {
        val ocr = """
            SUNEDU
            UNIVERSIDAD CONTINENTAL
            VIGENCIA 2018
        """.trimIndent()

        val result = parser(ocr)
        assertEquals("2018", result.validityYearDetected)
    }

    @Test
    fun testOcrReceiptGarbageRejection() {
        val ocr = """
            FARMACIA UNIVERSITARIA
            BOLETA 001-92345
            PANADOL ANTIGRIPAL S/ 8.00
            TOTAL S/ 8.00
        """.trimIndent()

        val result = parser(ocr)
        assertFalse(result.isVerified)
        assertFalse(result.hasSuneduKeyword)
        assertFalse(result.hasUniversityKeyword)
    }

    @Test
    fun testOcrBlankTextRejection() {
        val result = parser("")
        assertFalse(result.isVerified)
        assertFalse(result.hasSuneduKeyword)
        assertFalse(result.hasUniversityKeyword)
    }

    @Test
    fun testOcrWhitespaceAndTabsRejection() {
        val result = parser("   \n\t   \n  ")
        assertFalse(result.isVerified)
    }
}
