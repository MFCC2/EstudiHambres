package com.example.estudihambres.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.data.repository.MockPlaceRepository
import com.example.estudihambres.data.repository.PlayServicesLocationRepositoryImpl
import com.example.estudihambres.domain.model.Place
import com.example.estudihambres.domain.model.RedeemedCoupon
import com.example.estudihambres.presentation.common.RedemptionVoucherDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

/**
 * Modos de visualización para el radar de convenios estudiantiles.
 */
enum class MapViewType(val title: String) {
    GOOGLE_MAPS("Google Maps"),
    OPEN_STREET("Radar Mapa"),
    LIST("Lista de Locales")
}

/**
 * Pantalla de radar interactivo de beneficios universitarios con soporte dual:
 * Google Maps nativo y OpenStreetMap (Leaflet), círculo de radio interactivo, carrusel inferior
 * de locales, buscador en vivo y alternancia rápida de sedes universitarias (Cusco / Lima).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(
            placeRepository = MockPlaceRepository(),
            locationRepository = PlayServicesLocationRepositoryImpl(LocalContext.current.applicationContext)
        )
    )
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var redeemedCouponForDialog by remember { mutableStateOf<RedeemedCoupon?>(null) }

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

    // Actualizar cámara si cambia la sede universitaria o ubicación del estudiante
    LaunchedEffect(state.userLocation) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(state.userLocation, 14.5f),
            durationMs = 900
        )
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var viewType by remember { mutableStateOf(MapViewType.GOOGLE_MAPS) }

    val placeCategories = listOf("Todos", "Comida", "Café", "Librería")
    val radiusOptions = listOf(5.0 to "⚡ 5 km", 15.0 to "🚲 15 km", 30.0 to "🚗 30 km")

    // Filtrado conjunto: por radio (aplicado en ViewModel), categoría y texto de búsqueda
    val filteredPlaces = remember(state.nearbyPlaces, state.selectedCategory, state.searchQuery) {
        state.nearbyPlaces.filter { place ->
            val matchesCategory = if (state.selectedCategory == "Todos") {
                true
            } else {
                place.category.contains(state.selectedCategory, ignoreCase = true)
            }

            val matchesSearch = if (state.searchQuery.isBlank()) {
                true
            } else {
                place.name.contains(state.searchQuery, ignoreCase = true) ||
                place.address.contains(state.searchQuery, ignoreCase = true) ||
                place.category.contains(state.searchQuery, ignoreCase = true) ||
                place.discountBadge.contains(state.searchQuery, ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (viewType) {
            MapViewType.OPEN_STREET -> {
                // 1. Radar Interactivo con OpenStreetMap y Leaflet (100% visible, sin requerir API key de Google)
                InteractiveOsmRadarMap(
                    userLocation = state.userLocation,
                    nearbyPlaces = filteredPlaces,
                    radarRadiusMeters = state.selectedRadiusKm * 1000.0,
                    onPlaceSelected = { viewModel.onPlaceSelected(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            MapViewType.GOOGLE_MAPS -> {
                // 2. Google Maps Compose nativo con círculo de radar y pines reactivos
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
                    // Círculo visual del radar que ilustra el alcance geográfico de convenios
                    Circle(
                        center = state.userLocation,
                        radius = state.selectedRadiusKm * 1000.0,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        strokeWidth = 3f
                    )

                    // Marcador de la ubicación del estudiante o campus
                    Marker(
                        state = rememberMarkerState(position = state.userLocation),
                        title = "Tú estás aquí",
                        snippet = if (state.isDefaultLocation) state.selectedCampus.displayName else "GPS Activo",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )

                    // Marcadores de establecimientos aliados filtrados
                    filteredPlaces.forEach { place ->
                        Marker(
                            state = rememberMarkerState(key = place.id, position = LatLng(place.latitude, place.longitude)),
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
                // 3. Vista de lista interactiva de los locales cercanos filtrados con sus calificaciones y distancias
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 220.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredPlaces.isEmpty()) {
                        item {
                            EmptyPlacesCard(
                                searchQuery = state.searchQuery,
                                selectedCategory = state.selectedCategory,
                                onResetFilters = {
                                    viewModel.onSearchQueryChange("")
                                    viewModel.onCategoryChange("Todos")
                                    viewModel.setRadarRadius(30.0)
                                }
                            )
                        }
                    } else {
                        items(filteredPlaces, key = { it.id }) { place ->
                            PlaceListItemCard(
                                place = place,
                                onClick = { viewModel.onPlaceSelected(place) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Banner flotante superior con Buscador en vivo, selector de Sede, Vista y Filtros
        Card(
            shape = CampusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Buscador en vivo de comercios
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            "Buscar hamburguesas, café, librería...",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    shape = CampusShapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fila 1: Contador de convenios e indicador de sede universitaria
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CampusShapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "RADAR ${state.selectedRadiusKm.toInt()} KM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${filteredPlaces.size} convenios visibles",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    // Selector de sede académica (Cusco / Lima)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CampusLocation.entries.forEach { campus ->
                            FilterChip(
                                selected = state.selectedCampus == campus,
                                onClick = {
                                    viewModel.selectCampus(campus)
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(campus.coordinates, 14.5f),
                                            700
                                        )
                                    }
                                },
                                label = { Text(campus.shortName, style = MaterialTheme.typography.labelSmall) },
                                shape = CampusShapes.small,
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Fila 2: Selector de Radio de Radar y Modo de Visualización
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Selector de vista
                    MapViewType.entries.forEach { mode ->
                        FilterChip(
                            selected = viewType == mode,
                            onClick = { viewType = mode },
                            label = { Text(mode.title, style = MaterialTheme.typography.labelSmall) },
                            shape = CampusShapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Selector de radio
                    radiusOptions.forEach { (radius, label) ->
                        FilterChip(
                            selected = state.selectedRadiusKm == radius,
                            onClick = { viewModel.setRadarRadius(radius) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = CampusShapes.small,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Fila 3: Filtro rápido por categoría de comercio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    placeCategories.forEach { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat,
                            onClick = { viewModel.onCategoryChange(cat) },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                            shape = CampusShapes.small,
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }
        }

        // 3. Carrusel inferior flotante de locales en los modos de Mapa
        if (viewType != MapViewType.LIST && filteredPlaces.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(bottom = 76.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPlaces, key = { it.id }) { place ->
                    PlaceMapPreviewCard(
                        place = place,
                        onClick = {
                            viewModel.onPlaceSelected(place)
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(place.latitude, place.longitude),
                                        15.5f
                                    ),
                                    600
                                )
                            }
                        }
                    )
                }
            }
        }

        // 4. Controles flotantes en la esquina inferior (Recentrar GPS y Alternar Vista)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (viewType == MapViewType.GOOGLE_MAPS) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(state.userLocation, 14.5f),
                                700
                            )
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
                }
            }

            ExtendedFloatingActionButton(
                onClick = {
                    viewType = when (viewType) {
                        MapViewType.GOOGLE_MAPS -> MapViewType.LIST
                        MapViewType.LIST -> MapViewType.OPEN_STREET
                        MapViewType.OPEN_STREET -> MapViewType.GOOGLE_MAPS
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (viewType) {
                            MapViewType.GOOGLE_MAPS -> Icons.Default.Map
                            MapViewType.OPEN_STREET -> Icons.Default.PinDrop
                            MapViewType.LIST -> Icons.AutoMirrored.Filled.List
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                text = {
                    Text(viewType.title, style = MaterialTheme.typography.labelSmall)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CampusShapes.medium,
                modifier = Modifier.height(44.dp)
            )
        }

        // 5. ModalBottomSheet con el detalle e instrucciones de canje del local seleccionado
        state.selectedPlace?.let { place ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.onPlaceSelected(null) },
                sheetState = bottomSheetState,
                shape = CampusShapes.large
            ) {
                PlaceDiscountDetailContent(
                    place = place,
                    onDismiss = { viewModel.onPlaceSelected(null) },
                    onRedeemCoupon = { p ->
                        val sessionManager = SessionManager.getInstance(context)
                        val coupon = sessionManager.redeemCoupon(
                            title = p.discountDescription,
                            partnerName = p.name,
                            category = p.category,
                            discountBadge = p.discountBadge,
                            couponId = p.id
                        )
                        viewModel.onPlaceSelected(null)
                        redeemedCouponForDialog = coupon
                    }
                )
            }
        }

        // 6. Diálogo interactivo del vale de canje con código de barras y token único
        redeemedCouponForDialog?.let { coupon ->
            RedemptionVoucherDialog(
                coupon = coupon,
                onDismiss = { redeemedCouponForDialog = null }
            )
        }
    }
}

/**
 * Radar interactivo OpenStreetMap basado en Leaflet.js con círculo dinámico de radar,
 * pines con categorización cromática y popups interactivos.
 */
private fun sanitizeForJson(input: String): String {
    return input.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("<", "\\u003C")
        .replace(">", "\\u003E")
        .replace("&", "\\u0026")
        .replace("'", "\\u0027")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveOsmRadarMap(
    userLocation: LatLng,
    nearbyPlaces: List<Place>,
    radarRadiusMeters: Double,
    onPlaceSelected: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val placesJson = remember(nearbyPlaces) {
        val items = nearbyPlaces.map { p ->
            val colorHex = when {
                p.category.contains("Comida", ignoreCase = true) || p.category.contains("Pizzería", ignoreCase = true) -> "#EA580C"
                p.category.contains("Café", ignoreCase = true) || p.category.contains("Cafetería", ignoreCase = true) -> "#D97706"
                p.category.contains("Librería", ignoreCase = true) -> "#0284C7"
                else -> "#7C3AED"
            }
            val safeId = sanitizeForJson(p.id)
            val safeName = sanitizeForJson(p.name)
            val safeBadge = sanitizeForJson(p.discountBadge)
            val safeCategory = sanitizeForJson(p.category)
            """{"id":"$safeId","name":"$safeName","badge":"$safeBadge","lat":${p.latitude},"lng":${p.longitude},"category":"$safeCategory","color":"$colorHex"}"""
        }
        "[${items.joinToString(",")}]"
    }

    val htmlContent = remember(userLocation, placesJson, radarRadiusMeters) {
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
                    width: 22px;
                    height: 22px;
                    box-shadow: 0 0 12px rgba(51, 68, 238, 0.9);
                }
                .place-pin {
                    border: 2px solid #FFFFFF;
                    border-radius: 50%;
                    width: 18px;
                    height: 18px;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                }
                .leaflet-popup-content-wrapper {
                    border-radius: 12px;
                    box-shadow: 0 4px 16px rgba(0,0,0,0.18);
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

                    // Círculo visual de radar
                    var radar = L.circle([${userLocation.latitude}, ${userLocation.longitude}], {
                        radius: $radarRadiusMeters,
                        color: '#3344EE',
                        weight: 2,
                        fillColor: '#3344EE',
                        fillOpacity: 0.08
                    }).addTo(map);

                    // Pin del estudiante
                    var userIcon = L.divIcon({ className: 'user-pin', iconSize: [22, 22], iconAnchor: [11, 11] });
                    L.marker([${userLocation.latitude}, ${userLocation.longitude}], { icon: userIcon })
                        .addTo(map)
                        .bindPopup("<b>📍 Tú estás aquí</b><br>Campus Universitario");

                    // Pines cromáticos de comercios
                    function escapeHtml(str) {
                        if (!str) return '';
                        return String(str)
                            .replace(/&/g, '&amp;')
                            .replace(/</g, '&lt;')
                            .replace(/>/g, '&gt;')
                            .replace(/"/g, '&quot;')
                            .replace(/'/g, '&#39;');
                    }

                    var places = $placesJson;
                    places.forEach(function(p) {
                        var pIcon = L.divIcon({
                            className: 'place-pin',
                            html: '<div style=\"background:' + p.color + ';width:100%;height:100%;border-radius:50%;\"></div>',
                            iconSize: [18, 18],
                            iconAnchor: [9, 9]
                        });
                        var marker = L.marker([p.lat, p.lng], { icon: pIcon }).addTo(map);
                        var popupContent = "<div style='min-width:140px;text-align:center;'>" +
                            "<b style='font-size:13px;'>" + escapeHtml(p.name) + "</b><br>" +
                            "<span style='color:#E65100;font-weight:bold;font-size:12px;'>" + escapeHtml(p.badge) + "</span><br>" +
                            "<span style='color:#666;font-size:11px;'>" + escapeHtml(p.category) + "</span><br>" +
                            "<button onclick='window.Android.onSelectPlace(\"" + escapeHtml(p.id) + "\")' style='margin-top:8px;background:#3344EE;color:white;border:none;padding:5px 12px;border-radius:6px;font-weight:bold;font-size:11px;cursor:pointer;width:100%;'>Ver Beneficio</button>" +
                            "</div>";
                        marker.bindPopup(popupContent);
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
                // Fortificación de seguridad en el WebView
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setGeolocationEnabled(false)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return true
                        // Permitir únicamente tiles y scripts de OpenStreetMap y Leaflet CDN oficial
                        return !(url.startsWith("https://openstreetmap.org") || url.startsWith("https://unpkg.com"))
                    }
                }
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
 * Tarjeta compacta del carrusel horizontal inferior para previsualizar locales sobre el mapa.
 */
@Composable
private fun PlaceMapPreviewCard(
    place: Place,
    onClick: () -> Unit
) {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
        modifier = Modifier
            .width(225.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = place.rating.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = place.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))
            place.distanceKm?.let { dist ->
                val walkMinutes = (dist * 12).toInt().coerceAtLeast(1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "~$walkMinutes min a pie (${if (dist < 1.0) "${(dist * 1000).toInt()} m" else "${"%.1f".format(dist)} km"})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de la lista de locales cercanos dentro del radio configurado.
 */
@Composable
private fun PlaceListItemCard(
    place: Place,
    onClick: () -> Unit
) {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = place.discountBadge,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
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
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${place.category} • ${place.address}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            place.distanceKm?.let { dist ->
                val walkMinutes = (dist * 12).toInt().coerceAtLeast(1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "📍 A solo ${"%.1f".format(dist)} km (~$walkMinutes min caminando)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            val navUri = Uri.parse("google.navigation:q=${place.latitude},${place.longitude}")
                            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(navIntent)
                        } catch (_: Exception) {
                            try {
                                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            } catch (_: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cómo llegar", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Descuento", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Estado vacío cuando no hay comercios coincidentes con los filtros aplicados.
 */
@Composable
private fun EmptyPlacesCard(
    searchQuery: String,
    selectedCategory: String,
    onResetFilters: () -> Unit
) {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No se hallaron convenios",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "Ningún resultado para '$searchQuery' en categoría '$selectedCategory'." else "Prueba ampliando el radio a 30 km.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onResetFilters,
                shape = CampusShapes.small
            ) {
                Text("Restablecer Filtros y Radio", style = MaterialTheme.typography.labelSmall)
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onRedeemCoupon: (Place) -> Unit = {}
) {
    Column(
        modifier = modifier
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
                    fontWeight = FontWeight.Bold,
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = place.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${place.category} • ${place.address}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        place.distanceKm?.let { dist ->
            val walkMin = (dist * 12).toInt().coerceAtLeast(1)
            val driveMin = (dist * 3.5).toInt().coerceAtLeast(2)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "📍 A solo ${"%.1f".format(dist)} km • 🚶‍♂️ ~$walkMin min a pie • 🚗 ~$driveMin min en auto",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
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

        Spacer(modifier = Modifier.height(16.dp))
        val context = LocalContext.current
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    try {
                        val navUri = Uri.parse("google.navigation:q=${place.latitude},${place.longitude}")
                        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        context.startActivity(navIntent)
                    } catch (_: Exception) {
                        try {
                            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        } catch (_: Exception) {}
                    }
                },
                shape = CampusShapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🗺️ Cómo llegar (Google Maps)", fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            val uri = Uri.parse(place.url.trim())
                            val scheme = uri.scheme?.lowercase()
                            if (scheme == "https" || scheme == "http") {
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                }
                                context.startActivity(intent)
                            }
                        } catch (_: Exception) {}
                    },
                    shape = CampusShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sitio Oficial")
                }

                Button(
                    onClick = { onRedeemCoupon(place) },
                    shape = CampusShapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Canjear Cupón")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
