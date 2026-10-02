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
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
/**
 * Modos de visualización para el radar de comercios cercanos.
 */
enum class MapViewType(val title: String) {
    OPEN_STREET("Radar Mapa"),
    GOOGLE_MAPS("Google Maps"),
    LIST("Lista 30 km")
}

/**
 * Pantalla de radar interactivo de 30 km construida con Google Maps Compose y OpenStreetMap (presentation/map).
 *
 * Ofrece carga instantánea de mapa real sin requerir clave de facturación de Google Cloud,
 * opción alternativa de Google Maps nativo y modo lista de 9 comercios aliados con distancias y canjes.
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
    var viewType by remember { mutableStateOf(MapViewType.OPEN_STREET) }

    Box(modifier = modifier.fillMaxSize()) {
        when (viewType) {
            MapViewType.OPEN_STREET -> {
                // 1. Radar Interactivo con OpenStreetMap y Leaflet (100% visible, sin requerir API key de Google)
                InteractiveOsmRadarMap(
                    userLocation = state.userLocation,
                    nearbyPlaces = state.nearbyPlaces,
                    onPlaceSelected = { viewModel.onPlaceSelected(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            MapViewType.GOOGLE_MAPS -> {
                // 2. Google Maps Compose nativo (utiliza clave de Google Cloud Console si está activa)
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
                    Marker(
                        state = MarkerState(position = state.userLocation),
                        title = "Tú estás aquí",
                        snippet = if (state.isDefaultLocation) "Ubicación Campus Universitario" else "GPS Activo",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )

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
            }
            MapViewType.LIST -> {
                // 3. Vista de lista interactiva de los 9 locales cercanos con sus calificaciones y distancias
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 110.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.nearbyPlaces, key = { it.id }) { place ->
                        PlaceListItemCard(
                            place = place,
                            onClick = { viewModel.onPlaceSelected(place) }
                        )
                    }
                }
            }
        }

        // 2. Banner flotante con el estado del radar y selector de vista
        Card(
            shape = CampusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = "${state.nearbyPlaces.size} convenios cercanos",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector de modo visual: Radar OSM / Google Maps / Lista
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapViewType.values().forEach { mode ->
                        FilterChip(
                            selected = viewType == mode,
                            onClick = { viewType = mode },
                            label = { Text(mode.title, style = MaterialTheme.typography.labelSmall) },
                            shape = CampusShapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // 3. Controles flotantes en la esquina inferior (Alternar Modo y Recentrar GPS)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (viewType == MapViewType.GOOGLE_MAPS) {
                FloatingActionButton(
                    onClick = {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(state.userLocation, 14.5f)
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                }
            }

            ExtendedFloatingActionButton(
                onClick = {
                    viewType = when (viewType) {
                        MapViewType.OPEN_STREET -> MapViewType.GOOGLE_MAPS
                        MapViewType.GOOGLE_MAPS -> MapViewType.LIST
                        MapViewType.LIST -> MapViewType.OPEN_STREET
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (viewType) {
                            MapViewType.OPEN_STREET -> Icons.Default.Map
                            MapViewType.GOOGLE_MAPS -> Icons.Default.PinDrop
                            MapViewType.LIST -> Icons.AutoMirrored.Filled.List
                        },
                        contentDescription = null
                    )
                },
                text = {
                    Text(viewType.title)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CampusShapes.medium
            )
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
 * Radar interactivo OpenStreetMap basado en Leaflet.js.
 * Carga azulejos reales de calles, avenidas y comercios de forma garantizada y autónoma sin requerir API key de pago.
 */
@Composable
fun InteractiveOsmRadarMap(
    userLocation: LatLng,
    nearbyPlaces: List<Place>,
    onPlaceSelected: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val placesJson = remember(nearbyPlaces) {
        val items = nearbyPlaces.map { p ->
            """{"id":"${p.id}","name":"${p.name.replace("\"", "\\\"")}","badge":"${p.discountBadge.replace("\"", "\\\"")}","lat":${p.latitude},"lng":${p.longitude},"category":"${p.category.replace("\"", "\\\"")}"}"""
        }
        "[${items.joinToString(",")}]"
    }

    val htmlContent = remember(userLocation, placesJson) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                body, html, #map { margin: 0; padding: 0; height: 100%; width: 100%; background: #e8ecf4; font-family: -apple-system, Roboto, sans-serif; }
                .user-pin {
                    background-color: #3344EE;
                    border: 3px solid #FFFFFF;
                    border-radius: 50%;
                    width: 20px;
                    height: 20px;
                    box-shadow: 0 0 10px rgba(51, 68, 238, 0.8);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                try {
                    var map = L.map('map', { zoomControl: false }).setView([${userLocation.latitude}, ${userLocation.longitude}], 14);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 19,
                        attribution: '© OpenStreetMap'
                    }).addTo(map);

                    var userIcon = L.divIcon({ className: 'user-pin', iconSize: [20, 20], iconAnchor: [10, 10] });
                    L.marker([${userLocation.latitude}, ${userLocation.longitude}], { icon: userIcon })
                        .addTo(map)
                        .bindPopup("<b>Tú estás aquí</b><br>Campus Universitario");

                    var places = $placesJson;
                    places.forEach(function(p) {
                        var marker = L.marker([p.lat, p.lng]).addTo(map);
                        marker.bindPopup("<b>" + p.name + "</b><br><span style='color:#E65100;font-weight:bold'>" + p.badge + "</span><br><button onclick='window.Android.onSelectPlace(\"" + p.id + "\")' style='margin-top:6px;background:#3344EE;color:white;border:none;padding:6px 12px;border-radius:6px;font-weight:bold;cursor:pointer;'>Ver Beneficio</button>");
                        marker.on('click', function() {
                            if (window.Android && window.Android.onSelectPlace) {
                                window.Android.onSelectPlace(p.id);
                            }
                        });
                    });
                } catch(e) {
                    console.error(e);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onSelectPlace(placeId: String) {
                        val place = nearbyPlaces.firstOrNull { it.id == placeId }
                        if (place != null) {
                            post { onPlaceSelected(place) }
                        }
                    }
                }, "Android")
                loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

/**
 * Tarjeta interactiva de la lista de locales cercanos dentro del radio de 30 km (Modo Lista / Fallback).
 */
@Composable
private fun PlaceListItemCard(
    place: Place,
    onClick: () -> Unit
) {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = place.rating.toString(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = place.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${place.category} • ${place.address}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            place.distanceKm?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 A solo ${"%.1f".format(it)} km",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onClick,
                shape = CampusShapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver Descuento / Canjear")
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
