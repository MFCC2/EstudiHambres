package com.example.estudihambres.domain.model

/** Cómo se obtuvieron los datos del comprobante. */
enum class ReceiptSource { QR, OCR, MANUAL }

/**
 * Comprobante que el estudiante revisa antes de corroborarlo con SUNAT y acreditar puntos.
 *
 * @property amountText Importe como lo escribe el usuario (se valida al enviar).
 * @property issueDateMillis Fecha de emisión (SUNAT exige la fecha exacta del comprobante).
 * @property rawQr Contenido del QR SUNAT si se escaneó; se usa sólo si los datos no se editaron.
 */
data class ReceiptDraft(
    val ruc: String = "",
    val serie: String = "",
    val numero: String = "",
    val amountText: String = "",
    val issueDateMillis: Long,
    val establishment: String = "",
    val source: ReceiptSource = ReceiptSource.MANUAL,
    val rawQr: String? = null
) {
    /** "01" Factura o "03" Boleta, inferido de la serie. */
    val tipo: String
        get() = if (serie.startsWith("F") || (serie.startsWith("E") && !serie.startsWith("EB"))) "01" else "03"

    val tipoLabel: String get() = if (tipo == "01") "Factura" else "Boleta"

    val amount: Double? get() = amountText.replace(',', '.').trim().toDoubleOrNull()

    val isComplete: Boolean
        get() = ruc.length == 11 && serie.length == 4 && numero.isNotEmpty() && (amount ?: 0.0) > 0.0
}

/** Datos extraídos por la cámara (QR u OCR) antes de convertirse en [ReceiptDraft]. */
data class ScannedReceipt(
    val ruc: String,
    val serie: String,
    val numero: String,
    val amount: Double?,
    val issueDateMillis: Long?,
    val source: ReceiptSource,
    val rawQr: String? = null,
    val ocrText: String? = null
)
