package com.example.estudihambres.domain.model

/**
 * Resultado estructurado del procesamiento OCR del carnet universitario oficial peruano
 * expedido bajo la normativa de SUNEDU.
 *
 * Extrae y valida los 9 campos institucionales esenciales:
 * 1. [studentCode] - Código de estudiante universitario (ej. 0020186238, 1003005322).
 * 2. [studentDni] - Documento Nacional de Identidad de 8 dígitos numéricos peruanos.
 * 3. [universityName] - Universidad o institución de educación superior acreditada.
 * 4. [studentName] - Nombres y apellidos completos del titular.
 * 5. [faculty] - Facultad de pertenencia (ej. Humanidades, Ciencias de la Salud, Post Grado).
 * 6. [career] - Carrera profesional o posgrado (ej. Comunicación, Medicina Humana, Maestría).
 * 7. [suneduSealDescription] - Identificación del Sello Oficial SUNEDU (membrete, marca de agua de seguridad guilloché y holograma).
 * 8. [validityYearDetected] - Año de vigencia o fecha exacta de expiración (ej. 2025, 2026, 23/08/2026).
 * 9. [studentStatus] - Estado de condición del estudiante (ej. Activo / Vigente - Pregrado Regular o Postgrado).
 *
 * @property isVerified Determina si el documento cumple los criterios de autenticidad de SUNEDU.
 * @property hasSuneduKeyword Indica si se detectó el texto oficial de SUNEDU o la Superintendencia.
 * @property hasUniversityKeyword Indica si se detectó el término UNIVERSIDAD o acrónimo institucional.
 * @property hasSuneduSeal Indica si se validó la presencia de los sellos oficiales de SUNEDU.
 * @property isVigente Determina si el carnet se encuentra dentro de su periodo de vigencia legal.
 * @property apellidos Apellidos separados extraídos del carnet.
 * @property nombres Nombres separados extraídos del carnet.
 * @property expirationDate Fecha formateada de caducidad si fue identificada.
 * @property rawText Texto completo reconocido por ML Kit Text Recognition.
 */
data class StudentCardOcrResult(
    val isVerified: Boolean,
    val hasSuneduKeyword: Boolean = false,
    val hasUniversityKeyword: Boolean = false,
    val hasSuneduSeal: Boolean = false,
    val suneduSealDescription: String = "Sello Oficial Holográfico SUNEDU (Superintendencia Nacional de Educación Superior Universitaria)",
    val validityYearDetected: String? = null,
    val expirationDate: String? = null,
    val universityName: String? = null,
    val studentName: String? = null,
    val apellidos: String? = null,
    val nombres: String? = null,
    val studentDni: String? = null,
    val studentCode: String? = null,
    val career: String? = null,
    val faculty: String? = null,
    val studentStatus: String = "Estudiante Regular (Vigente)",
    val isVigente: Boolean = true,
    val rawText: String = ""
)
