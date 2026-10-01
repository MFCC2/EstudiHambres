package com.example.estudihambres.domain.usecase

/**
 * Resultado de validación de campos.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Caso de uso para la validación estricta de credenciales estudiantiles (Agente 1 y Agente 5).
 * Valida formato de DNI (exactamente 8 dígitos numéricos) y correo universitario (.edu / .edu.pe).
 */
class ValidateStudentCredentialsUseCase {

    /**
     * Valida que el DNI contenga exactamente 8 dígitos numéricos.
     */
    fun validateDni(dni: String): ValidationResult {
        if (dni.isBlank()) {
            return ValidationResult(isValid = false, errorMessage = "El DNI es obligatorio")
        }
        if (dni.length != 8 || !dni.all { it.isDigit() }) {
            return ValidationResult(isValid = false, errorMessage = "El DNI debe contener exactamente 8 dígitos numéricos")
        }
        return ValidationResult(isValid = true)
    }

    /**
     * Valida que el correo corresponda a un dominio universitario reconocido.
     */
    fun validateUniversityEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(isValid = false, errorMessage = "El correo universitario es obligatorio")
        }
        if (!email.contains("@") || !email.contains(".")) {
            return ValidationResult(isValid = false, errorMessage = "Formato de correo no válido")
        }
        val domain = email.substringAfter("@").lowercase()
        val isEdu = domain.contains("edu") || domain.endsWith(".pe") || domain.contains("universidad")
        if (!isEdu) {
            return ValidationResult(isValid = false, errorMessage = "Debe ser un correo institucional (.edu / .pe)")
        }
        return ValidationResult(isValid = true)
    }

    /**
     * Valida longitud mínima de contraseña (al menos 6 caracteres).
     */
    fun validatePassword(password: String): ValidationResult {
        if (password.length < 6) {
            return ValidationResult(isValid = false, errorMessage = "La contraseña debe tener al menos 6 caracteres")
        }
        return ValidationResult(isValid = true)
    }
}
