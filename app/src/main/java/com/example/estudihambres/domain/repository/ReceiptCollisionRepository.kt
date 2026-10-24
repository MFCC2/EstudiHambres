package com.example.estudihambres.domain.repository

/**
 * Repositorio para la verificación y registro atómico de comprobantes de pago (SUNAT)
 * en la nube (Cloud Firestore) para prevenir fraudes por escaneos duplicados entre múltiples usuarios.
 *
 * Cumple con la normativa de comprobantes electrónicos (Res. SUNAT N° 097-2012) y
 * la Ley de Protección de Datos Personales (Ley N° 29733).
 */
interface ReceiptCollisionRepository {

    /**
     * Comprueba si una huella digital SHA-256 de boleta ya fue canjeada en la ventana de 72 horas
     * por cualquier usuario en todo el sistema.
     *
     * @param fingerprint Huella compacta SHA-256 (16 caracteres hex) calculada de RUC + Serie + Correlativo.
     * @return `true` si la boleta ya fue registrada previamente, `false` si es apta para canje.
     */
    suspend fun isReceiptClaimed(fingerprint: String): Boolean

    /**
     * Registra de manera atómica el canje de la boleta en el registro global.
     * Si la boleta ya existe (condición de carrera entre 10 estudiantes escaneando la misma boleta),
     * la operación es rechazada garantizando consistencia estricta.
     *
     * @param fingerprint Huella única de la boleta.
     * @param userId Identificador único del estudiante (UID).
     * @param userEmail Correo del estudiante que realiza el canje.
     * @param partnerName Razón social o nombre comercial del emisor.
     * @param amount Monto total de la boleta en Soles (PEN).
     * @return [Result.success] si se registró exitosamente, o [Result.failure] con la excepción correspondiente.
     */
    suspend fun claimReceipt(
        fingerprint: String,
        userId: String,
        userEmail: String,
        partnerName: String,
        amount: Double
    ): Result<Unit>
}
