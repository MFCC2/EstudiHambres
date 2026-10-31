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
            VIGENCIA 2026
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
        assertFalse(result.isVigente)
        assertFalse(result.isVerified)
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

    @Test
    fun testOcrUserPhotoCarolinaAlvaradoNineFieldsExtraction() {
        val ocr = """
            REPÚBLICA DEL PERÚ
            SUPERINTENDENCIA NACIONAL DE EDUCACIÓN SUPERIOR UNIVERSITARIA
            SUNEDU
            UNIVERSIDAD PRIVADA SAN JUAN BAUTISTA
            CÓDIGO: 2010103140
            DNI: 71234567
            APELLIDOS: ALVARADO PÉREZ DE LA ROSA
            NOMBRES: CAROLINA DAJHANA
            FACULTAD: COMUNICACIÓN Y CIENCIAS ADMINISTRATIVAS
            CARRERA: ADMINISTRACIÓN DE NEGOCIOS
            EXPIRA
            31
            08
            26
            CARNÉ UNIVERSITARIO 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue("Carnet oficial SUNEDU debe estar verificado", result.isVerified)
        assertTrue("Debe detectar sello oficial SUNEDU", result.hasSuneduSeal)
        assertNotNull("Descripción del sello oficial SUNEDU debe estar presente", result.suneduSealDescription)
        assertEquals("71234567", result.studentDni)
        assertEquals("2010103140", result.studentCode)
        assertEquals("31/08/2026", result.expirationDate)
        assertEquals("2026", result.validityYearDetected)
        assertTrue("Debe identificar vigencia activa", result.isVigente)
        assertEquals("Universidad Privada San Juan Bautista", result.universityName)
        assertEquals("Administración De Negocios", result.career)
        assertEquals("Comunicación Y Ciencias Administrativas", result.faculty)
        assertTrue("Estado del estudiante debe ser activo regular", result.studentStatus.contains("Activo"))
    }

    @Test
    fun testOcrUserPhotoVivianRamirezNineFieldsExtraction() {
        val ocr = """
            REPÚBLICA DEL PERÚ
            SUNEDU
            UNIVERSIDAD CONTINENTAL
            DNI: 72345678-1
            APELLIDOS: RAMÍREZ ZÁRATE
            NOMBRES: VIVIAN
            CARRERA: MEDICINA HUMANA
            EXPIRA 30/06/2026
            CARNÉ UNIVERSITARIO 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue("Carnet Vivian debe estar verificado", result.isVerified)
        assertTrue("Debe detectar membrete y sello SUNEDU", result.hasSuneduSeal)
        assertEquals("72345678", result.studentDni)
        assertEquals("Universidad Continental", result.universityName)
        assertEquals("Medicina Humana", result.career)
        assertEquals("30/06/2026", result.expirationDate)
        assertEquals("2026", result.validityYearDetected)
        assertTrue("Estado del estudiante debe ser regular activo", result.studentStatus.contains("Activo"))
    }

    @Test
    fun testOcrPostgradoStudentIdentification() {
        val ocr = """
            REPÚBLICA DEL PERÚ
            SUNEDU
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            FACULTAD: ESCUELA DE POSGRADO
            CARRERA: MAESTRÍA EN INGENIERÍA DE SOFTWARE
            DNI: 70894512
            CÓDIGO: 20235012
            VÁLIDO 2026
        """.trimIndent()

        val result = parser(ocr)
        assertTrue("Carnet de postgrado debe ser verificado", result.isVerified)
        assertTrue("Debe identificar condición de Postgrado", result.studentStatus.contains("Postgrado"))
        assertTrue("Debe estar activo", result.studentStatus.contains("Activo"))
        assertEquals("Universidad Nacional Mayor de San Marcos", result.universityName)
        assertEquals("70894512", result.studentDni)
    }
}
