package com.example.estudihambres.presentation.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.ReceiptDraft
import com.example.estudihambres.domain.model.ReceiptSource
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.model.ScannedReceipt
import com.example.estudihambres.domain.repository.RewardsRepository
import com.example.estudihambres.domain.usecase.DetectReceiptEstablishmentUseCase
import com.example.estudihambres.domain.usecase.ValidateSunatReceiptUseCase
import com.example.estudihambres.presentation.rewards.receipt.SunatOutcome
import com.example.estudihambres.presentation.rewards.receipt.SunatOutcomeKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

/** Paso del flujo de registro de boletas: Escanear → Revisar → Verificar en SUNAT y acreditar. */
enum class ReceiptStep { SCAN, REVIEW, VERIFY }

/**
 * Estado UI de la Tienda de StudiPuntos (Premios).
 */
data class RewardsUiState(
    val selectedTab: Int = 0, // 0: Catálogo, 1: Escanear Boleta, 2: Mis Vales, 3: Historial
    val studiPuntos: Int = 0,
    val allRewards: List<RewardItem> = emptyList(),
    val filteredRewards: List<RewardItem> = emptyList(),
    val selectedCategory: String = "Todos",
    val categories: List<String> = listOf("Todos", "Comida", "Cine & Ocio", "Música", "Educación", "Transporte"),
    val activeStreakText: String = "Racha activa: 0 beneficios",
    val conversionRateText: String = "Tasa de lealtad: 100 pts = S/ 1.00",
    val redeemedRewards: List<RedeemedReward> = emptyList(),
    val pointTransactions: List<PointTransaction> = emptyList(),
    val activeRedeemedReward: RedeemedReward? = null,
    // Flujo de boletas
    val receiptStep: ReceiptStep = ReceiptStep.SCAN,
    val receiptDraft: ReceiptDraft? = null,
    val receiptFormError: String? = null,
    val sunatAttempt: Int = 0,
    val sunatOutcome: SunatOutcome? = null,
    val isScanningReceipt: Boolean = false, // true mientras se acredita la boleta
    val showLegalDialog: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel de la Tienda de StudiPuntos: catálogo, vales, historial del ledger y el flujo
 * guiado de registro de boletas electrónicas con corroboración en SUNAT.
 */
class RewardsViewModel(
    private val rewardsRepository: RewardsRepository = MockRewardsRepositoryImpl(),
    private val sessionManager: SessionManager? = SessionManager.getInstanceOrNull(),
    private val receiptValidator: ValidateSunatReceiptUseCase = ValidateSunatReceiptUseCase(),
    private val establishmentDetector: DetectReceiptEstablishmentUseCase = DetectReceiptEstablishmentUseCase(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    private var redeemJob: Job? = null
    private var receiptJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            rewardsRepository.getUserStudiPuntos().collect { points ->
                _uiState.update { it.copy(studiPuntos = points) }
            }
        }
        viewModelScope.launch {
            rewardsRepository.getAvailableRewards().collect { rewards ->
                _uiState.update { current ->
                    current.copy(allRewards = rewards, filteredRewards = filterByCategory(rewards, current.selectedCategory))
                }
            }
        }
        viewModelScope.launch {
            rewardsRepository.getRedeemedRewards().collect { list ->
                _uiState.update { it.copy(redeemedRewards = list) }
            }
        }
        viewModelScope.launch {
            rewardsRepository.getPointTransactions().collect { txs ->
                _uiState.update { it.copy(pointTransactions = txs) }
            }
        }
        calculateRealStreak()
    }

    private fun filterByCategory(rewards: List<RewardItem>, category: String): List<RewardItem> =
        if (category == "Todos") rewards else rewards.filter { it.category.contains(category, ignoreCase = true) }

    private fun calculateRealStreak() {
        val count = sessionManager?.getCouponsRedeemedCount() ?: 0
        val text = if (count == 1) "Racha activa: 1 beneficio canjeado" else "Racha activa: $count beneficios canjeados"
        _uiState.update { it.copy(activeStreakText = text) }
    }

    fun onTabSelect(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onCategorySelect(category: String) {
        _uiState.update { it.copy(selectedCategory = category, filteredRewards = filterByCategory(it.allRewards, category)) }
    }

    fun onRedeemReward(reward: RewardItem) {
        // Ignora toques repetidos mientras un canje sigue en curso (evita vales duplicados)
        if (redeemJob?.isActive == true) return
        redeemJob = viewModelScope.launch {
            when (val result = rewardsRepository.redeemReward(reward)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(activeRedeemedReward = result.data, toastMessage = "Premio canjeado. Tu vale está en \"Mis vales\".")
                    }
                    calculateRealStreak()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(errorMessage = result.message ?: "Saldo insuficiente de StudiPuntos")
                }
                else -> Unit
            }
        }
    }

    fun onMarkRewardAsUsed(rewardId: String) {
        viewModelScope.launch {
            if (rewardsRepository.markRewardAsUsed(rewardId)) {
                _uiState.update { it.copy(toastMessage = "Vale marcado como usado.") }
            }
        }
    }

    // ------------------------------------------------------------------
    // Flujo de boletas: Escanear → Revisar → Verificar en SUNAT → Acreditar
    // ------------------------------------------------------------------

    /** La cámara leyó un comprobante (QR u OCR): pasa a revisión con los datos precargados. */
    fun onReceiptScanned(scan: ScannedReceipt) {
        if (_uiState.value.receiptStep != ReceiptStep.SCAN) return
        val draft = ReceiptDraft(
            ruc = scan.ruc,
            serie = scan.serie,
            numero = scan.numero,
            amountText = scan.amount?.let { "%.2f".format(java.util.Locale.US, it) }.orEmpty(),
            issueDateMillis = scan.issueDateMillis ?: startOfDay(0),
            establishment = establishmentDetector(scan.ocrText, scan.ruc),
            source = scan.source,
            rawQr = scan.rawQr
        )
        _uiState.update { it.copy(receiptStep = ReceiptStep.REVIEW, receiptDraft = draft, receiptFormError = null, sunatOutcome = null) }
    }

    /** Ingreso manual sin cámara. */
    fun onStartManualReceipt() {
        _uiState.update {
            it.copy(
                selectedTab = 1,
                receiptStep = ReceiptStep.REVIEW,
                receiptDraft = ReceiptDraft(issueDateMillis = startOfDay(0)),
                receiptFormError = null,
                sunatOutcome = null
            )
        }
    }

    /** Edición de un campo en la revisión. Si cambian datos leídos del QR, ya no se usa el QR crudo. */
    fun onDraftChange(updated: ReceiptDraft) {
        _uiState.update { state ->
            val old = state.receiptDraft
            val edited = old != null && (old.ruc != updated.ruc || old.serie != updated.serie ||
                old.numero != updated.numero || old.amountText != updated.amountText ||
                old.issueDateMillis != updated.issueDateMillis)
            val establishment = if (old?.ruc != updated.ruc && updated.ruc.length == 11) {
                establishmentDetector(null, updated.ruc)
            } else {
                updated.establishment
            }
            state.copy(
                receiptDraft = updated.copy(
                    rawQr = if (edited) null else updated.rawQr,
                    establishment = establishment
                ),
                receiptFormError = null
            )
        }
    }

    fun onRescan() {
        _uiState.update {
            it.copy(receiptStep = ReceiptStep.SCAN, receiptDraft = null, receiptFormError = null, sunatOutcome = null)
        }
    }

    /** Valida localmente (formato, dígito verificador del RUC, importe, ventana de 72 h) y consulta SUNAT. */
    fun onVerifyWithSunat() {
        val draft = _uiState.value.receiptDraft ?: return
        val amount = draft.amount
        if (amount == null) {
            _uiState.update { it.copy(receiptFormError = "Escribe el importe total que figura en la boleta.") }
            return
        }
        val local = receiptValidator(
            ruc = draft.ruc,
            serie = draft.serie,
            correlativo = draft.numero,
            montoTotal = amount,
            fechaEmisionMillis = draft.issueDateMillis,
            tipoComprobante = draft.tipo
        )
        if (!local.isValid) {
            _uiState.update { it.copy(receiptFormError = local.rejectionReason ?: "Revisa los datos del comprobante.") }
            return
        }
        _uiState.update {
            it.copy(
                receiptStep = ReceiptStep.VERIFY,
                receiptFormError = null,
                sunatOutcome = null,
                sunatAttempt = it.sunatAttempt + 1
            )
        }
    }

    fun onSunatOutcome(attempt: Int, outcome: SunatOutcome) {
        _uiState.update {
            if (it.receiptStep == ReceiptStep.VERIFY && it.sunatAttempt == attempt) {
                it.copy(sunatOutcome = outcome)
            } else {
                it
            }
        }
    }

    fun onRetrySunat() {
        _uiState.update { it.copy(sunatOutcome = null, sunatAttempt = it.sunatAttempt + 1) }
    }

    fun onBackToReview() {
        _uiState.update { it.copy(receiptStep = ReceiptStep.REVIEW, sunatOutcome = null) }
    }

    /**
     * Acredita la boleta. Sólo se permite cuando SUNAT confirmó explícitamente que el comprobante
     * es válido; si no hubo respuesta, el estudiante debe reintentar la consulta.
     */
    fun onCreditReceipt() {
        val state = _uiState.value
        val draft = state.receiptDraft ?: return
        val outcome = state.sunatOutcome ?: return
        if (outcome.kind != SunatOutcomeKind.VALID) return
        if (receiptJob?.isActive == true) return

        receiptJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanningReceipt = true) }
            val result = if (draft.source == ReceiptSource.QR && draft.rawQr != null) {
                rewardsRepository.registerReceiptQr(draft.rawQr)
            } else {
                rewardsRepository.registerReceipt(
                    draft.ruc, draft.serie, draft.numero, draft.amount ?: 0.0, draft.issueDateMillis
                )
            }
            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isScanningReceipt = false,
                            receiptStep = ReceiptStep.SCAN,
                            receiptDraft = null,
                            sunatOutcome = null,
                            toastMessage = "+80 StudiPuntos acreditados por tu ${draft.tipoLabel.lowercase()}."
                        )
                    }
                    calculateRealStreak()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isScanningReceipt = false, errorMessage = result.message ?: "No se pudo acreditar la boleta.")
                }
                else -> _uiState.update { it.copy(isScanningReceipt = false) }
            }
        }
    }

    /** Inicio del día local, [daysAgo] días atrás (0 = hoy). */
    fun startOfDay(daysAgo: Int): Long = Calendar.getInstance().apply {
        timeInMillis = clock()
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun openLegalDialog() = _uiState.update { it.copy(showLegalDialog = true) }

    fun dismissLegalDialog() = _uiState.update { it.copy(showLegalDialog = false) }

    fun dismissRedemptionDialog() = _uiState.update { it.copy(activeRedeemedReward = null) }

    fun clearToast() = _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
}
