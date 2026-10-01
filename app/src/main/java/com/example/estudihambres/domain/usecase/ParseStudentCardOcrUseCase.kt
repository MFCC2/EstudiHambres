package com.example.estudihambres.domain.usecase

import com.example.estudihambres.domain.model.StudentCardOcrResult
import java.util.Locale

/**
 * Caso de uso que analiza el texto extraído por Google ML Kit Text Recognition para verificar
 * la autenticidad de un carnet universitario (Agente 3: Auth & OCR, Agente 5: QA).
 *
 * Criterios de validación:
 * 1. Detección de palabra clave "SUNEDU" o "SUPERINTENDENCIA".
 * 2. Detección de palabra clave "UNIVERSIDAD" o siglas universitarias conocidas.
 * 3. Detección de un año de vigencia actual o futuro (patrón 4 dígitos entre 2024 y 2030).
 */
class ParseStudentCardOcrUseCase {

    private val yearRegex = Regex("""\b(202[4-9]|203[0-9])\b""")

    /**
     * Procesa el texto plano extraído del carnet universitario y evalúa si califica como carnet válido.
     *
     * @param rawText Cadena de texto resultante del reconocimiento OCR.
     * @return [StudentCardOcrResult] con los hallazgos y el veredicto de validación.
     */
    operator fun invoke(rawText: String): StudentCardOcrResult {
        if (rawText.isBlank()) {
            return StudentCardOcrResult(isVerified = false, rawText = rawText)
        }

        val upperText = rawText.uppercase(Locale.ROOT)

        val hasSunedu = upperText.contains("SUNEDU") || upperText.contains("SUPERINTENDENCIA")
        val hasUniversity = upperText.contains("UNIVERSIDAD") ||
                upperText.contains("UNMSM") ||
                upperText.contains("UNI") ||
                upperText.contains("PUCP") ||
                upperText.contains("UPC") ||
                upperText.contains("UTP")

        val yearMatch = yearRegex.find(upperText)?.value

        // Extracción simple de la línea que contiene "UNIVERSIDAD"
        val universityLine = rawText.lines()
            .firstOrNull { it.uppercase(Locale.ROOT).contains("UNIVERSIDAD") }
            ?.trim()

        // Es verificado si tiene al menos SUNEDU y UNIVERSIDAD, o UNIVERSIDAD + Año de vigencia válido
        val isVerified = (hasSunedu && hasUniversity) || (hasUniversity && yearMatch != null)

        return StudentCardOcrResult(
            isVerified = isVerified,
            hasSuneduKeyword = hasSunedu,
            hasUniversityKeyword = hasUniversity,
            validityYearDetected = yearMatch,
            universityName = universityLine,
            rawText = rawText
        )
    }
}
