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
 * Incluye respaldo en memoria para pruebas unitarias.
 *
 * @param sessionManager Gestor opcional de persistencia mediante SharedPreferences.
 */
class MockAuthRepositoryImpl(
    sessionManager: SessionManager? = null
) : AuthRepository {

    private val session: SessionManager? = sessionManager ?: SessionManager.getInstanceOrNull()

    // Memoria volátil para pruebas unitarias sin Android Context
    private val inMemoryAccounts = mutableMapOf<String, Pair<StudentUser, String>>()

    init {
        // Semilla en memoria para pruebas
        val seedUser = StudentUser(
            id = "u001",
            fullName = "Manuel Fabrizio Callañaupa",
            email = "alumno@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            career = "Ingeniería de Sistemas",
            isDev = false
        )
        inMemoryAccounts["alumno@continental.edu.pe"] = Pair(seedUser, "campus123")
    }

    private val currentUserState = MutableStateFlow<StudentUser?>(
        session?.getUserSession() ?: StudentUser(
            id = "u001",
            fullName = "Manuel Fabrizio Callañaupa",
            email = "alumno@continental.edu.pe",
            dni = "72945602",
            university = "Universidad Continental",
            studentCode = "72945602",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION,
            career = "Ingeniería de Sistemas",
            isDev = false
        )
    )

    override fun getCurrentUser(): Flow<StudentUser?> {
        val saved = session?.getUserSession()
        if (saved != null && currentUserState.value != saved) {
            currentUserState.value = saved
        }
        return currentUserState.asStateFlow()
    }

    override suspend fun authenticate(email: String, password: String): Result<StudentUser> {
        val normEmail = email.trim().lowercase()

        if (session != null) {
            val result = session.authenticate(normEmail, password)
            if (result.isSuccess) {
                currentUserState.value = result.getOrNull()
            }
            return result
        }

        // Respaldo en memoria para testing
        val account = inMemoryAccounts[normEmail]
            ?: return Result.failure(
                IllegalArgumentException("No existe ninguna cuenta registrada con el correo '$normEmail'. Por favor, regístrate primero.")
            )

        if (account.second != password) {
            return Result.failure(
                IllegalArgumentException("Contraseña incorrecta. Por favor verifica tus credenciales.")
            )
        }

        currentUserState.value = account.first
        return Result.success(account.first)
    }

    override suspend fun register(user: StudentUser, password: String): Result<StudentUser> {
        val normEmail = user.email.trim().lowercase()

        if (session != null) {
            val result = session.registerAccount(user.copy(email = normEmail), password)
            if (result.isSuccess) {
                currentUserState.value = result.getOrNull()
            }
            return result
        }

        // Respaldo en memoria
        if (inMemoryAccounts.containsKey(normEmail)) {
            return Result.failure(IllegalArgumentException("Ya existe una cuenta registrada con el correo $normEmail"))
        }

        val newUser = user.copy(email = normEmail)
        inMemoryAccounts[normEmail] = Pair(newUser, password)
        currentUserState.value = newUser
        return Result.success(newUser)
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
