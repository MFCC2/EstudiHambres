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
 * Centros universitarios principales para alternancia rápida del mapa.
 */
enum class CampusLocation(val displayName: String, val shortName: String, val coordinates: LatLng) {
    CUSCO(
        displayName = "Cusco (Campus Continental / UNSAAC)",
        shortName = "🏛️ Cusco",
        coordinates = LatLng(-13.5230, -71.9480)
    ),
    LIMA(
        displayName = "Lima (Campus UNMSM / PUCP)",
        shortName = "🏛️ Lima",
        coordinates = LatLng(-12.0833, -77.0428)
    )
}

/**
 * Estado UI del mapa interactivo de beneficios y radar de hasta 30 km (presentation/map).
 */
data class MapUiState(
    val userLocation: LatLng = LatLng(-13.5230, -71.9480), // Real Plaza Cusco / Av. de la Cultura por defecto
    val isDefaultLocation: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val nearbyPlaces: List<Place> = emptyList(),
    val selectedPlace: Place? = null,
    val selectedRadiusKm: Double = 30.0,
    val selectedCampus: CampusLocation = CampusLocation.CUSCO,
    val searchQuery: String = "",
    val selectedCategory: String = "Todos",
    val isLoading: Boolean = false
)

/**
 * ViewModel para el mapa radar interactivo (Agente 4: Geolocalización & Google Maps).
 * Controla permisos de GPS, ubicación del estudiante, alternancia de campus, radio de radar,
 * búsqueda en tiempo real, carga de locales y visualización de descuentos en el BottomSheet.
 *
 * @param placeRepository Repositorio de lugares y comercios afiliados.
 * @param locationRepository Repositorio para consultar GPS del dispositivo.
 * @param filterPlacesUseCase Caso de uso para filtrar dentro del radio con Haversine.
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

    fun setRadarRadius(radiusKm: Double) {
        _uiState.update { it.copy(selectedRadiusKm = radiusKm) }
        loadPlaces()
    }

    fun selectCampus(campus: CampusLocation) {
        _uiState.update {
            it.copy(
                selectedCampus = campus,
                userLocation = campus.coordinates,
                isDefaultLocation = false
            )
        }
        loadPlaces()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun loadPlaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            placeRepository.getPlaces().collect { rawPlaces ->
                val userCoords = UserCoordinates(
                    latitude = _uiState.value.userLocation.latitude,
                    longitude = _uiState.value.userLocation.longitude
                )
                val filtered = filterPlacesUseCase(
                    userLocation = userCoords,
                    places = rawPlaces,
                    maxDistanceKm = _uiState.value.selectedRadiusKm
                )

                _uiState.update {
                    it.copy(
                        nearbyPlaces = filtered,
                        isLoading = false
                    )
                }
            }
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
