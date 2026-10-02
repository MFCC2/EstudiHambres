package com.example.estudihambres.presentation.verification

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.MockAuthRepositoryImpl
import com.example.estudihambres.domain.model.StudentCardOcrResult
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.repository.AuthRepository
import com.example.estudihambres.domain.usecase.ParseStudentCardOcrUseCase
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
    val errorMessage: String? = null
)

/**
 * ViewModel encargado de la orquestación del escaneo OCR mediante Google ML Kit Text Recognition,
 * análisis de palabras clave institucionales y gestión del estado PENDING_VERIFICATION (Agente 3).
 *
 * @param authRepository Repositorio para persistir el estado de verificación.
 * @param parser Caso de uso para inspeccionar las líneas reconocidas.
 */
class StudentVerificationViewModel(
    private val authRepository: AuthRepository = MockAuthRepositoryImpl(),
    private val parser: ParseStudentCardOcrUseCase = ParseStudentCardOcrUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
     * Simula el reconocimiento de un carnet universitario para entornos de testing y emuladores sin cámara física.
     */
    fun simulateCardScan(
        customText: String = "REPÚBLICA DEL PERÚ\nSUNEDU\nCARNET UNIVERSITARIO\nUNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS\nVIGENCIA 2026"
    ) {
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        val result = parser(customText)
        handleOcrParsed(result)
    }

    private fun handleOcrParsed(result: StudentCardOcrResult) {
        viewModelScope.launch {
            val newStatus = if (result.isVerified) {
                VerificationStatus.VERIFIED
            } else {
                VerificationStatus.REJECTED
            }
            authRepository.updateVerificationStatus(newStatus)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    ocrResult = result,
                    verificationStatus = newStatus
                )
            }
        }
    }

    /**
     * Permite confirmar y aprobar la verificación manualmente si el OCR detectó texto pero la iluminación
     * impidió identificar automáticamente todos los sellos oficiales.
     */
    fun confirmManualVerification(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.updateVerificationStatus(VerificationStatus.VERIFIED)
            _uiState.update {
                it.copy(
                    verificationStatus = VerificationStatus.VERIFIED,
                    ocrResult = it.ocrResult?.copy(isVerified = true) ?: StudentCardOcrResult(
                        isVerified = true,
                        hasSuneduKeyword = true,
                        hasUniversityKeyword = true,
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
}
