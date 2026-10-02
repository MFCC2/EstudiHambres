package com.example.estudihambres.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.domain.model.VerificationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI para la gestión y configuración del perfil del estudiante universitario.
 */
data class ProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val dni: String = "",
    val university: String = "",
    val studentCode: String = "",
    val career: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING_VERIFICATION,
    val accumulatedSavings: Double = 142.50,
    val couponsRedeemed: Int = 8,
    val radarRadiusKm: Float = 30f,
    val notificationsEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel encargado de la carga, edición y persistencia del perfil universitario.
 */
class ProfileViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val user = sessionManager.getUserSession()
        if (user != null) {
            _uiState.update {
                it.copy(
                    fullName = user.fullName,
                    email = user.email,
                    dni = user.dni,
                    university = user.university,
                    studentCode = user.studentCode,
                    career = user.career,
                    verificationStatus = user.verificationStatus,
                    radarRadiusKm = sessionManager.getRadarRadiusKm(),
                    notificationsEnabled = sessionManager.getNotificationsEnabled()
                )
            }
        }
    }

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }
    fun onDniChange(value: String) = _uiState.update { it.copy(dni = value.filter { c -> c.isDigit() }.take(8)) }
    fun onUniversityChange(value: String) = _uiState.update { it.copy(university = value) }
    fun onStudentCodeChange(value: String) = _uiState.update { it.copy(studentCode = value) }
    fun onCareerChange(value: String) = _uiState.update { it.copy(career = value) }
    
    fun onRadarRadiusChange(value: Float) {
        _uiState.update { it.copy(radarRadiusKm = value) }
        sessionManager.setRadarRadiusKm(value)
    }

    fun onNotificationsToggle(value: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = value) }
        sessionManager.setNotificationsEnabled(value)
    }

    fun saveProfile() {
        val state = _uiState.value
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre no puede estar vacío") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            sessionManager.updateProfile(
                fullName = state.fullName.trim(),
                email = state.email.trim(),
                dni = state.dni.trim(),
                university = state.university.trim(),
                studentCode = state.studentCode.trim(),
                career = state.career.trim()
            )
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveSuccessMessage = "Perfil universitario actualizado correctamente"
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(saveSuccessMessage = null, errorMessage = null) }
    }
}

class ProfileViewModelFactory(
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(sessionManager) as T
    }
}
