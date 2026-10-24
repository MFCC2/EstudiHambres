package com.example.estudihambres.domain.model

import java.security.MessageDigest

/**
 * Entidad de dominio que representa los campos fiscales extraídos de una Boleta de Venta Electrónica (BVE)
 * bajo la normativa técnica de SUNAT (Res. 097-2012/SUNAT).
 *
 * @property ruc Número de RUC del comercio emisor (11 dígitos).
 * @property tipoComprobante Tipo de comprobante ("03" = Boleta de Venta Electrónica).
 * @property serie Serie fiscal del comprobante (ej. "B001", "BA01").
 * @property correlativo Número correlativo de emisión (ej. "00049281").
 * @property montoTotal Importe total cobrado en Soles (PEN).
 * @property fechaEmisionMillis Fecha de emisión del ticket en milisegundos.
 * @property digestValueHash Código hash de la firma digital generada por el OSE/SUNAT.
 * @property partnerName Nombre del comercio aliado reconocido (ej. "Bembos", "Starbucks").
 * @property fingerprint Huella digital única SHA-256 para prevenir doble gasto (Anti-Replay).
 */
data class SunatReceiptData(
    val ruc: String,
    val tipoComprobante: String,
    val serie: String,
    val correlativo: String,
    val montoTotal: Double,
    val fechaEmisionMillis: Long,
    val digestValueHash: String = "",
    val partnerName: String = "",
    val fingerprint: String = calculateFingerprint(ruc, serie, correlativo)
) {
    companion object {
        /**
         * Genera la huella única e invariable del comprobante para el registro global anti-doble gasto.
         */
        fun calculateFingerprint(ruc: String, serie: String, correlativo: String): String {
            val raw = "${ruc.trim()}-${serie.trim().uppercase()}-${correlativo.trim().padStart(8, '0')}"
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }.take(16) // Truncado a 16 bytes (32 hex)
        }
    }
}
