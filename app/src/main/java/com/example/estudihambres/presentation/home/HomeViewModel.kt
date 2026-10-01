package com.example.estudihambres.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.MockAuthRepositoryImpl
import com.example.estudihambres.data.repository.MockPromotionRepositoryImpl
import com.example.estudihambres.domain.model.Promotion
import com.example.estudihambres.domain.model.StudentUser
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.repository.AuthRepository
import com.example.estudihambres.domain.repository.PromotionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI del Dashboard principal de promociones y beneficios (presentation/home).
 */
data class HomeUiState(
    val studentName: String = "Estudiante Universitario",
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING_VERIFICATION,
    val accumulatedSavings: Double = 148.50,
    val searchQuery: String = "",
    val selectedCategory: String = "Todas",
    val categories: List<String> = listOf("Todas", "Comida", "Herramientas digitales", "Transporte"),
    val filteredPromotions: List<Promotion> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel del Dashboard de CampusPass (Agente 2: UI/UX & Agente 1: Arquitectura).
 * Maneja el catálogo de promociones, búsqueda reactiva, filtrado por categorías y cálculo de ahorros.
 *
 * @param promotionRepository Repositorio de promociones y cupones.
 * @param authRepository Repositorio de sesión para nombre y estado de verificación del estudiante.
 */
class HomeViewModel(
    private val promotionRepository: PromotionRepository = MockPromotionRepositoryImpl(),
    private val authRepository: AuthRepository = MockAuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val allPromotions = MutableStateFlow<List<Promotion>>(emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.update {
                    it.copy(
                        studentName = user?.fullName ?: "Estudiante Universitario",
                        verificationStatus = user?.verificationStatus ?: VerificationStatus.PENDING_VERIFICATION
                    )
                }
            }
        }

        viewModelScope.launch {
            promotionRepository.getPromotions().collect { list ->
                allPromotions.value = list
                applyFilters()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onCategorySelect(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val category = _uiState.value.selectedCategory

        val filtered = allPromotions.value.filter { promo ->
            val matchesCategory = category == "Todas" || promo.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isEmpty() ||
                    promo.title.lowercase().contains(query) ||
                    promo.partnerName.lowercase().contains(query) ||
                    promo.description.lowercase().contains(query)
            matchesCategory && matchesQuery
        }

        _uiState.update { it.copy(filteredPromotions = filtered) }
    }
}
