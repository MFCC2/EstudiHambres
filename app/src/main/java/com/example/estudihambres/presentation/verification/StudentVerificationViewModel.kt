package com.example.estudihambres.presentation.verification

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.FirebaseAuthRepositoryImpl
import com.example.estudihambres.domain.model.StudentCardOcrResult
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.repository.AuthRepository
import com.example.estudihambres.domain.usecase.ParseStudentCardOcrUseCase
import com.example.estudihambres.domain.usecase.ValidateStudentCredentialsUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI para la pantalla de verificación de carnet universitario.
 */
data class VerificationUiState(
    val isProcessing: Boolean = false,
    val ocrResult: StudentCardOcrResult? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val errorMessage: String? = null,
    val showManualInputSheet: Boolean = false
)

/**
 * ViewModel encargado de la orquestación del escaneo OCR mediante Google ML Kit Text Recognition,
 * análisis de los 9 campos institucionales (SUNEDU, código, DNI, universidad, nombres, facultad,
 * carrera, sello y vigencia) y soporte de ingreso manual con validación estricta (Agente 3).
 *
 * @param authRepository Repositorio para persistir el estado de verificación.
 * @param parser Caso de uso para inspeccionar las líneas reconocidas.
 * @param validator Caso de uso para validar formato de DNI y credenciales.
 */
class StudentVerificationViewModel(
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val parser: ParseStudentCardOcrUseCase = ParseStudentCardOcrUseCase(),
    private val validator: ValidateStudentCredentialsUseCase = ValidateStudentCredentialsUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        // Cargar estado inicial del usuario activo
        val session = com.example.estudihambres.core.util.SessionManager.getInstanceOrNull()
        val user = session?.getUserSession()
        if (user != null) {
            _uiState.update { it.copy(verificationStatus = user.verificationStatus) }
        }
    }

    /**
     * Procesa un [Bitmap] capturado de la cámara o galería utilizando Google ML Kit Text Recognition.
     */
    fun processImageBitmap(bitmap: Bitmap, onComplete: () -> Unit = {}) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = parser(visionText.text)
                    handleOcrParsed(result)
                    onComplete()
                }
                .addOnFailureListener { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.localizedMessage ?: "Error al procesar la imagen con ML Kit"
                        )
                    }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isProcessing = false, errorMessage = e.localizedMessage ?: "Error inesperado")
            }
        }
    }

    /**
     * Simula el reconocimiento de un carnet universitario oficial SUNEDU para pruebas y emuladores.
     */
    fun simulateCardScan(
        customText: String = """
            República del Perú
            Superintendencia Nacional de Educación Superior Universitaria
            UNIVERSIDAD CONTINENTAL
            Código: 72945602
            DNI: 72945602
            Apellidos: CALLAÑAUPA CJUIRO
            Nombres: MANUEL FABRIZIO
            Facultad: INGENIERÍA
            Carrera: ING. DE SISTEMAS E INFORMÁTICA
            Expira 23 08 26
            CARNÉ UNIVERSITARIO 2026
        """.trimIndent()
    ) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        val result = parser(customText)
        handleOcrParsed(result)
    }

    /**
     * Procesa una imagen de ML Kit proveniente de CameraX ImageAnalysis en tiempo real.
     */
    fun processInputImage(inputImage: InputImage, onVerified: (StudentCardOcrResult) -> Unit = {}) {
        if (_uiState.value.verificationStatus == VerificationStatus.VERIFIED) return
        try {
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    if (visionText.text.isNotBlank()) {
                        val result = parser(visionText.text)
                        if (result.isVerified) {
                            handleOcrParsed(result)
                            onVerified(result)
                        } else if (_uiState.value.ocrResult == null || !_uiState.value.ocrResult!!.isVerified) {
                            _uiState.update { it.copy(ocrResult = result) }
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignorar fallas transitorias de frame
                }
        } catch (_: Exception) {}
    }

    /**
     * Reinicia el estado de verificación para permitir un nuevo escaneo con la cámara.
     */
    fun resetVerification() {
        _uiState.update {
            it.copy(
                isProcessing = false,
                ocrResult = null,
                verificationStatus = VerificationStatus.UNVERIFIED,
                errorMessage = null
            )
        }
    }

    fun onCardScannedSuccessfully(result: StudentCardOcrResult) {
        handleOcrParsed(result)
    }

    fun toggleManualInputSheet(show: Boolean) {
        _uiState.update { it.copy(showManualInputSheet = show) }
    }

    /**
     * Permite ingresar los datos manualmente cuando el estudiante no cuenta con su carnet físico.
     * Valida obligatoriamente los 8 dígitos del DNI y registra el perfil en PENDING_VERIFICATION.
     */
    fun submitManualStudentData(
        fullName: String,
        dni: String,
        studentCode: String,
        university: String,
        faculty: String,
        career: String,
        validityYear: String,
        onSuccess: () -> Unit
    ) {
        val dniValidation = validator.validateDni(dni)
        if (!dniValidation.isValid) {
            _uiState.update { it.copy(errorMessage = dniValidation.errorMessage) }
            return
        }

        viewModelScope.launch {
            val session = com.example.estudihambres.core.util.SessionManager.getInstanceOrNull()
            session?.let { mgr ->
                val current = mgr.getUserSession()
                val updated = (current ?: com.example.estudihambres.domain.model.StudentUser(
                    id = "usr-${System.currentTimeMillis()}",
                    fullName = fullName,
                    email = "alumno@universidad.edu.pe",
                    dni = dni,
                    university = university,
                    studentCode = studentCode
                )).copy(
                    fullName = fullName.ifBlank { current?.fullName ?: "Estudiante Universitario" },
                    dni = dni,
                    studentCode = studentCode.ifBlank { current?.studentCode ?: "" },
                    university = university.ifBlank { current?.university ?: "Universidad Nacional" },
                    faculty = faculty.ifBlank { current?.faculty ?: "Facultad General" },
                    career = career.ifBlank { current?.career ?: "Carrera Profesional" },
                    validityYear = validityYear.ifBlank { "2026" },
                    studentStatus = "Pendiente de Validación Física",
                    verificationStatus = VerificationStatus.PENDING_VERIFICATION
                )
                mgr.saveUserSession(updated)
            }

            val mockResult = StudentCardOcrResult(
                isVerified = false,
                hasSuneduKeyword = true,
                hasUniversityKeyword = true,
                hasSuneduSeal = false,
                suneduSealDescription = "Ingreso manual: Validación pendiente de verificación con carnet físico.",
                validityYearDetected = validityYear.ifBlank { "2026" },
                universityName = university,
                studentName = fullName,
                studentDni = dni,
                studentCode = studentCode,
                career = career,
                faculty = faculty,
                studentStatus = "Ingresado Manualmente (Pendiente)",
                isVigente = true,
                rawText = "Ingreso manual de estudiante"
            )

            _uiState.update {
                it.copy(
                    ocrResult = mockResult,
                    verificationStatus = VerificationStatus.PENDING_VERIFICATION,
                    showManualInputSheet = false,
                    errorMessage = null
                )
            }
            onSuccess()
        }
    }

    private fun handleOcrParsed(result: StudentCardOcrResult) {
        viewModelScope.launch {
            val session = com.example.estudihambres.core.util.SessionManager.getInstanceOrNull()

            if (result.isVerified) {
                // Validación estricta 1 carnet = 1 cuenta (reconciliando DNI si estaba en pendiente)
                if (session != null) {
                    val (isEligible, errorReason) = session.validateCardScanningEligibility(result.studentDni)
                    if (!isEligible) {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                ocrResult = result,
                                verificationStatus = VerificationStatus.REJECTED,
                                errorMessage = errorReason ?: "Solo puedes vincular un único carnet universitario a tu cuenta."
                            )
                        }
                        return@launch
                    }
                }

                val newStatus = VerificationStatus.VERIFIED
                authRepository.updateVerificationStatus(newStatus)

                session?.let { mgr ->
                    mgr.linkScannedCard(result.studentDni)
                    val user = mgr.getUserSession()
                    if (user != null) {
                        val updated = user.copy(
                            fullName = if (!result.studentName.isNullOrBlank()) result.studentName else user.fullName,
                            dni = result.studentDni ?: user.dni,
                            studentCode = result.studentCode ?: user.studentCode,
                            university = result.universityName ?: user.university,
                            faculty = result.faculty ?: user.faculty,
                            career = result.career ?: user.career,
                            validityYear = result.validityYearDetected ?: user.validityYear,
                            studentStatus = result.studentStatus,
                            verificationStatus = VerificationStatus.VERIFIED,
                            scannedCardDni = result.studentDni ?: user.dni
                        )
                        mgr.saveUserSession(updated)
                    } else {
                        mgr.updateVerificationStatus(VerificationStatus.VERIFIED)
                    }
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        ocrResult = result,
                        verificationStatus = newStatus,
                        errorMessage = null
                    )
                }
            } else {
                val newStatus = VerificationStatus.REJECTED
                authRepository.updateVerificationStatus(newStatus)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        ocrResult = result,
                        verificationStatus = newStatus,
                        errorMessage = result.rejectionReason ?: "El documento escaneado no contiene los sellos oficiales de SUNEDU o la vigencia requerida."
                    )
                }
            }
        }
    }

    /**
     * Permite confirmar y aprobar la verificación manualmente si el OCR detectó texto pero la iluminación
     * impidió identificar automáticamente todos los sellos oficiales.
     */
    fun confirmManualVerification(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val session = com.example.estudihambres.core.util.SessionManager.getInstanceOrNull()
            if (session != null) {
                val (isEligible, errorReason) = session.validateCardScanningEligibility(null)
                if (!isEligible) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = errorReason ?: "No puedes vincular otro carnet universitario."
                        )
                    }
                    return@launch
                }
                session.linkScannedCard(null)
            }

            authRepository.updateVerificationStatus(VerificationStatus.VERIFIED)
            _uiState.update {
                it.copy(
                    verificationStatus = VerificationStatus.VERIFIED,
                    errorMessage = null,
                    ocrResult = it.ocrResult?.copy(isVerified = true) ?: StudentCardOcrResult(
                        isVerified = true,
                        hasSuneduKeyword = true,
                        hasUniversityKeyword = true,
                        hasSuneduSeal = true,
                        validityYearDetected = "2026",
                        universityName = "Universidad Continental",
                        rawText = "Validación de carnet universitario confirmada"
                    )
                )
            }
            onSuccess()
        }
    }

    /**
     * Ocurre cuando el estudiante decide omitir el paso temporalmente.
     * Asigna el estado obligatorio PENDING_VERIFICATION sin bloquear su acceso a la app.
     */
    fun skipVerification(onNavigateToHome: () -> Unit) {
        viewModelScope.launch {
            authRepository.updateVerificationStatus(VerificationStatus.PENDING_VERIFICATION)
            _uiState.update { it.copy(verificationStatus = VerificationStatus.PENDING_VERIFICATION) }
            onNavigateToHome()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
