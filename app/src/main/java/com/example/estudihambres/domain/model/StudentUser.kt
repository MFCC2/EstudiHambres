package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que representa a un estudiante registrado en EstudiHambres.
 *
 * @property id Identificador único del usuario en el sistema.
 * @property fullName Nombre completo del estudiante.
 * @property email Correo electrónico institucional universitario.
 * @property dni Documento Nacional de Identidad oficial peruano (8 dígitos).
 * @property university Nombre o acrónimo de la universidad de procedencia.
 * @property studentCode Código de matrícula universitaria.
 * @property verificationStatus Estado actual de la validación del carnet estudiantil.
 * @property career Carrera profesional o programa de estudio.
 * @property isDev Bandera de entorno para pruebas y soporte técnico.
 * @property scannedCardDni DNI extraído y verificado directamente del carnet físico mediante OCR.
 * @property profilePhotoPath Ruta local opcional de la foto de perfil.
 * @property faculty Facultad detectada o seleccionada (ej. Humanidades, Ingeniería, Post Grado).
 * @property validityYear Año de vigencia del carnet universitario.
 * @property studentStatus Condición académica del estudiante (ej. Activo - Pregrado / Postgrado).
 * @property studiPuntos Saldo actual acumulado de puntos de lealtad y recompensas.
 */
data class StudentUser(
    val id: String,
    val fullName: String,
    val email: String,
    val dni: String,
    val university: String,
    val studentCode: String,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val career: String = "Ingeniería de Sistemas",
    val isDev: Boolean = false,
    val scannedCardDni: String? = null,
    val profilePhotoPath: String? = null,
    val faculty: String? = null,
    val validityYear: String? = null,
    val studentStatus: String? = null,
    val studiPuntos: Int = 2450
)
