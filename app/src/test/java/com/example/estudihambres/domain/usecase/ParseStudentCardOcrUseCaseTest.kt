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
            VÁLIDO 2026
        """.trimIndent()

        val result = parser(sampleOcrText)

        assertTrue(result.isVerified)
        assertTrue(result.hasUniversityKeyword)
        assertEquals("2026", result.validityYearDetected)
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
        assertTrue(result.isCompleteForRegistration)

    }

    @Test
    fun `video sample with semester and uncoloned faculty extracts accurately and rejects noise`() {
        val ocrFromVideo = """
            República del Perú
            Superintendencia Nacional de Educación Superior Universitaria
            UNIVERSIDAD CONTINENTAL
            2023 - I
            CALLANAUPA QUIRO
            MANUEL FABRIZIO
            D.N.I.
            72945602
            FAC. DE INGENIERÍA
            ING. DE SISTEMAS E INFORMÁTICA
            CADUCIDAD 2026
            CARNÉ UNIVERSITARIO
        """.trimIndent()

        val result = parser(ocrFromVideo)

        assertTrue(result.isVerified)
        assertEquals("72945602", result.studentDni)
        assertEquals("72945602", result.studentCode)
        assertEquals("Ingeniería", result.faculty)
        assertEquals("Ing. De Sistemas E Informática", result.career)
        assertTrue(result.isCompleteForRegistration)

    }

    @Test
    fun `two letter noise like ga is rejected for student code`() {
        val ocrWithGa = """
            REPÚBLICA DEL PERÚ
            SUNEDU
            UNIVERSIDAD CONTINENTAL
            CÓD: ga
            DNI: 72945602
            APELLIDOS: CALLANAUPA QUIRO
            NOMBRES: MANUEL FABRIZIO
            2u23-1
            CARNÉ UNIVERSITARIO 2026
        """.trimIndent()

        val result = parser(ocrWithGa)

        assertTrue(result.isVerified)
        assertEquals("72945602", result.studentCode)
        org.junit.Assert.assertNotEquals("2u23-1", result.faculty)
        org.junit.Assert.assertNotEquals("2u23-1", result.career)
        assertTrue(result.isCompleteForRegistration)

    }

    @Test
    fun `peruvian national DNI is rejected with informative message and flagged as DNI`() {
        val dniOcrText = """
            REPÚBLICA DEL PERÚ
            REGISTRO NACIONAL DE IDENTIFICACIÓN Y ESTADO CIVIL
            DOCUMENTO NACIONAL DE IDENTIDAD
            72945602
            CALLAÑAUPA CJUIRO
            MANUEL FABRIZIO
            FECHA DE NACIMIENTO: 15/04/2003
            SEXO: M
            ESTADO CIVIL: S
        """.trimIndent()

        val result = parser(dniOcrText)

        assertFalse(result.isVerified)
        assertTrue(result.isNationalIdCard)
        assertEquals("72945602", result.studentDni)
        assertNotNull(result.rejectionReason)
        assertTrue(result.rejectionReason!!.contains("RENIEC"))
        assertFalse(result.isCompleteForRegistration)
    }

    @Test
    fun `expired carnet with year 2024 or 2025 is marked as expired and unverified`() {
        val expiredCarnet = """
            REPÚBLICA DEL PERÚ
            SUNEDU
            CARNET UNIVERSITARIO
            UNIVERSIDAD CONTINENTAL
            ESTUDIANTE: CALLAÑAUPA CJUIRO, MANUEL FABRIZIO
            CÓDIGO: 72945602
            EXPIRA: 31/12/2024
            VÁLIDO 2024
        """.trimIndent()

        val result = parser(expiredCarnet)

        assertFalse(result.isVerified)
        assertFalse(result.isVigente)
        assertEquals("2024", result.validityYearDetected)
        assertTrue(result.studentStatus.contains("Caducado"))
        assertNotNull(result.rejectionReason)
        assertTrue(result.rejectionReason!!.contains("caducado"))
        assertFalse(result.isCompleteForRegistration)

    }
    }
