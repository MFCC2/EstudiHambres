package com.example.estudihambres.qa

import com.example.estudihambres.domain.service.SunatBotStatus
import com.example.estudihambres.domain.service.SunatCpeValidatorBot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de pruebas unitarias para el Bot Validador de Comprobantes de SUNAT.
 * Verifica la compatibilidad con https://e-consulta.sunat.gob.pe/ol-ti-itconsvalicpe/ConsValiCpe.htm
 */
class SunatCpeValidatorBotTest {

    private lateinit var bot: SunatCpeValidatorBot

    @Before
    fun setUp() {
        bot = SunatCpeValidatorBot()
    }

    @Test
    fun testRucModulo11ChecksumValidation() {
        // RUCs reales de empresas peruanas autorizadas
        assertTrue("RUC Delosi debe ser válido", bot.validateRucChecksum("20100107214"))
        assertTrue("RUC Bembos debe ser válido", bot.validateRucChecksum("20252391498"))
        assertTrue("RUC Cineplanet debe ser válido", bot.validateRucChecksum("20429683581"))
        assertTrue("RUC Tambo debe ser válido", bot.validateRucChecksum("20508565934"))

        // RUCs inválidos
        assertFalse("RUC con longitud incorrecta debe fallar", bot.validateRucChecksum("12345"))
        assertFalse("RUC con prefijo no tributario debe fallar", bot.validateRucChecksum("30100107214"))
        assertFalse("RUC con checksum alterado debe fallar", bot.validateRucChecksum("20100107219"))
    }

    @Test
    fun testInferTipoComprobanteFromSerie() {
        assertEquals("03", bot.inferTipoComprobanteFromSerie("B001"))
        assertEquals("03", bot.inferTipoComprobanteFromSerie("BA01"))
        assertEquals("03", bot.inferTipoComprobanteFromSerie("EB01"))
        assertEquals("01", bot.inferTipoComprobanteFromSerie("F001"))
        assertEquals("01", bot.inferTipoComprobanteFromSerie("FC01"))
        assertEquals("01", bot.inferTipoComprobanteFromSerie("E001"))
    }

    @Test
    fun testMapToSunatWebTipoComprobante() {
        // En ConsValiCpe.htm: '03' es Factura y '06' es Boleta
        assertEquals("03", bot.mapToSunatWebTipoComprobante("01"))
        assertEquals("06", bot.mapToSunatWebTipoComprobante("03"))
    }

    @Test
    fun testGenerateAutoFillScriptContainsAllSunatFields() {
        val script = bot.generateAutoFillScript(
            ruc = "20252391498",
            tipoCpe = "03", // Boleta
            serie = "B001",
            correlativo = "49281",
            fechaEmision = "06/11/2026",
            monto = 24.50
        )

        assertNotNull(script)
        assertTrue(script.contains("f.num_ruc.value = '20252391498'"))
        assertTrue(script.contains("f.tipocomprobante.value = '06'")) // Boleta en web
        assertTrue(script.contains("f.num_serie.value = 'B001'"))
        assertTrue(script.contains("f.num_comprob.value = '00049281'"))
        assertTrue(script.contains("f.fec_emision.value = '06/11/2026'"))
        assertTrue(script.contains("f.cantidad.value = '24.50'"))
        assertTrue(script.contains("getTokenCaptcha3()"))
    }

    @Test
    fun testVerifyCpeFacturaAndBoleta() {
        val now = System.currentTimeMillis()

        // 1. Boleta electrónica válida
        val verdictBoleta = bot.verifyCpe(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            monto = 18.50,
            fechaEmisionMillis = now - 3600_000L
        )
        assertTrue("Boleta debe ser aceptada", verdictBoleta.isValid)
        assertEquals(SunatBotStatus.ACEPTADO, verdictBoleta.status)
        assertTrue(verdictBoleta.message.contains("Boleta de Venta Electrónica"))

        // 2. Factura electrónica válida
        val verdictFactura = bot.verifyCpe(
            ruc = "20100107214",
            serie = "F001",
            correlativo = "00012345",
            monto = 85.00,
            fechaEmisionMillis = now - 3600_000L
        )
        assertTrue("Factura debe ser aceptada", verdictFactura.isValid)
        assertEquals(SunatBotStatus.ACEPTADO, verdictFactura.status)
        assertTrue(verdictFactura.message.contains("Factura Electrónica"))
    }

    @Test
    fun testVerifyCpeRejections() {
        val now = System.currentTimeMillis()

        // Monto menor a S/ 5.00
        val lowAmount = bot.verifyCpe("20252391498", "B001", "123", 2.00, now)
        assertFalse(lowAmount.isValid)
        assertEquals(SunatBotStatus.RECHAZADO, lowAmount.status)

        // Serie no válida
        val invalidSerie = bot.verifyCpe("20252391498", "Z999", "123", 20.00, now)
        assertFalse(invalidSerie.isValid)
        assertEquals(SunatBotStatus.FORMATO_INVALIDO, invalidSerie.status)
    }
}
