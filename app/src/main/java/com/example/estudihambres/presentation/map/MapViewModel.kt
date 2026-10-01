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
                val filtered = filterPlacesUseCase(userCoords, rawPlaces, maxDistanceKm = 30.0)
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
