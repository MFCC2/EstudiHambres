package com.example.estudihambres.domain.model

import java.security.MessageDigest

/**
 * Tipos de movimientos contables dentro del libro mayor de StudiPuntos.
 */
enum class PointTransactionType {
    WELCOME_BONUS,       // Bono de bienvenida al registrarse (+200)
    ID_VERIFICATION,     // Acreditación de carnet universitario SUNEDU (+300)
    COUPON_REDEEMED,     // Canje de cupón en comercio asociado (+50)
    RECEIPT_SCAN,        // Escaneo y validación de boleta electrónica (+80)
    REWARD_PURCHASE      // Canje de premio o tarjeta de regalo (-costo)
}

/**
 * Entidad de dominio inmutable que modela cada transacción de StudiPuntos.
 * Incorpora encadenamiento criptográfico ([previousHash] y [txHash]) para garantizar
 * la trazabilidad absoluta del saldo y evitar manipulaciones en almacenamiento local o memoria.
 *
 * @property id Identificador único de la transacción.
 * @property title Concepto o motivo descriptivo del movimiento.
 * @property description Detalle explicativo de la operación.
 * @property pointsDelta Puntos agregados (positivo) o descontados (negativo).
 * @property type Categoría de la transacción.
 * @property timestampMillis Momento exacto en que ocurrió la transacción.
 * @property referenceId ID del comprobante, cupón o premio asociado.
 * @property previousHash Hash criptográfico del bloque transaccional anterior.
 * @property txHash Hash criptográfico SHA-256 de este bloque.
 */
data class PointTransaction(
    val id: String,
    val title: String,
    val description: String,
    val pointsDelta: Int,
    val type: PointTransactionType,
    val timestampMillis: Long,
    val referenceId: String? = null,
    val previousHash: String = "GENESIS_BLOCK",
    val txHash: String = calculateHash(previousHash, id, pointsDelta, type.name, timestampMillis)
) {
    companion object {
        /**
         * Calcula el hash criptográfico SHA-256 del bloque para garantizar la integridad contable.
         */
        fun calculateHash(
            previousHash: String,
            id: String,
            pointsDelta: Int,
            typeName: String,
            timestampMillis: Long
        ): String {
            val payload = "$previousHash|$id|$pointsDelta|$typeName|$timestampMillis"
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
