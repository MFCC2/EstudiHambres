package com.example.estudihambres.data

import com.example.estudihambres.data.repository.MockAuthRepositoryImpl
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
 * Pruebas unitarias para [MockAuthRepositoryImpl] garantizando integridad de sesión
 * y transiciones de estado de verificación.
 */
class MockAuthRepositoryTest {

    private lateinit var repository: MockAuthRepositoryImpl

    @Before
    fun setUp() {
        repository = MockAuthRepositoryImpl(sessionManager = null)
    }

    @Test
    fun `default user is provided when session manager is null`() = runBlocking {
        val user = repository.getCurrentUser().first()
        assertNotNull(user)
        assertEquals("u001", user?.id)
        assertEquals(VerificationStatus.PENDING_VERIFICATION, user?.verificationStatus)
    }

    @Test
    fun `saveSession updates current user correctly`() = runBlocking {
        val newUser = StudentUser(
            id = "usr-custom-99",
            fullName = "Carlos Pérez",
            email = "cperez@continental.edu.pe",
            dni = "78965412",
            university = "Universidad Continental",
            studentCode = "78965412",
            verificationStatus = VerificationStatus.PENDING_VERIFICATION
        )

        repository.saveSession(newUser)
        val user = repository.getCurrentUser().first()

        assertEquals("usr-custom-99", user?.id)
        assertEquals("Carlos Pérez", user?.fullName)
        assertEquals("cperez@continental.edu.pe", user?.email)
    }

    @Test
    fun `updateVerificationStatus changes status to VERIFIED`() = runBlocking {
        val result = repository.updateVerificationStatus(VerificationStatus.VERIFIED)
        assertTrue(result.isSuccess)

        val user = repository.getCurrentUser().first()
        assertEquals(VerificationStatus.VERIFIED, user?.verificationStatus)
    }

    @Test
    fun `clearSession removes current user`() = runBlocking {
        val result = repository.clearSession()
        assertTrue(result.isSuccess)

        val user = repository.getCurrentUser().first()
        assertNull(user)
    }
}
