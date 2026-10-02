package com.example.estudihambres.domain.model

/**
 * Entidad de dominio que representa a un estudiante registrado en CampusPass.
 *
 * @property id Identificador único del usuario en el sistema.
 * @property fullName Nombre completo del estudiante.
 * @property email Correo electrónico institucional universitario.
 * @property dni Documento Nacional de Identidad o identificación oficial.
 * @property university Nombre o acrónimo de la universidad de procedencia.
 * @property studentCode Código de matrícula universitaria extraído o ingresado.
 * @property verificationStatus Estado actual de la validación del carnet estudiantil.
 */
data class StudentUser(
    val id: String,
    val fullName: String,
    val email: String,
    val dni: String,
    val university: String,
    val studentCode: String,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val career: String = "Ingeniería de Sistemas"
)
