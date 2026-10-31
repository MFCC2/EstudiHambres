package com.example.estudihambres.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.FirebaseAuthRepositoryImpl
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
/**
 * Estado UI del Dashboard principal de promociones y beneficios (presentation/home).
 */
data class HomeUiState(
    val studentName: String = "Estudiante Universitario",
    val university: String = "Universidad Continental",
    val career: String = "Ingeniería de Software · Ciclo 2025-I",
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING_VERIFICATION,
    val accumulatedSavings: Double = 142.50,
    val studiPuntos: Int = 2450,
    val searchQuery: String = "",
    val selectedCategory: String = "Todas",
    val categories: List<String> = listOf("Todas", "🔥 Destacados", "🍕 Comida", "💻 Software", "🎬 Ocio & Cine", "📚 Educación", "🚌 Transporte"),
    val filteredPromotions: List<Promotion> = emptyList(),
    val popularPromotions: List<Promotion> = emptyList(),
    val nearbyPromotions: List<Promotion> = emptyList(),
    val digitalPromotions: List<Promotion> = emptyList(),
    val isDev: Boolean = false,
    val isLoading: Boolean = false,
    val profilePhotoPath: String? = null
)

/**
 * ViewModel del Dashboard de EstudiHambres (Agente 2: UI/UX & Agente 1: Arquitectura).
 * Maneja el catálogo de promociones, búsqueda reactiva, carruseles temáticos y cálculo de ahorros.
 *
 * @param promotionRepository Repositorio de promociones y cupones.
 * @param authRepository Repositorio de sesión para nombre y estado de verificación del estudiante.
 */
class HomeViewModel(
    private val promotionRepository: PromotionRepository = MockPromotionRepositoryImpl(),
    private val authRepository: AuthRepository = FirebaseAuthRepositoryImpl(),
    private val sessionManager: com.example.estudihambres.core.util.SessionManager? = com.example.estudihambres.core.util.SessionManager.getInstanceOrNull()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val allPromotions = MutableStateFlow<List<Promotion>>(emptyList())

    init {
        loadData()
    }

    fun refreshSavings() {
        val savings = sessionManager?.getAccumulatedSavings() ?: 142.50
        val points = sessionManager?.getStudiPuntos() ?: 2450
        _uiState.update { it.copy(accumulatedSavings = savings, studiPuntos = points) }
    }

    private fun loadData() {
        refreshSavings()
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                val isDevMode = user?.isDev ?: false
                _uiState.update {
                    it.copy(
                        isDev = isDevMode,
                        studentName = if (isDevMode) "Modo Desarrollador (Vista Genérica)" else (user?.fullName?.ifBlank { "Estudiante Universitario" } ?: "Estudiante Universitario"),
                        university = if (isDevMode) "Entorno Sandbox / Testing" else (user?.university?.ifBlank { "Universidad Continental" } ?: "Universidad Continental"),
                        career = if (isDevMode) "Pruebas de Integración y Validación OCR" else if (!user?.studentCode.isNullOrBlank()) "${user.university} · Cód. ${user.studentCode}" else "Ingeniería de Software · Ciclo 2025-I",
                        verificationStatus = user?.verificationStatus ?: VerificationStatus.PENDING_VERIFICATION,
                        profilePhotoPath = user?.profilePhotoPath,
                        studiPuntos = user?.studiPuntos ?: sessionManager?.getStudiPuntos() ?: 2450
                    )
                }
            }
        }

        viewModelScope.launch {
            promotionRepository.getPromotions().collect { list ->
                allPromotions.value = list
                val popular = list.filter { it.isFeatured || it.id in listOf("p-02", "p-07", "p-03", "p-01", "p-10", "p-11", "p-12") }
                val nearby = list.filter { it.category in listOf("Comida", "Entretenimiento", "Educación") || it.distanceText.contains("m") }
                val digital = list.filter { it.category == "Herramientas digitales" }

                _uiState.update {
                    it.copy(
                        popularPromotions = popular,
                        nearbyPromotions = nearby,
                        digitalPromotions = digital
                    )
                }
                applyFilters()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                selectedCategory = if (query.isNotBlank()) "Todas" else it.selectedCategory
            )
        }
        applyFilters()
    }

    fun onCategorySelect(category: String) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                searchQuery = if (category != "Todas") "" else it.searchQuery
            )
        }
    }

    fun onQuickTagSelect(tag: String) {
        val currentTag = _uiState.value.searchQuery
        if (currentTag.equals(tag, ignoreCase = true)) {
            _uiState.update { it.copy(searchQuery = "", selectedCategory = "Todas") }
        } else {
            _uiState.update { it.copy(searchQuery = tag, selectedCategory = "Todas") }
        }
        applyFilters()
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val category = _uiState.value.selectedCategory

        val filtered = allPromotions.value.filter { promo ->
            val matchesCategory = when {
                category == "Todas" -> true
                category.contains("Destacados", ignoreCase = true) -> promo.isFeatured
                category.contains("Comida", ignoreCase = true) -> promo.category.equals("Comida", ignoreCase = true)
                category.contains("Software", ignoreCase = true) -> promo.category.equals("Herramientas digitales", ignoreCase = true)
                category.contains("Transporte", ignoreCase = true) -> promo.category.equals("Transporte", ignoreCase = true)
                category.contains("Cine", ignoreCase = true) || category.contains("Ocio", ignoreCase = true) -> promo.category.equals("Entretenimiento", ignoreCase = true)
                category.contains("Educación", ignoreCase = true) || category.contains("Libros", ignoreCase = true) -> promo.category.equals("Educación", ignoreCase = true)
                else -> promo.category.contains(category, ignoreCase = true)
            }
            val matchesQuery = query.isEmpty() ||
                    promo.title.lowercase().contains(query) ||
                    promo.partnerName.lowercase().contains(query) ||
                    promo.description.lowercase().contains(query) ||
                    promo.category.lowercase().contains(query)
            matchesCategory && matchesQuery
        }

        _uiState.update { it.copy(filteredPromotions = filtered) }
    }
}
