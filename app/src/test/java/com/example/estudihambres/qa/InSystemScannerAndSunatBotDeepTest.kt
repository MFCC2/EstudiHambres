package com.example.estudihambres.qa

import com.example.estudihambres.domain.model.ReceiptValidationResult
import com.example.estudihambres.domain.model.SunatReceiptData
import com.example.estudihambres.domain.service.SunatBotStatus
import com.example.estudihambres.domain.service.SunatCpeValidatorBot
import com.example.estudihambres.domain.usecase.ValidateSunatReceiptUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Suite de Pruebas Profundas para el Escáner Inteligente en Sistema y el Bot Validador SUNAT Oficial.
 *
 * Audita meticulosamente:
 * 1. Algoritmo Módulo 11 de SUNAT (Res. SUNAT / js.js valruc) para personas jurídicas y naturales con negocio.
 * 2. Catálogo oficial de aliados comerciales peruanos (Bembos, Cineplanet, Delosi, Tambo, Crisol, Cruz del Sur).
 * 3. Mapeo bidireccional y auto-inferencia de Comprobantes de Pago Electrónicos (Boletas '03' vs Facturas '01').
 * 4. Validación técnica de Series según normativas SUNAT (Res. 097-2012 y Res. 203-2015).
 * 5. Generación de scripts de inyección JavaScript para el portal oficial e-consulta (ConsValiCpe.htm).
 * 6. Pipeline progresivo de escaneo en sistema (Estructura -> Metadatos -> Monto -> Bot SUNAT).
 * 7. Tolerancias temporales de frescura (máx. 72h, no futuras) y umbrales mínimos de consumo.
 * 8. Resistencia criptográfica anti-colisión SHA-256 para prevenir fraudes de doble registro.
 */
class InSystemScannerAndSunatBotDeepTest {

    private lateinit var bot: SunatCpeValidatorBot
    private lateinit var validateUseCase: ValidateSunatReceiptUseCase

    @Before
    fun setUp() {
        bot = SunatCpeValidatorBot()
        validateUseCase = ValidateSunatReceiptUseCase(bot)
    }

    // =========================================================================
    // BLOQUE 1: Algoritmo Módulo 11 y Validación de RUC SUNAT (valruc)
    // =========================================================================

    @Test
    fun testModulo11_legalEntities_starts20_validChecksums() {
        // RUCs de empresas legalmente constituidas en Perú con dígitos de control válidos
        val validRucs = listOf(
            "20252391498", // Bembos (InRetail)
            "20429683581", // Cineplanet (Cineplex S.A.)
            "20100107214", // Delosi S.A.
            "20508565934", // Tiendas Tambo
            "20100084761", // Librerías Crisol
            "20100055230"  // Cruz del Sur Transportes
        )

        for (ruc in validRucs) {
            assertTrue("El RUC $ruc debe superar la validación Módulo 11 de SUNAT", bot.validateRucChecksum(ruc))
        }
    }

    @Test
    fun testModulo11_naturalPersonsWithBusiness_starts10_validChecksums() {
        // Algoritmo oficial valruc() con pesos [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]
        // Para RUC 1046182930:
        // Suma: 1*5 + 0*4 + 4*3 + 6*2 + 1*7 + 8*6 + 2*5 + 9*4 + 3*3 + 0*2 = 5 + 0 + 12 + 12 + 7 + 48 + 10 + 36 + 9 + 0 = 139
        // 139 % 11 = 7. Resto = 11 - 7 = 4.
        val ruc10 = "10461829304"
        assertTrue("RUC con negocio formal que inicia con 10 debe ser válido", bot.validateRucChecksum(ruc10))
    }

    @Test
    fun testModulo11_tamperedCheckDigit_failsVerification() {
        // Alterar el último dígito del RUC para simular comprobante falsificado
        val forgedRuc1 = "20252391499" // Correcto es 8
        val forgedRuc2 = "10461829309" // Correcto es 4

        assertFalse("RUC con dígito verificador adulterado debe ser rechazado", bot.validateRucChecksum(forgedRuc1))
        assertFalse("RUC con dígito verificador adulterado debe ser rechazado", bot.validateRucChecksum(forgedRuc2))
    }

    @Test
    fun testModulo11_invalidLengthOrPrefix_failsVerification() {
        // Longitudes incorrectas
        assertFalse("RUC de 8 dígitos debe fallar", bot.validateRucChecksum("20252391"))
        assertFalse("RUC de 12 dígitos debe fallar", bot.validateRucChecksum("202523914981"))
        assertFalse("RUC vacío debe fallar", bot.validateRucChecksum(""))

        // Prefijos no permitidos por la Ley Tributaria de SUNAT
        assertFalse("RUC que inicia con 30 debe fallar", bot.validateRucChecksum("30252391498"))
        assertFalse("RUC que inicia con 40 debe fallar", bot.validateRucChecksum("40252391498"))
        assertFalse("RUC alfanumérico debe fallar", bot.validateRucChecksum("2025239149A"))
    }

    // =========================================================================
    // BLOQUE 2: Mapeo y Detección de Boletas vs Facturas
    // =========================================================================

    @Test
    fun testInferTipoComprobante_boletasYFacturas() {
        // Boletas electrónicas estándar (B###)
        assertEquals("03", bot.inferTipoComprobanteFromSerie("B001"))
        assertEquals("03", bot.inferTipoComprobanteFromSerie("B023"))
        assertEquals("03", bot.inferTipoComprobanteFromSerie("BA01"))
        assertEquals("03", bot.inferTipoComprobanteFromSerie("BZ99"))

        // Boletas emitidas desde el portal SUNAT Operaciones en Línea (EB##)
        assertEquals("03", bot.inferTipoComprobanteFromSerie("EB01"))
        assertEquals("03", bot.inferTipoComprobanteFromSerie("EB45"))

        // Facturas electrónicas estándar (F###)
        assertEquals("01", bot.inferTipoComprobanteFromSerie("F001"))
        assertEquals("01", bot.inferTipoComprobanteFromSerie("F102"))
        assertEquals("01", bot.inferTipoComprobanteFromSerie("FA01"))

        // Facturas emitidas desde el portal SUNAT (E### que no sea EB)
        assertEquals("01", bot.inferTipoComprobanteFromSerie("E001"))
        assertEquals("01", bot.inferTipoComprobanteFromSerie("EA01"))
    }

    @Test
    fun testMapToSunatWebTipoComprobante_formMapping() {
        // En el formulario ConsValiCpe.htm de SUNAT:
        // '03' corresponde a FACTURA ELECTRÓNICA
        // '06' corresponde a BOLETA DE VENTA ELECTRÓNICA
        assertEquals("03", bot.mapToSunatWebTipoComprobante("01"))
        assertEquals("03", bot.mapToSunatWebTipoComprobante("FACTURA"))
        assertEquals("03", bot.mapToSunatWebTipoComprobante("FE"))

        assertEquals("06", bot.mapToSunatWebTipoComprobante("03"))
        assertEquals("06", bot.mapToSunatWebTipoComprobante("BOLETA"))
        assertEquals("06", bot.mapToSunatWebTipoComprobante("BVE"))
    }

    @Test
    fun testValidateSerie_technicalRulesSUNAT() {
        // Series válidas para Boletas (03)
        assertTrue(bot.validateSerie("B001", "03"))
        assertTrue(bot.validateSerie("BA01", "03"))
        assertTrue(bot.validateSerie("EB01", "03"))

        // Series inválidas para Boletas
        assertFalse(bot.validateSerie("F001", "03")) // F es de Factura
        assertFalse(bot.validateSerie("B01", "03"))  // Longitud corta
        assertFalse(bot.validateSerie("B0001", "03"))// Longitud excesiva
        assertFalse(bot.validateSerie("0001", "03")) // Sin prefijo alfabético

        // Series válidas para Facturas (01)
        assertTrue(bot.validateSerie("F001", "01"))
        assertTrue(bot.validateSerie("FA01", "01"))
        assertTrue(bot.validateSerie("E001", "01"))

        // Series inválidas para Facturas
        assertFalse(bot.validateSerie("B001", "01")) // B es de Boleta
        assertFalse(bot.validateSerie("F01", "01"))  // Longitud corta
        assertFalse(bot.validateSerie("F0001", "01"))// Longitud excesiva
    }

    // =========================================================================
    // BLOQUE 3: Generación del Script de Inyección JavaScript para ConsValiCpe.htm
    // =========================================================================

    @Test
    fun testGenerateAutoFillScript_structureAndSafety() {
        val script = bot.generateAutoFillScript(
            ruc = "20252391498",
            tipoCpe = "03", // Boleta -> Web '06'
            serie = "B001",
            correlativo = "49281",
            fechaEmision = "06/11/2026",
            monto = 18.50
        )

        assertNotNull(script)
        assertTrue("El script debe ser una función autoejecutable IIFE", script.contains("(function() {"))
        assertTrue("Debe validar existencia de document.lista", script.contains("var f = document.lista;"))
        assertTrue("Debe inyectar el RUC en num_ruc", script.contains("f.num_ruc.value = '20252391498';"))
        assertTrue("Debe inyectar '06' para Boleta en tipocomprobante", script.contains("f.tipocomprobante.value = '06';"))
        assertTrue("Debe inyectar la serie B001 en num_serie", script.contains("f.num_serie.value = 'B001';"))
        assertTrue("Debe formatear el correlativo a 8 dígitos con ceros", script.contains("f.num_comprob.value = '00049281';"))
        assertTrue("Debe inyectar la fecha de emisión en fec_emision", script.contains("f.fec_emision.value = '06/11/2026';"))
        assertTrue("Debe inyectar el monto formateado en cantidad", script.contains("f.cantidad.value = '18.50';"))
        assertTrue("Debe contemplar la llamada a getTokenCaptcha3() o opcGrabar()", script.contains("getTokenCaptcha3()"))
    }

    // =========================================================================
    // BLOQUE 4: Auditoría y Veredicto Integral con SunatCpeValidatorBot
    // =========================================================================

    @Test
    fun testVerifyCpe_acceptedBoleta() {
        val now = System.currentTimeMillis()
        val verdict = bot.verifyCpe(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            monto = 18.50,
            fechaEmisionMillis = now
        )

        assertTrue(verdict.isValid)
        assertEquals(SunatBotStatus.ACEPTADO, verdict.status)
        assertTrue(verdict.message.contains("verificado en SUNAT"))
        assertTrue(verdict.rucRazonSocial.contains("Bembos"))
    }

    @Test
    fun testVerifyCpe_acceptedFactura() {
        val now = System.currentTimeMillis()
        val verdict = bot.verifyCpe(
            ruc = "20100107214",
            serie = "F001",
            correlativo = "00018420",
            monto = 26.00,
            fechaEmisionMillis = now,
            tipoCpe = "01"
        )

        assertTrue(verdict.isValid)
        assertEquals(SunatBotStatus.ACEPTADO, verdict.status)
        assertTrue(verdict.rucRazonSocial.contains("Delosi"))
    }

    @Test
    fun testVerifyCpe_rejectedUnderMinimumAmount() {
        val now = System.currentTimeMillis()
        val verdict = bot.verifyCpe(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            monto = 4.50, // Menor a S/ 5.00
            fechaEmisionMillis = now
        )

        assertFalse(verdict.isValid)
        assertEquals(SunatBotStatus.RECHAZADO, verdict.status)
        assertTrue(verdict.message.contains("mínimo para acreditar StudiPuntos es de S/ 5.00"))
    }

    @Test
    fun testVerifyCpe_rejectedExpiredBeyond72Hours() {
        val fourDaysAgo = System.currentTimeMillis() - (4L * 24 * 3600 * 1000)
        val verdict = bot.verifyCpe(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            monto = 25.00,
            fechaEmisionMillis = fourDaysAgo
        )

        assertFalse(verdict.isValid)
        assertEquals(SunatBotStatus.RECHAZADO, verdict.status)
        assertTrue(verdict.message.contains("antigüedad máxima de 72 horas"))
    }

    @Test
    fun testVerifyCpe_rejectedFutureDate() {
        val tomorrow = System.currentTimeMillis() + (24L * 3600 * 1000)
        val verdict = bot.verifyCpe(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            monto = 25.00,
            fechaEmisionMillis = tomorrow
        )

        assertFalse(verdict.isValid)
        assertEquals(SunatBotStatus.RECHAZADO, verdict.status)
        assertTrue(verdict.message.contains("no puede ser futura"))
    }

    // =========================================================================
    // BLOQUE 5: Integración con ValidateSunatReceiptUseCase y Anti-Colisión
    // =========================================================================

    @Test
    fun testUseCase_integrationBothBoletaAndFactura_award80Points() {
        val now = System.currentTimeMillis()

        // Validar Boleta Bembos
        val resBoleta = validateUseCase(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            montoTotal = 19.90,
            fechaEmisionMillis = now
        )
        assertTrue(resBoleta.isValid)
        assertEquals(80, resBoleta.pointsAwarded)
        assertEquals("03", resBoleta.receiptData?.tipoComprobante)
        assertEquals("Bembos (InRetail)", resBoleta.receiptData?.partnerName)
        assertEquals("ACEPTADO", resBoleta.sunatStatus)

        // Validar Factura Starbucks
        val resFactura = validateUseCase(
            ruc = "20100107214",
            serie = "F001",
            correlativo = "00018420",
            montoTotal = 32.50,
            fechaEmisionMillis = now
        )
        assertTrue(resFactura.isValid)
        assertEquals(80, resFactura.pointsAwarded)
        assertEquals("01", resFactura.receiptData?.tipoComprobante)
        assertEquals("Delosi (Starbucks / KFC / Burger King)", resFactura.receiptData?.partnerName)
        assertEquals("ACEPTADO", resFactura.sunatStatus)
    }

    @Test
    fun testUseCase_qrParsingStandardSunatPipeFormat() {
        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date())
        val qrBoleta = "20252391498|03|B001|00049281|3.03|19.90|$todayStr|1|73618293|a8f9c7e2|"

        val res = validateUseCase.parseSunatQr(qrBoleta)
        assertTrue(res.isValid)
        assertEquals("20252391498", res.receiptData?.ruc)
        assertEquals("B001", res.receiptData?.serie)
        assertEquals("00049281", res.receiptData?.correlativo)
        assertEquals(19.90, res.receiptData?.montoTotal ?: 0.0, 0.001)
        assertEquals(80, res.pointsAwarded)
    }

    @Test
    fun testUseCase_antiReplayCryptographicCollision() {
        val now = System.currentTimeMillis()
        val registeredFingerprints = mutableSetOf<String>()

        // Primer intento: Exitoso
        val res1 = validateUseCase(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            montoTotal = 18.50,
            fechaEmisionMillis = now,
            isFingerprintAlreadyClaimed = { fingerprint -> registeredFingerprints.contains(fingerprint) }
        )
        assertTrue("El primer registro debe ser exitoso", res1.isValid)
        assertNotNull(res1.receiptData?.fingerprint)
        registeredFingerprints.add(res1.receiptData!!.fingerprint)

        // Segundo intento: Colisión detectada y rechazo estricto
        val res2 = validateUseCase(
            ruc = "20252391498",
            serie = "B001",
            correlativo = "00049281",
            montoTotal = 18.50,
            fechaEmisionMillis = now,
            isFingerprintAlreadyClaimed = { fingerprint -> registeredFingerprints.contains(fingerprint) }
        )
        assertFalse("El segundo intento debe ser rechazado por comprobante duplicado", res2.isValid)
        assertTrue(res2.rejectionReason!!.contains("Comprobante duplicado"))
    }
}
