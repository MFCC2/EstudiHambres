package com.example.estudihambres.domain.usecase

import com.example.estudihambres.domain.model.ReceiptSource
import com.example.estudihambres.domain.model.ScannedReceipt
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Extrae los datos de un comprobante electrónico a partir del QR SUNAT o del texto OCR.
 *
 * Unifica la lógica que estaba duplicada (con variaciones) en tres visores de cámara distintos.
 * Es puro (sin Android) para poder probarse en la JVM.
 */
object ReceiptScanParser {

    private val RUC_REGEX = Regex("""\b((?:10|15|17|20)\d{9})\b""")
    private val RUC_LABELED_REGEX = Regex("""R\.?\s?U\.?\s?C\.?\s*[:#N°º.]*\s*((?:10|15|17|20)[\d\s-]{9,13})""")
    private val SERIE_NUMERO_REGEX =
        Regex("""\b([BF][A-Z0-9]{3}|EB[A-Z0-9]{2}|E[A-Z0-9]{3})\s*[-–:/#N°º.\s]\s*0*([0-9]{1,8})\b""")
    private val TOTAL_REGEX = Regex(
        """\b(?:IMPORTE\s+TOTAL|TOTAL\s+A\s+PAGAR|TOTAL\s+VENTA|VENTA\s+TOTAL|TOTAL)\s*(?:S/\.?|PEN)?\s*[:=]?\s*(?:S/\.?|PEN)?\s*(\d{1,5}[.,]\d{2})\b"""
    )
    private val DATE_REGEX = Regex("""\b(\d{2}[/-]\d{2}[/-]\d{4}|\d{4}-\d{2}-\d{2})\b""")
    private val VALID_SERIE = Regex("""^(?:[BF][A-Z0-9]{3}|EB[A-Z0-9]{2}|E[A-Z0-9]{3})$""")

    /**
     * Formato del QR SUNAT: `RUC|TIPO|SERIE|NUMERO|IGV|TOTAL|FECHA|TIPO_DOC_CLIENTE|DOC_CLIENTE|...`
     */
    fun parseQr(raw: String): ScannedReceipt? {
        val parts = raw.split('|').map { it.trim() }
        if (parts.size < 6) return null
        val ruc = parts[0]
        val tipo = parts[1]
        val serie = parts[2].uppercase()
        val numero = parts[3].trimStart('0').ifEmpty { "0" }
        if (!ruc.matches(Regex("""^(?:10|15|17|20)\d{9}$"""))) return null
        if (tipo != "01" && tipo != "03") return null
        if (!VALID_SERIE.matches(serie) || !numero.matches(Regex("""^\d{1,8}$"""))) return null
        return ScannedReceipt(
            ruc = ruc,
            serie = serie,
            numero = numero,
            amount = parts[5].replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 },
            issueDateMillis = parts.getOrNull(6)?.let(::parseDate),
            source = ReceiptSource.QR,
            rawQr = raw.trim()
        )
    }

    /** Extrae los datos del texto impreso. Devuelve null si falta RUC o serie-número. */
    fun parseOcr(text: String): ScannedReceipt? {
        val upper = text.uppercase()
        val ruc = RUC_REGEX.find(upper)?.groupValues?.get(1)
            ?: RUC_LABELED_REGEX.find(upper)?.groupValues?.get(1)
                ?.filter { it.isDigit() }
                ?.takeIf { it.length == 11 }
            ?: return null
        // Las series reales (B001, F001, EB01) siempre incluyen un dígito: así se descartan
        // palabras de 4 letras seguidas de números ("FONO: 984...").
        val combo = SERIE_NUMERO_REGEX.findAll(upper)
            .firstOrNull { match -> match.groupValues[1].any { it.isDigit() } }
            ?: return null
        val amount = TOTAL_REGEX.findAll(upper)
            .mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }
            .lastOrNull()
        val date = DATE_REGEX.find(upper)?.value?.let(::parseDate)
        return ScannedReceipt(
            ruc = ruc,
            serie = combo.groupValues[1],
            numero = combo.groupValues[2].trimStart('0').ifEmpty { "0" },
            amount = amount?.takeIf { it > 0.0 },
            issueDateMillis = date,
            source = ReceiptSource.OCR,
            ocrText = text
        )
    }

    /** Fecha estricta (sin fechas "corregidas" como 32/13/2026). Null si no es válida. */
    fun parseDate(value: String): Long? {
        val clean = value.trim()
        if (clean.isEmpty()) return null
        for (pattern in listOf("yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.ROOT).apply { isLenient = false }.parse(clean)
            }.getOrNull()
            if (parsed != null && clean.length == pattern.length) return parsed.time
        }
        return null
    }
}
