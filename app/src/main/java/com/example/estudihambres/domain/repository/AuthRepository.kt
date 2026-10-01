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
     * Actualiza el estado de verificación del estudiante tras procesar el OCR o si omite el paso.
     *
     * @param status Nuevo [VerificationStatus] a asignar.
     */
    suspend fun updateVerificationStatus(status: VerificationStatus): Result<Unit>
}
