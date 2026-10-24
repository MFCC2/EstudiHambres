package com.example.estudihambres.data.remote.dto

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Data Transfer Object (DTO) para la colección `claimed_receipts` en Cloud Firestore.
 * Representa el registro inmutable de un comprobante electrónico SUNAT verificado.
 * Diseñado en estricta conformidad con las reglas de seguridad auditadas en [firestore.rules].
 *
 * @property fingerprint Huella digital SHA-256 única de la boleta (16 caracteres hex).
 * @property claimedByUserId Identificador de autenticación del estudiante (Firebase UID).
 * @property claimedByUserEmail Correo institucional ofuscado del reclamante.
 * @property partnerName Comercio donde se emitió el comprobante.
 * @property totalAmount Monto total en Soles (PEN).
 * @property claimedAt Marca de tiempo Firestore Timestamp de cuándo se registró el canje.
 * @property expiresAt Marca de tiempo Firestore Timestamp (claimedAt + 72 horas) para prevención de duplicados.
 */
data class ClaimedReceiptFirestoreDto(
    val fingerprint: String = "",
    val claimedByUserId: String = "",
    val claimedByUserEmail: String = "",
    val partnerName: String = "",
    val totalAmount: Double = 0.0,
    val claimedAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp(Date(claimedAt.toDate().time + (72L * 3600 * 1000)))
) {
    /**
     * Convierte el DTO a un mapa con los 7 campos estrictamente autorizados por [firestore.rules].
     */
    fun toMap(): Map<String, Any> = mapOf(
        "fingerprint" to fingerprint,
        "claimedByUserId" to claimedByUserId,
        "claimedByUserEmail" to claimedByUserEmail,
        "partnerName" to partnerName,
        "totalAmount" to totalAmount,
        "claimedAt" to claimedAt,
        "expiresAt" to expiresAt
    )
}
