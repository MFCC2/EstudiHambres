package com.example.estudihambres.data.repository

import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementación de [AuthRepository] que gestiona el estado de sesión y sincroniza con [SessionManager] para persistencia local.
 *
 * @param sessionManager Gestor opcional de persistencia mediante SharedPreferences.
 */
class MockAuthRepositoryImpl(
    sessionManager: SessionManager? = null
) : AuthRepository {

    private val session: SessionManager? = sessionManager ?: SessionManager.getInstanceOrNull()

    private val currentUserState = MutableStateFlow<StudentUser?>(
        session?.getUserSession() ?: if (session == null) {
            StudentUser(
                id = "u001",
                fullName = "Estudiante Universitario",
                email = "alumno@universidad.edu.pe",
                dni = "72345678",
                university = "Universidad Continental",
                studentCode = "72945602",
                verificationStatus = VerificationStatus.PENDING_VERIFICATION
            )
        } else {
            null
        }
    )

    override fun getCurrentUser(): Flow<StudentUser?> {
        val saved = session?.getUserSession()
        if (saved != null && currentUserState.value != saved) {
            currentUserState.value = saved
        }
        return currentUserState.asStateFlow()
    }

    override suspend fun updateVerificationStatus(status: VerificationStatus): Result<Unit> {
        val current = currentUserState.value
        return if (current != null) {
            val updated = current.copy(verificationStatus = status)
            currentUserState.value = updated
            session?.updateVerificationStatus(status)
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("No hay un usuario en sesión"))
        }
    }

    override suspend fun saveSession(user: StudentUser): Result<Unit> {
        currentUserState.value = user
        session?.saveUserSession(user)
        return Result.success(Unit)
    }

    override suspend fun clearSession(): Result<Unit> {
        currentUserState.value = null
        session?.clearSession()
        return Result.success(Unit)
    }
}
