package com.example.estudihambres.domain.model

/**
 * Resultado estructurado del procesamiento OCR del carnet universitario mediante ML Kit.
 *
 * @property isVerified Determina si el documento cumple los criterios mínimos (SUNEDU/UNIVERSIDAD y año vigente).
 * @property hasSuneduKeyword Indica si se detectó el texto oficial de SUNEDU.
 * @property hasUniversityKeyword Indica si se detectó el término UNIVERSIDAD o acrónimo institucional.
 * @property validityYearDetected Año de vigencia o caducidad detectado (ej. 2025, 2026, 2027).
 * @property universityName Nombre de la universidad extraído si fue identificado.
 * @property rawText Texto completo reconocido por ML Kit Text Recognition.
 */
data class StudentCardOcrResult(
    val isVerified: Boolean,
    val hasSuneduKeyword: Boolean = false,
    val hasUniversityKeyword: Boolean = false,
    val validityYearDetected: String? = null,
    val universityName: String? = null,
    val rawText: String = ""
)
