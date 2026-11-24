package com.example.estudihambres.domain.service

import java.text.Normalizer

/** Resultado de corroborar un comprobante en la consulta pública de validez de SUNAT. */
enum class SunatCheckStatus {
    /** SUNAT indica que es un comprobante de pago válido. */
    VALID,

    /** SUNAT indica que no es válido, no existe o no fue informado. */
    NOT_VALID,

    /** SUNAT indica que el comprobante fue dado de baja / anulado. */
    CANCELLED
}

/**
 * Interpreta el texto de la página de resultado de
 * `e-consulta.sunat.gob.pe/ol-ti-itconsvalicpe/ConsValiCpe.htm`.
 *
 * SUNAT responde con frases del tipo
 * "La Boleta de Venta Electrónica B001-123 es un comprobante de pago válido.".
 * Antes la app buscaba palabras sueltas ("ERROR", "ACEPTADO") en cualquier parte de la página
 * y trataba cualquier respuesta desconocida como válida.
 */
object SunatResultClassifier {

    private val NEGATIVE_PHRASES = listOf(
        "NO ES UN COMPROBANTE DE PAGO VALIDO",
        "NO ES UN COMPROBANTE VALIDO",
        "NO ES VALIDO",
        "NO EXISTE",
        "NO HA SIDO INFORMAD",
        "NO FUE INFORMAD",
        "NO SE ENCUENTRA REGISTRAD",
        "NO SE ENCONTRO"
    )
    private val CANCELLED_PHRASES = listOf("DE BAJA", "DADO DE BAJA", "ANULAD", "RECHAZAD")
    private const val VALID_PHRASE = "ES UN COMPROBANTE DE PAGO VALIDO"

    /**
     * @param pageText Texto visible (innerText) de la página tras enviar la consulta.
     * @param serie Serie consultada (para confirmar que la respuesta es de este comprobante).
     * @param numero Número consultado.
     * @return El estado reconocido, o null si la página aún no muestra un resultado.
     */
    fun classify(pageText: String, serie: String, numero: String): SunatCheckStatus? {
        val text = normalize(pageText)
        if (text.isBlank()) return null

        // El orden importa: "NO ES UN COMPROBANTE DE PAGO VALIDO" contiene la frase positiva.
        if (NEGATIVE_PHRASES.any { it in text }) return SunatCheckStatus.NOT_VALID
        if (CANCELLED_PHRASES.any { it in text }) return SunatCheckStatus.CANCELLED
        if (VALID_PHRASE in text) {
            return if (mentionsReceipt(text, serie, numero)) SunatCheckStatus.VALID else null
        }
        return null
    }

    /** Comprueba que la respuesta cite la misma serie y número (con o sin ceros a la izquierda). */
    private fun mentionsReceipt(text: String, serie: String, numero: String): Boolean {
        val cleanSerie = normalize(serie)
        val number = numero.trim().trimStart('0').ifEmpty { "0" }
        val pattern = Regex("""${Regex.escape(cleanSerie)}\s*-\s*0*${Regex.escape(number)}\b""")
        return pattern.containsMatchIn(text)
    }

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .uppercase()
            .replace(Regex("""\s+"""), " ")
            .trim()
}
