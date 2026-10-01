package com.example.estudihambres.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.MockAuthRepositoryImpl
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.repository.AuthRepository
import com.example.estudihambres.domain.usecase.ValidateStudentCredentialsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI para el formulario de inicio de sesión.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Estado UI para el formulario de registro de estudiante.
 */
data class RegisterUiState(
    val dni: String = "",
    val studentCode: String = "",
    val university: String = "",
    val email: String = "",
    val password: String = "",
    val dniError: String? = null,
    val studentCodeError: String? = null,
    val universityError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel encargado de la lógica de autenticación, validación estricta de credenciales
 * estudiantiles y soporte de bypass para pruebas locales (Agente 3: Auth).
 *
 * @param authRepository Repositorio para persistencia y estado del estudiante.
 * @param validator Caso de uso para validación de formato de credenciales.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = MockAuthRepositoryImpl(),
    private val validator: ValidateStudentCredentialsUseCase = ValidateStudentCredentialsUseCase()
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private val _currentUser = MutableStateFlow<StudentUser?>(null)
    val currentUser: StateFlow<StudentUser?> = _currentUser.asStateFlow()

    // --- Manejadores de Login ---

    fun onLoginEmailChange(value: String) {
        _loginState.update { it.copy(email = value.trim(), emailError = null) }
    }

    fun onLoginPasswordChange(value: String) {
        _loginState.update { it.copy(password = value, passwordError = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val email = _loginState.value.email
        val password = _loginState.value.password

        val emailValidation = validator.validateUniversityEmail(email)
        val passwordValidation = validator.validatePassword(password)

        if (!emailValidation.isValid || !passwordValidation.isValid) {
            _loginState.update {
                it.copy(
                    emailError = emailValidation.errorMessage,
                    passwordError = passwordValidation.errorMessage
                )
            }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, errorMessage = null) }
            onSuccess()
            _loginState.update { it.copy(isLoading = false) }
        }
    }

    // --- Manejadores de Registro ---

    fun onRegisterDniChange(value: String) {
        if (value.length <= 8 && value.all { it.isDigit() }) {
            _registerState.update { it.copy(dni = value, dniError = null) }
        }
    }

    fun onRegisterStudentCodeChange(value: String) {
        _registerState.update { it.copy(studentCode = value.trim(), studentCodeError = null) }
    }

    fun onRegisterUniversityChange(value: String) {
        _registerState.update { it.copy(university = value.trim(), universityError = null) }
    }

    fun onRegisterEmailChange(value: String) {
        _registerState.update { it.copy(email = value.trim(), emailError = null) }
    }

    fun onRegisterPasswordChange(value: String) {
        _registerState.update { it.copy(password = value, passwordError = null) }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _registerState.value
        val dniVal = validator.validateDni(state.dni)
        val emailVal = validator.validateUniversityEmail(state.email)
        val passVal = validator.validatePassword(state.password)
        val isCodeValid = state.studentCode.isNotBlank()
        val isUniValid = state.university.isNotBlank()

        if (!dniVal.isValid || !isCodeValid || !isUniValid || !emailVal.isValid || !passVal.isValid) {
            _registerState.update {
                it.copy(
                    dniError = dniVal.errorMessage,
                    studentCodeError = if (!isCodeValid) "El código universitario es obligatorio" else null,
                    universityError = if (!isUniValid) "La universidad es obligatoria" else null,
                    emailError = emailVal.errorMessage,
                    passwordError = passVal.errorMessage
                )
            }
            return
        }

        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true, errorMessage = null) }
            _currentUser.value = StudentUser(
                id = "usr-${System.currentTimeMillis()}",
                fullName = "Estudiante ${state.university}",
                email = state.email,
                dni = state.dni,
                university = state.university,
                studentCode = state.studentCode,
                verificationStatus = VerificationStatus.PENDING_VERIFICATION
            )
            onSuccess()
            _registerState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Acceso directo para pruebas locales y desarrollo rápido (Bypass).
     * Configura el usuario en PENDING_VERIFICATION y ejecuta el callback de navegación.
     */
    fun bypass(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.updateVerificationStatus(VerificationStatus.PENDING_VERIFICATION)
            onSuccess()
        }
    }
}
