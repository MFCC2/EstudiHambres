package com.example.estudihambres.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.model.Promotion
import com.example.estudihambres.domain.model.VerificationStatus

/**
 * Dashboard principal de CampusPass (presentation/home).
 * Presenta el saludo y estado del estudiante, ahorro acumulado, búsqueda en tiempo real,
 * carrusel de categorías temáticas y catálogo de ofertas universitarias.
 *
 * @param viewModel ViewModel encargado del estado y filtrado del dashboard.
 * @param modifier Modificador Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBarDashboard(
                studentName = state.studentName,
                verificationStatus = state.verificationStatus
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Tarjeta de Ahorro Acumulado
            item {
                SavingsHighlightCard(accumulatedSavings = state.accumulatedSavings)
            }

            // 2. Barra de Búsqueda Funcional
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Buscar hamburguesas, apps, transportes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    shape = CampusShapes.small,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Carrusel Horizontal de Categorías
            item {
                CategoryCarousel(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelect(it) }
                )
            }

            // Encabezado de la lista
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Beneficios Disponibles (${state.filteredPromotions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 4. Lista Vertical de Promociones
            if (state.filteredPromotions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron ofertas para \"${state.searchQuery}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.filteredPromotions, key = { it.id }) { promo ->
                    PromotionCard(promotion = promo)
                }
            }
        }
    }
}

/**
 * TopAppBar que exhibe el avatar del estudiante, saludo cordial y badge de verificación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarDashboard(
    studentName: String,
    verificationStatus: VerificationStatus
) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                // Avatar circular
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "\uD83C\uDF93", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡Hola, ${studentName.split(" ").firstOrNull() ?: "Estudiante"}! \uD83D\uDC4B",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Explora convenios universitarios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Badge de verificación
                VerificationBadge(status = verificationStatus)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * Indicador visual del estado del carnet según las reglas de CampusPass.
 */
@Composable
private fun VerificationBadge(status: VerificationStatus) {
    val (badgeText, badgeColor) = when (status) {
        VerificationStatus.VERIFIED -> "VERIFICADO" to MaterialTheme.colorScheme.tertiary
        VerificationStatus.PENDING_VERIFICATION -> "PENDIENTE" to MaterialTheme.colorScheme.secondary
        VerificationStatus.UNVERIFIED -> "NO VERIF." to Color.Gray
        VerificationStatus.REJECTED -> "RECHAZADO" to MaterialTheme.colorScheme.error
    }

    Surface(
        shape = CampusShapes.small,
        color = badgeColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Tarjeta destacada con el cálculo acumulativo de ahorro en Soles.
 */
@Composable
private fun SavingsHighlightCard(accumulatedSavings: Double) {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Ahorro acumulado este ciclo",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "S/ ${"%.2f".format(accumulatedSavings)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "¡Aprovecha tus descuentos activos!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Carrusel horizontal de categorías para filtrado dinámico.
 */
@Composable
private fun CategoryCarousel(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category) },
                label = { Text(category) },
                shape = CampusShapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

/**
 * Tarjeta individual para mostrar promociones con bordes redondeados a 16.dp.
 */
@Composable
private fun PromotionCard(promotion: Promotion) {
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
                        text = promotion.discountTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = CampusShapes.small,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Ahorras ~S/ ${"%.2f".format(promotion.estimatedSavingsSoles)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = promotion.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = promotion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${promotion.partnerName} • ${promotion.category}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
