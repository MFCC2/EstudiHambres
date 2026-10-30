package com.example.estudihambres.data

import com.example.estudihambres.data.repository.FirebaseAuthRepositoryImpl
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para [FirebaseAuthRepositoryImpl] garantizando la gestión de sesión,
 * registro y autenticación en modo fallback / JVM sin emulador.
 */
class FirebaseAuthRepositoryTest {

    private lateinit var repository: FirebaseAuthRepositoryImpl

    @Before
    fun setUp() {
        repository = FirebaseAuthRepositoryImpl(
            auth = null,
            firestore = null,
            sessionManager = null
        )
    }

    @Test
    fun defaultUserIsProvidedWhenSessionManagerIsNull() = runBlocking {
        val user = repository.getCurrentUser().first()
        assertNotNull(user)
        assertEquals("u001", user?.id)
        assertEquals(VerificationStatus.PENDING_VERIFICATION, user?.verificationStatus)
    }

    @Test
    fun authenticateWithValidSeedCredentialsSucceeds() = runBlocking {
        val result = repository.authenticate("alumno@continental.edu.pe", "campus123")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("alumno@continental.edu.pe", user?.email)
    }

    @Test
    fun authenticateWithInvalidPasswordFails() = runBlocking {
        val result = repository.authenticate("alumno@continental.edu.pe", "wrongpass")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Contraseña incorrecta") == true)
    }

    @Test
    fun registerNewStudentUserSucceeds() = runBlocking {
        val newStudent = StudentUser(
            id = "usr-cloud-01",
            fullName = "Estudiante Nube",
            email = "cloud.student@pucp.edu.pe",
            dni = "77889900",
            university = "PUCP",
            studentCode = "20261122",
            verificationStatus = VerificationStatus.UNVERIFIED,
            studiPuntos = 200
        )

        val regResult = repository.register(newStudent, "password123")
        assertTrue(regResult.isSuccess)

        val authResult = repository.authenticate("cloud.student@pucp.edu.pe", "password123")
        assertTrue(authResult.isSuccess)
        assertEquals("Estudiante Nube", authResult.getOrNull()?.fullName)
    }

    @Test
    fun updateProfileUpdatesCurrentUserState() = runBlocking {
        val updateResult = repository.updateProfile(
            fullName = "Manuel Actualizado",
            career = "Ingeniería de Software",
            studentCode = "72945602-A"
        )
        assertTrue(updateResult.isSuccess)
        val user = repository.getCurrentUser().first()
        assertEquals("Manuel Actualizado", user?.fullName)
        assertEquals("Ingeniería de Software", user?.career)
        assertEquals("72945602-A", user?.studentCode)
    }

    @Test
    fun clearSessionRemovesCurrentUser() = runBlocking {
        val clearResult = repository.clearSession()
        assertTrue(clearResult.isSuccess)
        val user = repository.getCurrentUser().first()
        assertNull(user)
    }
}
