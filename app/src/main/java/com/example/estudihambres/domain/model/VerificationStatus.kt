package com.example.estudihambres.domain.model

/**
 * Representa el estado de verificación estudiantil dentro de CampusPass.
 *
 * De acuerdo a las reglas del negocio:
 * - [VERIFIED]: Carnet o credencial validado correctamente mediante OCR/proceso.
 * - [PENDING_VERIFICATION]: El usuario omitió o tiene en proceso la verificación sin bloquear su acceso a la app.
 * - [UNVERIFIED]: El usuario aún no ha iniciado el proceso de verificación.
 * - [REJECTED]: La verificación fue denegada o no cumplió los requisitos de validación.
 */
enum class VerificationStatus {
    VERIFIED,
    PENDING_VERIFICATION,
    UNVERIFIED,
    REJECTED
}
