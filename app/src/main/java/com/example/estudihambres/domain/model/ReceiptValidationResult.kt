package com.example.estudihambres.domain.model

/**
 * Resultado integral del análisis y auditoría de una boleta de venta electrónica.
 *
 * @property isValid Indica si el comprobante cumple todas las reglas fiscales y de seguridad.
 * @property receiptData Datos fiscales extraídos si el análisis fue exitoso.
 * @property pointsAwarded Puntos otorgados por la validación exitosa (habitualmente +80).
 * @property rejectionReason Causa de rechazo detallada en caso de no ser válida.
 */
data class ReceiptValidationResult(
    val isValid: Boolean,
    val receiptData: SunatReceiptData? = null,
    val pointsAwarded: Int = 0,
    val rejectionReason: String? = null
)
