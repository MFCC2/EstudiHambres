package com.example.estudihambres.presentation.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.model.Place
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Pantalla de radar interactivo de 30 km construida con Google Maps Compose (presentation/map).
 *
 * Muestra el marcador de ubicación del estudiante (o fallback a campus universitario),
 * al menos 8 marcadores de locales con convenios y un ModalBottomSheet con el descuento
 * al pulsar sobre cualquier marcador.
 *
 * @param viewModel ViewModel del mapa.
 * @param modifier Modificador Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Solicitar permiso de ubicación al ingresar
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onLocationPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(state.userLocation, 14.5f)
    }

    // Actualizar cámara si cambia la ubicación del estudiante
    LaunchedEffect(state.userLocation) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(state.userLocation, 14.5f)
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Google Maps interactivo
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = state.hasLocationPermission),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            )
        ) {
            // Marcador de la ubicación del usuario
            Marker(
                state = MarkerState(position = state.userLocation),
                title = "Tú estás aquí",
                snippet = if (state.isDefaultLocation) "Ubicación Campus Universitario" else "GPS Activo",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )

            // Marcadores de locales con ofertas dentro del radio de 30 km
            state.nearbyPlaces.forEach { place ->
                Marker(
                    state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                    title = place.name,
                    snippet = "${place.discountBadge} • ${place.category}",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    onClick = {
                        viewModel.onPlaceSelected(place)
                        true
                    }
                )
            }
        }

        // 2. Banner flotante con el estado del radar y conteo de ofertas
        Card(
            shape = CampusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CampusShapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "RADAR 30 KM",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${state.nearbyPlaces.size} locales con convenios cercanos",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Botón flotante para recentrar mapa
        FloatingActionButton(
            onClick = {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(state.userLocation, 14.5f)
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
        }

        // 4. ModalBottomSheet con el descuento del local seleccionado
        state.selectedPlace?.let { place ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.onPlaceSelected(null) },
                sheetState = bottomSheetState,
                shape = CampusShapes.large
            ) {
                PlaceDiscountDetailContent(
                    place = place,
                    onDismiss = { viewModel.onPlaceSelected(null) }
                )
            }
        }
    }
}

/**
 * Contenido detallado del beneficio del local dentro del ModalBottomSheet.
 */
@Composable
private fun PlaceDiscountDetailContent(
    place: Place,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CampusShapes.small,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = place.discountBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = place.rating.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = place.name, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${place.category} • ${place.address}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        place.distanceKm?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\uD83D\uDCCD A solo ${"%.1f".format(it)} km de tu ubicación",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            shape = CampusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = place.discountDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onDismiss,
                shape = CampusShapes.small,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Canjear Beneficio")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
