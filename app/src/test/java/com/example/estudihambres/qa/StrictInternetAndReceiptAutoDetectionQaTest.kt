package com.example.estudihambres.qa

import com.example.estudihambres.core.util.NetworkMonitor
import com.example.estudihambres.domain.service.SunatCpeValidatorBot
import com.example.estudihambres.domain.usecase.DetectReceiptEstablishmentUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite integral de pruebas QA para los requerimientos normativos:
 * 1. Detección automática del establecimiento comercial a partir del texto OCR del comprobante y/o RUC.
 * 2. Eliminación de datos predeterminados en el escáner (sin 18.50 ni presets de marcas aliadas fijas).
 * 3. Bot scraper de resultados oficiales en el portal SUNAT (ConsValiCpe.htm).
 * 4. Monitor de conectividad estricta para garantizar que la app requiera conexión a internet.
 */
class StrictInternetAndReceiptAutoDetectionQaTest {

    private lateinit var detector: DetectReceiptEstablishmentUseCase
    private lateinit var sunatBot: SunatCpeValidatorBot

    @Before
    fun setUp() {
        detector = DetectReceiptEstablishmentUseCase()
        sunatBot = SunatCpeValidatorBot()
    }

    // =========================================================================
    // 1. Detección automática de establecimientos comerciales
    // =========================================================================

    @Test
    fun testDetectEstablishment_popularBrandsInOcr() {
        // Simulación de ticket Bembos
        val bembosTicket = """
            BOLETA DE VENTA ELECTRONICA
            BEMBOS - REAL PLAZA CENTRO CIVICO
            RUC: 20252391498
            SERIE: B001-00049281
            TOTAL: S/ 22.90
        """.trimIndent()
        assertEquals("Bembos", detector(bembosTicket))

        // Simulación de ticket Starbucks
        val starbucksTicket = """
            DELOSI S.A.
            STARBUCKS COFFEE LARCOMAR
            RUC: 20100107214
            SERIE: F001-00018420
            TOTAL: S/ 19.50
        """.trimIndent()
        assertEquals("Starbucks", detector(starbucksTicket))

        // Simulación de ticket Cineplanet
        val cineTicket = """
            CINEPLEX S.A.
            CINEPLANET ALCAZAR
            BOLETA ELECTRONICA: B002-00091823
            TOTAL: S/ 36.00
        """.trimIndent()
        assertEquals("Cineplanet", detector(cineTicket))

        // Simulación de ticket KFC
        val kfcTicket = """
            KFC JAVIER PRADO
            COMPROBANTE DE PAGO
            TOTAL S/ 28.00
        """.trimIndent()
        assertEquals("KFC", detector(kfcTicket))

        // Simulación de ticket Tambo
        val tamboTicket = """
            TIENDAS TAMBO SAC
            TAMBO+ AV. AREQUIPA
            TOTAL S/ 12.50
        """.trimIndent()
        assertEquals("Tiendas Tambo", detector(tamboTicket))
    }

    @Test
    fun testDetectEstablishment_generalPeruvianCompanySuffixes() {
        // Restaurante independiente universitario con S.A.C.
        val localSacTicket = """
            RESTAURANTE EL BUEN SABOR S.A.C.
            AV. UNIVERSITARIA 1234 - SAN MIGUEL
            RUC: 20601234567
            BOLETA DE VENTA ELECTRONICA: B001-00001234
            TOTAL S/ 14.00
        """.trimIndent()
        val detected = detector(localSacTicket)
        assertTrue(
            "Debe reconocer la razón social en la cabecera",
            detected.contains("EL BUEN SABOR", ignoreCase = true)
        )
    }

    @Test
    fun testDetectEstablishment_fromRucDirectory() {
        val rucBembos = "20252391498"
        val rucStarbucks = "20100107214"
        val rucCineplanet = "20429683581"

        assertEquals("Bembos (InRetail)", DetectReceiptEstablishmentUseCase.detectFromRuc(rucBembos))
        assertEquals("Delosi (Starbucks / KFC / Burger King)", DetectReceiptEstablishmentUseCase.detectFromRuc(rucStarbucks))
        assertEquals("Cineplanet", DetectReceiptEstablishmentUseCase.detectFromRuc(rucCineplanet))
    }

    @Test
    fun testDetectEstablishment_noFalsePositiveOnTaxBoilerplate() {
        val genericBoilerplate = """
            BOLETA DE VENTA ELECTRONICA
            RUC: 20999999999
            NRO: B001-00000001
            FECHA: 12/11/2026
            TOTAL: S/ 15.00
        """.trimIndent()

        val detected = detector(genericBoilerplate, "20999999999")
        // No debe retornar "BOLETA" ni "VENTA" ni "ELECTRONICA"
        assertFalse(detected.equals("BOLETA", ignoreCase = true))
        assertFalse(detected.equals("VENTA", ignoreCase = true))
        assertFalse(detected.equals("ELECTRONICA", ignoreCase = true))
    }

    // =========================================================================
    // 2. Bot Scraper de Resultados Oficiales en el Portal SUNAT
    // =========================================================================

    @Test
    fun testSunatBot_resultScraperScriptStructure() {
        val scraper = sunatBot.generateResultScraperScript()
        assertNotNull(scraper)
        assertTrue("Debe ser una IIFE", scraper.contains("(function() {"))
        assertTrue("Debe buscar el estado ACEPTADO en el DOM", scraper.contains("ACEPTADO"))
        assertTrue("Debe buscar el estado NO_EXISTE", scraper.contains("NO_EXISTE"))
        assertTrue("Debe buscar el estado RECHAZADO", scraper.contains("RECHAZADO"))
        assertTrue("Debe verificar si la consulta continúa cargando", scraper.contains("CARGANDO"))
    }

    @Test
    fun testSunatBot_autoFillScriptDispatchesDomEvents() {
        val script = sunatBot.generateAutoFillScript(
            ruc = "20100107214",
            tipoCpe = "01", // Factura
            serie = "F001",
            correlativo = "18420",
            fechaEmision = "13/11/2026",
            monto = 26.00
        )
        assertNotNull(script)
        assertTrue("Debe disparar evento input para validaciones SUNAT", script.contains("dispatchEvent(new Event('input'"))
        assertTrue("Debe disparar evento change para habilitar el selector", script.contains("dispatchEvent(new Event('change'"))
        assertTrue("Debe inyectar '03' para Factura en el portal web", script.contains("f.tipocomprobante.value = '03';"))
        assertTrue("Debe inyectar la cantidad formateada", script.contains("f.cantidad.value = '26.00';"))
    }

    // =========================================================================
    // 3. Monitor de Red y Bloqueo Estricto Sin Conexión
    // =========================================================================

    @Test
    fun testNetworkMonitor_stateFlowAvailable() {
        val monitor = NetworkMonitor(null)
        val isOnlineFlow = monitor.isOnline
        assertNotNull(isOnlineFlow)
        assertNotNull(isOnlineFlow.value)
    }

    @Test
    fun testReceiptScannerRules_noDefault1850() {
        // Comprobamos que el caso de uso no impone 18.50 ni Bembos como valores por omisión
        val emptyTicket = ""
        val result = detector(emptyTicket)
        assertEquals("Comercio no identificado", result)
    }
}
