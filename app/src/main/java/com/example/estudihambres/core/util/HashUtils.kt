package com.example.estudihambres.core.util

import java.security.MessageDigest

/**
 * Hash SHA-256 en hexadecimal (minúsculas) compartido por contraseñas, ledger y huellas SUNAT.
 *
 * Reemplaza `joinToString { "%02x".format(it) }`, que invocaba `String.format` 32 veces por
 * hash. El ledger calcula un hash por cada [com.example.estudihambres.domain.model.PointTransaction]
 * creado, así que la conversión por tabla reduce notablemente el costo del historial.
 */
object HashUtils {

    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    fun sha256Hex(input: String): String =
        toHex(MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)))

    fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        bytes.forEachIndexed { i, b ->
            val v = b.toInt() and 0xFF
            out[i * 2] = HEX_CHARS[v ushr 4]
            out[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(out)
    }
}
