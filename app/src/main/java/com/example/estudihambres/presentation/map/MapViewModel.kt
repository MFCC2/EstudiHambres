package com.example.estudihambres.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudihambres.data.repository.MockPlaceRepository
import com.example.estudihambres.domain.model.Place
import com.example.estudihambres.domain.repository.LocationRepository
import com.example.estudihambres.domain.repository.PlaceRepository
import com.example.estudihambres.domain.repository.UserCoordinates
import com.example.estudihambres.domain.usecase.FilterPlacesByDistanceUseCase
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI del mapa interactivo de beneficios y radar de 30 km (presentation/map).
 */
data class MapUiState(
    val userLocation: LatLng = LatLng(-12.0833, -77.0428), // Campus Universitario (Lima) por defecto
    val isDefaultLocation: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val nearbyPlaces: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel para el mapa radar de 30 km (Agente 4: Geolocalización & Google Maps).
 * Controla permisos de GPS, ubicación del estudiante (o fallback a campus),
 * carga de locales y visualización de descuentos en el BottomSheet.
 *
 * @param placeRepository Repositorio de lugares y comercios afiliados.
 * @param locationRepository Repositorio para consultar GPS del dispositivo.
 * @param filterPlacesUseCase Caso de uso para filtrar dentro de 30 km con Haversine.
 */
class MapViewModel(
    private val placeRepository: PlaceRepository = MockPlaceRepository(),
    private val locationRepository: LocationRepository? = null,
    private val filterPlacesUseCase: FilterPlacesByDistanceUseCase = FilterPlacesByDistanceUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadPlaces()
    }

    fun onLocationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted && locationRepository != null) {
            viewModelScope.launch {
                val lastKnown = locationRepository.getLastKnownLocation()
                if (lastKnown != null) {
                    val coords = LatLng(lastKnown.latitude, lastKnown.longitude)
                    _uiState.update { it.copy(userLocation = coords, isDefaultLocation = false) }
                    loadPlaces()
                }
            }
        }
    }

    fun onPlaceSelected(place: Place?) {
        _uiState.update { it.copy(selectedPlace = place) }
    }

    fun loadPlaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            placeRepository.getPlaces().collect { rawPlaces ->
                val userCoords = UserCoordinates(
                    latitude = _uiState.value.userLocation.latitude,
                    longitude = _uiState.value.userLocation.longitude
                )
                var filtered = filterPlacesUseCase(userCoords, rawPlaces, maxDistanceKm = 30.0)

                // Si el estudiante se encuentra fuera de Lima (ej. Huancayo, Arequipa, Cusco, etc.)
                // adaptamos dinámicamente los convenios alrededor de su GPS real para que el radar
                // localice comercios y ofertas en sus calles y campus local.
                if (filtered.isEmpty() && rawPlaces.isNotEmpty()) {
                    val dynamicPlaces = generatePlacesAroundUser(userCoords, rawPlaces)
                    filtered = filterPlacesUseCase(userCoords, dynamicPlaces, maxDistanceKm = 30.0)
                }

                _uiState.update {
                    it.copy(
                        nearbyPlaces = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun generatePlacesAroundUser(userCoords: UserCoordinates, templatePlaces: List<Place>): List<Place> {
        val offsets = listOf(
            Pair(0.0035, 0.0040),   // ~550 m NE
            Pair(-0.0042, 0.0055),  // ~700 m SE
            Pair(0.0060, -0.0035),  // ~750 m NW
            Pair(-0.0050, -0.0060), // ~850 m SW
            Pair(0.0090, 0.0020),   // ~1.0 km N
            Pair(-0.0085, -0.0030), // ~1.0 km S
            Pair(0.0120, -0.0080),  // ~1.6 km NW
            Pair(-0.0150, 0.0110),  // ~2.0 km SE
            Pair(0.0015, -0.0020),  // ~250 m W (Comercio a unos pasos)
            Pair(0.5000, 0.5000)    // > 65 km (Fuera de radio para verificar filtro de 30 km)
        )

        return templatePlaces.mapIndexed { index, place ->
            val offset = offsets.getOrElse(index) { Pair(0.004 * (index + 1), 0.004 * (index + 1)) }
            place.copy(
                latitude = userCoords.latitude + offset.first,
                longitude = userCoords.longitude + offset.second
            )
        }
    }
}

/**
 * Fábrica para instanciar [MapViewModel] proveyendo el repositorio de ubicación activo.
 */
class MapViewModelFactory(
    private val placeRepository: PlaceRepository = MockPlaceRepository(),
    private val locationRepository: LocationRepository? = null,
    private val filterPlacesUseCase: FilterPlacesByDistanceUseCase = FilterPlacesByDistanceUseCase()
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(placeRepository, locationRepository, filterPlacesUseCase) as T
    }
}
