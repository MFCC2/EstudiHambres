package com.example.estudihambres.domain.model

/**
 * Resultado integral del análisis y auditoría de una boleta o factura de venta electrónica SUNAT.
 *
 * @property isValid Indica si el comprobante cumple todas las reglas fiscales y de seguridad.
 * @property receiptData Datos fiscales extraídos si el análisis fue exitoso.
 * @property pointsAwarded Puntos otorgados por la validación exitosa (habitualmente +80).
 * @property rejectionReason Causa de rechazo detallada en caso de no ser válida.
 * @property sunatStatus Estado oficial retornado por el validador SUNAT (ej. "ACEPTADO", "AUTORIZADO", "RECHAZADO").
 * @property isSunatVerified Indica si el comprobante fue verificado por el motor/bot de SUNAT.
 */
data class ReceiptValidationResult(
    val isValid: Boolean,
    val receiptData: SunatReceiptData? = null,
    val pointsAwarded: Int = 0,
    val rejectionReason: String? = null,
    val sunatStatus: String = if (isValid) "ACEPTADO" else "NO_VALIDO",
    val isSunatVerified: Boolean = isValid
)
