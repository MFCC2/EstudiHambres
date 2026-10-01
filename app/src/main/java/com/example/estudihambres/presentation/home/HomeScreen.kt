package com.example.estudihambres.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudihambres.core.theme.CampusShapes

/**
 * Pantalla principal del catálogo de convenios y descuentos estudiantiles (presentation/home).
 *
 * @param modifier Modificador de diseño Compose.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection()
        }
        item {
            PromoCard(
                title = "Bienvenido a CampusPass",
                subtitle = "Accede a beneficios universitarios exclusivos y encuentra comida económica cerca a tu campus.",
                badge = "Fase 1 Setup"
            )
        }
    }
}

/**
 * Sección de encabezado con saludo y estado del estudiante.
 */
@Composable
private fun HeaderSection() {
    Column {
        Text(
            text = "CampusPass \uD83C\uDF93",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Explora convenios y restaurantes universitarios aliados",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tarjeta de promoción o anuncio respetando las directrices de diseño (borde 16dp, colores corporativos).
 *
 * @param title Título principal del anuncio.
 * @param subtitle Descripción o detalle.
 * @param badge Etiqueta destacada.
 */
@Composable
private fun PromoCard(
    title: String,
    subtitle: String,
    badge: String
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
                .padding(20.dp)
        ) {
            Surface(
                shape = CampusShapes.small,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
