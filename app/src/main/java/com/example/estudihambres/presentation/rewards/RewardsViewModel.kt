package com.example.estudihambres.presentation.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.core.util.Resource
import com.example.estudihambres.data.repository.MockRewardsRepositoryImpl
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.repository.RewardsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI de la pantalla Tienda de StudiPuntos (Premios).
 */
data class RewardsUiState(
    val studiPuntos: Int = 2450,
    val allRewards: List<RewardItem> = emptyList(),
    val filteredRewards: List<RewardItem> = emptyList(),
    val selectedCategory: String = "Todos",
    val categories: List<String> = listOf("Todos", "Gaming", "Música", "Comida", "Cine", "Educación"),
    val activeStreakText: String = "Racha activa: 4 compras con descuento",
    val conversionRateText: String = "Tasa: 100 pts = S/ 1.00",
    val activeRedeemedReward: RedeemedReward? = null,
    val isScanningReceipt: Boolean = false,
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel encargado del catálogo de recompensas, saldo de StudiPuntos, canje y acreditación por boletas.
 *
 * @param rewardsRepository Repositorio de premios y billetera de puntos.
 */
class RewardsViewModel(
    private val rewardsRepository: RewardsRepository = MockRewardsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        loadRewardsAndPoints()
    }

    private fun loadRewardsAndPoints() {
        viewModelScope.launch {
            rewardsRepository.getUserStudiPuntos().collect { points ->
                _uiState.update { it.copy(studiPuntos = points) }
            }
        }

        viewModelScope.launch {
            rewardsRepository.getAvailableRewards().collect { rewards ->
                _uiState.update { current ->
                    val filtered = if (current.selectedCategory == "Todos") {
                        rewards
                    } else {
                        rewards.filter { it.category.equals(current.selectedCategory, ignoreCase = true) }
                    }
                    current.copy(allRewards = rewards, filteredRewards = filtered)
                }
            }
        }
    }

    /**
     * Filtra el catálogo por categoría temática.
     */
    fun onCategorySelect(category: String) {
        _uiState.update { current ->
            val filtered = if (category == "Todos") {
                current.allRewards
            } else {
                current.allRewards.filter { it.category.equals(category, ignoreCase = true) }
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
                            toastMessage = "🎉 ¡Premio canjeado con éxito! Código generado."
                        )
                    }
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
     * Acredita +80 StudiPuntos al escanear una boleta de compra.
     */
    fun onScanReceipt() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningReceipt = true) }
            delay(500) // Simulación de reconocimiento OCR de boleta
            when (val result = rewardsRepository.earnPointsFromReceipt()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isScanningReceipt = false,
                            toastMessage = "🧾 Boleta verificada: +80 StudiPuntos añadidos a tu billetera."
                        )
                    }
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

    /**
     * Cierra el modal de premio canjeado.
     */
    fun dismissRedemptionDialog() {
        _uiState.update { it.copy(activeRedeemedReward = null) }
    }

    /**
     * Limpia mensajes transitorios.
     */
    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
    }
}
