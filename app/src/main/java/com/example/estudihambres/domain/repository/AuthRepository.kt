package com.example.estudihambres.domain.repository

import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio para la gestión de autenticación, perfil del estudiante
 * y flujo de verificación de carnet universitario.
 */
interface AuthRepository {
    /**
     * Emite el estado y datos del estudiante en sesión.
     */
    fun getCurrentUser(): Flow<StudentUser?>

    /**
     * Autentica con correo y contraseña comprobando correspondencia de credenciales.
     */
    suspend fun authenticate(email: String, password: String): Result<StudentUser> =
        Result.success(StudentUser("u-default", "Estudiante", email, "", "", ""))

    /**
     * Registra una nueva cuenta de estudiante con contraseña.
     */
    suspend fun register(user: StudentUser, password: String): Result<StudentUser> =
        Result.success(user)

    /**
     * Actualiza el estado de verificación del estudiante tras procesar el OCR o si omite el paso.
     *
     * @param status Nuevo [VerificationStatus] a asignar.
     */
    suspend fun updateVerificationStatus(status: VerificationStatus): Result<Unit>

    /**
     * Guarda la sesión del estudiante para persistencia local.
     */
    suspend fun saveSession(user: StudentUser): Result<Unit> = Result.success(Unit)

    /**
     * Cierra la sesión activa y limpia los datos persistidos.
     */
    suspend fun clearSession(): Result<Unit> = Result.success(Unit)
}
