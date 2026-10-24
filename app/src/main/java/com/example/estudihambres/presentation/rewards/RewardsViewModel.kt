package com.example.estudihambres.presentation.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.repository.RewardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI enriquecido de la Tienda de StudiPuntos (Premios).
 */
data class RewardsUiState(
    val selectedTab: Int = 0, // 0: Catálogo, 1: Mis Vales, 2: Historial & Misiones
    val studiPuntos: Int = 2450,
    val allRewards: List<RewardItem> = emptyList(),
    val filteredRewards: List<RewardItem> = emptyList(),
    val selectedCategory: String = "Todos",
    val categories: List<String> = listOf("Todos", "Comida", "Cine & Ocio", "Música", "Educación", "Transporte"),
    val activeStreakText: String = "Racha activa: 0 beneficios",
    val conversionRateText: String = "Tasa de lealtad: 100 pts = S/ 1.00",
    val redeemedRewards: List<RedeemedReward> = emptyList(),
    val pointTransactions: List<PointTransaction> = emptyList(),
    val activeRedeemedReward: RedeemedReward? = null,
    val isScanningReceipt: Boolean = false,
    val showReceiptDialog: Boolean = false,
    val showReceiptScanner: Boolean = false,
    val showLegalDialog: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel de la Tienda de StudiPuntos.
 * Orquesta el catálogo oficial, la billetera inmutable con encadenamiento SHA-256,
 * el historial de vales canjeados y el registro de boletas electrónicas SUNAT.
 */
class RewardsViewModel(
    private val rewardsRepository: RewardsRepository = MockRewardsRepositoryImpl(),
    private val sessionManager: SessionManager? = SessionManager.getInstanceOrNull()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // 1. Saldo de puntos reactivo
        viewModelScope.launch {
            rewardsRepository.getUserStudiPuntos().collect { points ->
                _uiState.update { it.copy(studiPuntos = points) }
            }
        }

        // 2. Catálogo de recompensas verificado
        viewModelScope.launch {
            rewardsRepository.getAvailableRewards().collect { rewards ->
                _uiState.update { current ->
                    val filtered = if (current.selectedCategory == "Todos") {
                        rewards
                    } else {
                        rewards.filter { it.category.contains(current.selectedCategory, ignoreCase = true) }
                    }
                    current.copy(allRewards = rewards, filteredRewards = filtered)
                }
            }
        }

        // 3. Vales canjeados permanentes
        viewModelScope.launch {
            rewardsRepository.getRedeemedRewards().collect { list ->
                _uiState.update { it.copy(redeemedRewards = list) }
            }
        }

        // 4. Historial contable de movimientos (PointLedger)
        viewModelScope.launch {
            rewardsRepository.getPointTransactions().collect { txs ->
                _uiState.update { it.copy(pointTransactions = txs) }
            }
        }

        calculateRealStreak()
    }

    private fun calculateRealStreak() {
        val count = sessionManager?.getCouponsRedeemedCount() ?: 0
        val text = if (count == 1) "Racha activa: 1 beneficio canjeado" else "Racha activa: $count beneficios canjeados"
        _uiState.update { it.copy(activeStreakText = text) }
    }

    /**
     * Cambia de pestaña activa (0: Catálogo, 1: Mis Vales, 2: Historial).
     */
    fun onTabSelect(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    /**
     * Filtra el catálogo por categoría temática.
     */
    fun onCategorySelect(category: String) {
        _uiState.update { current ->
            val filtered = if (category == "Todos") {
                current.allRewards
            } else {
                current.allRewards.filter { it.category.contains(category, ignoreCase = true) }
            }
            current.copy(selectedCategory = category, filteredRewards = filtered)
        }
    }

    /**
     * Procesa la solicitud de canje de un premio.
     */
    fun onRedeemReward(reward: RewardItem) {
        viewModelScope.launch {
            when (val result = rewardsRepository.redeemReward(reward)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            activeRedeemedReward = result.data,
                            toastMessage = "🎉 ¡Premio canjeado con éxito! Código y voucher generados."
                        )
                    }
                    calculateRealStreak()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message ?: "Saldo insuficiente de StudiPuntos"
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    /**
     * Marca un vale como usado en caja o en el comercio asociado.
     */
    fun onMarkRewardAsUsed(rewardId: String) {
        viewModelScope.launch {
            val success = rewardsRepository.markRewardAsUsed(rewardId)
            if (success) {
                _uiState.update {
                    it.copy(toastMessage = "✅ Vale marcado como utilizado en tienda.")
                }
            }
        }
    }

    /**
     * Registra una boleta electrónica SUNAT con validación fiscal y anti-duplicidad.
     */
    fun onRegisterReceiptManual(ruc: String, serie: String, correlativo: String, amount: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningReceipt = true, showReceiptDialog = false, showReceiptScanner = false) }
            val now = System.currentTimeMillis()
            when (val result = rewardsRepository.registerReceipt(ruc, serie, correlativo, amount, now)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isScanningReceipt = false,
                            toastMessage = "🧾 ¡Boleta SUNAT auditada exitosamente! +80 StudiPuntos acreditados a tu billetera."
                        )
                    }
                    calculateRealStreak()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isScanningReceipt = false,
                            errorMessage = result.message ?: "La boleta no superó la validación fiscal."
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isScanningReceipt = false) }
                }
            }
        }
    }

    /**
     * Registra una boleta a partir del código QR normado por SUNAT.
     */
    fun onRegisterReceiptQr(qrRawText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningReceipt = true, showReceiptDialog = false, showReceiptScanner = false) }
            when (val result = rewardsRepository.registerReceiptQr(qrRawText)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isScanningReceipt = false,
                            toastMessage = "🧾 Firma digital QR SUNAT verificada. +80 StudiPuntos añadidos."
                        )
                    }
                    calculateRealStreak()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isScanningReceipt = false,
                            errorMessage = result.message ?: "QR no válido para acreditación."
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isScanningReceipt = false) }
                }
            }
        }
    }

    /**
     * Acreditación rápida asistida para testing institucional.
     */
    fun onScanReceiptDemo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningReceipt = true) }
            when (val result = rewardsRepository.earnPointsFromReceipt()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isScanningReceipt = false,
                            toastMessage = "🧾 Boleta verificada: +80 StudiPuntos añadidos a tu billetera."
                        )
                    }
                    calculateRealStreak()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isScanningReceipt = false, errorMessage = result.message) }
                }
                else -> {
                    _uiState.update { it.copy(isScanningReceipt = false) }
                }
            }
        }
    }

    fun openReceiptScanner() {
        _uiState.update { it.copy(showReceiptScanner = true, showReceiptDialog = false) }
    }

    fun dismissReceiptScanner() {
        _uiState.update { it.copy(showReceiptScanner = false) }
    }

    fun switchToManualReceipt() {
        _uiState.update { it.copy(showReceiptScanner = false, showReceiptDialog = true) }
    }

    fun switchToScannerReceipt() {
        _uiState.update { it.copy(showReceiptScanner = true, showReceiptDialog = false) }
    }

    fun openReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = true) }
    }

    fun dismissReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = false) }
    }

    fun openLegalDialog() {
        _uiState.update { it.copy(showLegalDialog = true) }
    }

    fun dismissLegalDialog() {
        _uiState.update { it.copy(showLegalDialog = false) }
    }

    fun dismissRedemptionDialog() {
        _uiState.update { it.copy(activeRedeemedReward = null) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
    }
}
