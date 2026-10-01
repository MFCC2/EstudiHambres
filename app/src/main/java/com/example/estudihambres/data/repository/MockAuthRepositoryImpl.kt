package com.example.estudihambres.data.repository

import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación en memoria de [AuthRepository] que gestiona el estado de sesión y verificación del estudiante.
 */
class MockAuthRepositoryImpl : AuthRepository {

    private val currentUserState = MutableStateFlow<StudentUser?>(
        StudentUser(
            id = "u001",
            fullName = "Estudiante Universitario",
            email = "alumno@universidad.edu.pe",
            dni = "72345678",
            university = "Universidad Nacional",
            studentCode = "202400123",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION
        )
    )

    override fun getCurrentUser(): Flow<StudentUser?> {
        return currentUserState.asStateFlow()
    }

    override suspend fun updateVerificationStatus(status: VerificationStatus): Result<Unit> {
        val current = currentUserState.value
        return if (current != null) {
            currentUserState.value = current.copy(verificationStatus = status)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("No hay un usuario en sesión"))
        }
    }
}
