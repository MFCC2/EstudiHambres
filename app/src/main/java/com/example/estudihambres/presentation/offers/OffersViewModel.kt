package com.example.estudihambres.presentation.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.CuponidadWebOffersRepository
import com.example.estudihambres.domain.model.WebOffer
import com.example.estudihambres.domain.model.WebOfferCategory
import com.example.estudihambres.domain.repository.WebOffersRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Orden de la lista de ofertas. */
enum class OfferSort(val label: String) {
    RELEVANCE("Destacadas"),
    PRICE_ASC("Más baratas")
}

/**
 * Estado de la pantalla de Ofertas Web.
 *
 * @property offers Ofertas descargadas de la categoría activa.
 * @property visibleOffers Ofertas tras aplicar búsqueda, presupuesto y orden (lo que se pinta).
 */
data class OffersUiState(
    val sourceName: String = "",
    val categories: List<WebOfferCategory> = emptyList(),
    val selectedCategory: WebOfferCategory? = null,
    val offers: List<WebOffer> = emptyList(),
    val visibleOffers: List<WebOffer> = emptyList(),
    val query: String = "",
    val onlyBudget: Boolean = false,
    val sort: OfferSort = OfferSort.RELEVANCE,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val lastUpdatedMillis: Long? = null
) {
    companion object {
        /** Tope del filtro "económicas" pensado para presupuesto estudiantil. */
        const val BUDGET_LIMIT_SOLES = 30.0
    }
}

/**
 * ViewModel de Ofertas Web: descarga ofertas de restaurantes de una página externa mediante
 * scraping ([WebOffersRepository]) y aplica filtros locales sin volver a descargar.
 */
class OffersViewModel(
    private val repository: WebOffersRepository = CuponidadWebOffersRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OffersUiState(
            sourceName = repository.sourceName,
            categories = repository.categories,
            selectedCategory = repository.categories.firstOrNull()
        )
    )
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(forceRefresh = false)
    }

    fun onCategorySelected(category: WebOfferCategory) {
        if (category == _uiState.value.selectedCategory) return
        _uiState.update { it.copy(selectedCategory = category, offers = emptyList(), visibleOffers = emptyList()) }
        load(forceRefresh = false)
    }

    fun onRefresh() = load(forceRefresh = true)

    fun onRetry() = load(forceRefresh = true)

    fun onQueryChange(query: String) = updateFilters { it.copy(query = query.take(60)) }

    fun onToggleBudget() = updateFilters { it.copy(onlyBudget = !it.onlyBudget) }

    fun onSortChange(sort: OfferSort) = updateFilters { it.copy(sort = sort) }

    private fun load(forceRefresh: Boolean) {
        val category = _uiState.value.selectedCategory ?: return
        // Cancela la descarga anterior si el usuario cambia de categoría rápidamente
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.offers.isEmpty(),
                    isRefreshing = it.offers.isNotEmpty(),
                    errorMessage = null
                )
            }
            val result = repository.getOffers(category, forceRefresh)
            _uiState.update { current ->
                if (current.selectedCategory != category) return@update current
                result.fold(
                    onSuccess = { offers ->
                        applyFilters(
                            current.copy(
                                offers = offers,
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = null,
                                lastUpdatedMillis = System.currentTimeMillis()
                            )
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "No se pudieron cargar las ofertas."
                        )
                    }
                )
            }
        }
    }

    private fun updateFilters(change: (OffersUiState) -> OffersUiState) {
        _uiState.update { applyFilters(change(it)) }
    }

    companion object {
        /** Filtro puro (búsqueda + presupuesto + orden), expuesto para pruebas. */
        fun applyFilters(state: OffersUiState): OffersUiState {
            val terms = state.query.trim().lowercase().split(' ').filter { it.isNotBlank() }
            var list = state.offers.asSequence()
            if (terms.isNotEmpty()) {
                list = list.filter { offer ->
                    val haystack = "${offer.title} ${offer.merchant} ${offer.district}".lowercase()
                    terms.all { it in haystack }
                }
            }
            if (state.onlyBudget) {
                list = list.filter { (it.priceSoles ?: Double.MAX_VALUE) <= OffersUiState.BUDGET_LIMIT_SOLES }
            }
            val sorted = when (state.sort) {
                OfferSort.RELEVANCE -> list.toList()
                OfferSort.PRICE_ASC -> list.sortedBy { it.priceSoles ?: Double.MAX_VALUE }.toList()
            }
            return state.copy(visibleOffers = sorted)
        }
    }
}
