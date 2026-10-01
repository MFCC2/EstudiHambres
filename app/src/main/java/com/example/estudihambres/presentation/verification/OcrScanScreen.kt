package com.example.estudihambres.presentation.verification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudihambres.core.theme.CampusShapes

/**
 * Pantalla de verificación de carnet universitario mediante OCR (presentation/verification).
 * Cumple con la regla de Agente 3 permitiendo omitir el paso hacia PENDING_VERIFICATION.
 *
 * @param onSkipClick Acción ejecutada cuando el estudiante decide omitir el paso.
 * @param modifier Modificador de diseño Compose.
 */
@Composable
fun OcrScanScreen(
    onSkipClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "\uD83D\uDCF8 Verificación con Carnet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Escanea tu carnet universitario con la cámara. Google ML Kit extraerá automáticamente tu información institucional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { /* Próxima fase: iniciar cámara / OCR */ },
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Escanear carnet")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSkipClick,
                    shape = CampusShapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Omitir por ahora (PENDING_VERIFICATION)")
                }
            }
        }
    }
}
