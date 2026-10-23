package com.example.estudihambres.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.MockAuthRepositoryImpl
import com.example.estudihambres.domain.model.StudentCardOcrResult
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
 * Modo de registro estudiantil.
 */
enum class RegistrationMode {
    SCANNER, // Escaneo directo con cámara o galería (Recomendado)
    MANUAL   // Ingreso manual si no cuenta con carnet a la mano
}

/**
 * Estado UI para el formulario de registro de estudiante.
 */
data class RegisterUiState(
    val fullName: String = "",
    val dni: String = "",
    val studentCode: String = "",
    val university: String = "",
    val faculty: String = "",
    val career: String = "",
    val validityYear: String = "2026",
    val studentStatus: String = "Estudiante Regular (Vigente)",
    val email: String = "",
    val password: String = "",
    val dniError: String? = null,
    val studentCodeError: String? = null,
    val universityError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationMode: RegistrationMode = RegistrationMode.SCANNER,
    val isCardScanned: Boolean = false,
    val scannedCardResult: StudentCardOcrResult? = null
)

/**
 * ViewModel encargado de la lógica de autenticación, validación estricta de credenciales
 * estudiantiles y flujo dual de registro (Escaneo directo de carnet o ingreso manual) (Agente 3).
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

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
            }
        }
    }

    // --- Manejadores de Login ---

    fun onLoginEmailChange(value: String) {
        _loginState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onLoginPasswordChange(value: String) {
        _loginState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    /**
     * Inicia sesión validando formato, existencia de la cuenta y correspondencia de contraseña.
     */
    fun login(onSuccess: () -> Unit) {
        val email = _loginState.value.email.trim()
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
            val result = authRepository.authenticate(email, password)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _loginState.update { it.copy(isLoading = false, errorMessage = null) }
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message
                    ?: "Credenciales inválidas. Por favor verifica tu contraseña."
                _loginState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    // --- Manejadores de Registro ---

    fun setRegistrationMode(mode: RegistrationMode) {
        _registerState.update { it.copy(registrationMode = mode, errorMessage = null) }
    }

    fun onRegisterFullNameChange(value: String) {
        _registerState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onRegisterDniChange(value: String) {
        if (value.length <= 8 && value.all { it.isDigit() }) {
            val error = if (value.length == 8) null else null
            _registerState.update { it.copy(dni = value, dniError = error, errorMessage = null) }
        }
    }

    fun onRegisterStudentCodeChange(value: String) {
        _registerState.update { it.copy(studentCode = value, studentCodeError = null, errorMessage = null) }
    }

    fun onRegisterUniversityChange(value: String) {
        _registerState.update { it.copy(university = value, universityError = null, errorMessage = null) }
    }

    fun onRegisterFacultyChange(value: String) {
        _registerState.update { it.copy(faculty = value, errorMessage = null) }
    }

    fun onRegisterCareerChange(value: String) {
        _registerState.update { it.copy(career = value, errorMessage = null) }
    }

    fun onRegisterEmailChange(value: String) {
        _registerState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun onRegisterPasswordChange(value: String) {
        _registerState.update { it.copy(password = value, passwordError = null, errorMessage = null) }
    }

    /**
     * Aplica los datos reconocidos automáticamente tras escanear el carnet oficial con OCR.
     * Esto evita que el estudiante cometa errores al escribir su DNI o código manualmente.
     */
    fun onCarnetScannedInRegister(result: StudentCardOcrResult) {
        _registerState.update {
            it.copy(
                isCardScanned = true,
                scannedCardResult = result,
                dni = result.studentDni ?: it.dni,
                studentCode = result.studentCode ?: it.studentCode,
                university = result.universityName ?: it.university,
                fullName = result.studentName ?: it.fullName,
                faculty = result.faculty ?: it.faculty,
                career = result.career ?: it.career,
                validityYear = result.validityYearDetected ?: it.validityYear,
                studentStatus = result.studentStatus,
                dniError = null,
                studentCodeError = null,
                universityError = null,
                errorMessage = null
            )
        }
    }

    /**
     * Registra al nuevo estudiante guardando su perfil y credenciales.
     * Si escaneó su carnet previamente, la cuenta queda automáticamente VERIFIED con +500 StudiPuntos.
     * Si ingresó datos manualmente, queda en PENDING_VERIFICATION para escanearlo más adelante.
     */
    fun register(onSuccess: () -> Unit) {
        val state = _registerState.value
        val cleanDni = state.dni.trim()
        val cleanCode = state.studentCode.trim()
        val cleanUni = state.university.trim()
        val cleanEmail = state.email.trim()
        val cleanPass = state.password
        val cleanName = state.fullName.trim()
        val cleanCareer = state.career.trim()
        val cleanFaculty = state.faculty.trim()

        val dniVal = validator.validateDni(cleanDni)
        val emailVal = validator.validateUniversityEmail(cleanEmail)
        val passVal = validator.validatePassword(cleanPass)
        val isCodeValid = cleanCode.isNotBlank() || state.isCardScanned
        val isUniValid = cleanUni.isNotBlank()

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
            val verificationStatus = if (state.isCardScanned && state.scannedCardResult?.isVerified == true) {
                VerificationStatus.VERIFIED
            } else {
                VerificationStatus.PENDING_VERIFICATION
            }

            val newUser = StudentUser(
                id = "usr-${System.currentTimeMillis()}",
                fullName = cleanName.ifBlank { "Estudiante $cleanUni" },
                email = cleanEmail,
                dni = cleanDni,
                university = cleanUni,
                studentCode = cleanCode.ifBlank { cleanDni },
                verificationStatus = verificationStatus,
                career = cleanCareer.ifBlank { "Ingeniería de Sistemas" },
                isDev = false,
                scannedCardDni = if (state.isCardScanned) cleanDni else null,
                faculty = cleanFaculty.ifBlank { "Facultad Universitaria" },
                validityYear = state.validityYear,
                studentStatus = state.studentStatus
            )

            val result = authRepository.register(newUser, cleanPass)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                _registerState.update { it.copy(isLoading = false, errorMessage = null) }
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error al registrar la cuenta."
                _registerState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    /**
     * Acceso directo para pruebas locales y desarrollo rápido (Bypass Dev).
     */
    fun bypass(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val devUser = StudentUser(
                id = "usr-dev-mode",
                fullName = "Modo Desarrollador (Vista Genérica)",
                email = "dev@estudihambres.pe",
                dni = "00000000",
                university = "EstudiHambres Sandbox (Dev)",
                studentCode = "DEV-999",
                verificationStatus = VerificationStatus.PENDING_VERIFICATION,
                career = "Ingeniería de Software / QA",
                isDev = true
            )
            authRepository.saveSession(devUser)
            _currentUser.value = devUser
            onSuccess()
        }
    }
}
