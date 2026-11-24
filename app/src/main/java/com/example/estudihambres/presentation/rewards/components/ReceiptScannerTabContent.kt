package com.example.estudihambres.presentation.rewards.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.estudihambres.core.theme.CampusSuccess
import com.example.estudihambres.core.theme.CampusWarning
import com.example.estudihambres.domain.model.ReceiptDraft
import com.example.estudihambres.domain.model.ReceiptSource
import com.example.estudihambres.presentation.rewards.ReceiptStep
import com.example.estudihambres.presentation.rewards.RewardsUiState
import com.example.estudihambres.presentation.rewards.RewardsViewModel
import com.example.estudihambres.presentation.rewards.receipt.ReceiptCameraView
import com.example.estudihambres.presentation.rewards.receipt.SunatOutcome
import com.example.estudihambres.presentation.rewards.receipt.SunatOutcomeKind
import com.example.estudihambres.presentation.rewards.receipt.SunatVerificationPanel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pestaña "Boleta": registro guiado en tres pasos.
 *
 * 1. **Escanear**: la cámara lee el QR SUNAT (o el texto impreso).
 * 2. **Revisar**: el estudiante confirma RUC, serie, número, importe y fecha.
 * 3. **Verificar**: se consulta la validez en SUNAT y, si procede, se acreditan los puntos.
 *
 * Reemplaza tres flujos paralelos (pestaña, modal "in-system" y diálogo manual) que pedían
 * los mismos datos de formas distintas.
 */
@Composable
fun ReceiptScannerTabContent(
    state: RewardsUiState,
    viewModel: RewardsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StepIndicator(current = state.receiptStep)

        AnimatedContent(
            targetState = state.receiptStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "receiptStep"
        ) { step ->
            when (step) {
                ReceiptStep.SCAN -> ScanStep(viewModel)
                ReceiptStep.REVIEW -> state.receiptDraft?.let { draft ->
                    ReviewStep(draft = draft, error = state.receiptFormError, viewModel = viewModel)
                }
                ReceiptStep.VERIFY -> state.receiptDraft?.let { draft ->
                    VerifyStep(
                        draft = draft,
                        attempt = state.sunatAttempt,
                        outcome = state.sunatOutcome,
                        isCrediting = state.isScanningReceipt,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Indicador de pasos
// --------------------------------------------------------------------------

@Composable
private fun StepIndicator(current: ReceiptStep) {
    val steps = listOf(ReceiptStep.SCAN to "Escanear", ReceiptStep.REVIEW to "Revisar", ReceiptStep.VERIFY to "Verificar")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, (step, label) ->
            val reached = step.ordinal <= current.ordinal
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (reached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (step == current) FontWeight.Bold else FontWeight.Normal,
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

// --------------------------------------------------------------------------
// Paso 1: Escanear
// --------------------------------------------------------------------------

@Composable
private fun ScanStep(viewModel: RewardsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReceiptCameraView(
            isScanning = true,
            onDetected = viewModel::onReceiptScanned,
            onPermissionDenied = viewModel::onStartManualReceipt,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
        )
        Text(
            text = "Boletas y facturas electrónicas desde S/ 5.00, con hasta 3 días de emitidas. Cada comprobante suma 80 StudiPuntos una sola vez.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = viewModel::onStartManualReceipt, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Escribir los datos a mano")
        }
    }
}

// --------------------------------------------------------------------------
// Paso 2: Revisar
// --------------------------------------------------------------------------

@Composable
private fun ReviewStep(draft: ReceiptDraft, error: String?, viewModel: RewardsViewModel) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Revisa los datos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            SourceBadge(draft.source)
        }
        Text(
            text = "Deben coincidir con lo impreso en tu ${draft.tipoLabel.lowercase()}: SUNAT compara el importe y la fecha exactos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (draft.establishment.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(draft.establishment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        OutlinedTextField(
            value = draft.ruc,
            onValueChange = { viewModel.onDraftChange(draft.copy(ruc = it.filter(Char::isDigit).take(11))) },
            label = { Text("RUC del comercio") },
            supportingText = { Text("11 dígitos, figura arriba en el ticket") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = draft.serie,
                onValueChange = { value ->
                    viewModel.onDraftChange(draft.copy(serie = value.uppercase().filter { it.isLetterOrDigit() }.take(4)))
                },
                label = { Text("Serie") },
                placeholder = { Text("B001") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true,
                modifier = Modifier.weight(0.42f)
            )
            OutlinedTextField(
                value = draft.numero,
                onValueChange = { viewModel.onDraftChange(draft.copy(numero = it.filter(Char::isDigit).take(8))) },
                label = { Text("Número") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.58f)
            )
        }
        OutlinedTextField(
            value = draft.amountText,
            onValueChange = { value ->
                viewModel.onDraftChange(draft.copy(amountText = value.filter { it.isDigit() || it == '.' || it == ',' }.take(9)))
            },
            label = { Text("Importe total") },
            prefix = { Text("S/ ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Fecha de emisión", style = MaterialTheme.typography.labelLarge)
        DateChips(draft = draft, viewModel = viewModel)

        if (error != null) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        Button(
            onClick = viewModel::onVerifyWithSunat,
            enabled = draft.isComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Verificar en SUNAT", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = viewModel::onRescan, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Escanear otra vez")
        }
    }
}

/** Sólo se aceptan comprobantes de los últimos 3 días: Hoy / Ayer / Anteayer cubren la ventana. */
@Composable
private fun DateChips(draft: ReceiptDraft, viewModel: RewardsViewModel) {
    val options = listOf(0 to "Hoy", 1 to "Ayer", 2 to "Anteayer").map { (days, label) ->
        viewModel.startOfDay(days) to label
    }
    val matchesOption = options.any { it.first == draft.issueDateMillis }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        options.forEach { (millis, label) ->
            FilterChip(
                selected = draft.issueDateMillis == millis,
                onClick = { viewModel.onDraftChange(draft.copy(issueDateMillis = millis)) },
                label = { Text("$label · ${formatShortDate(millis)}") }
            )
        }
    }
    if (!matchesOption) {
        // Fecha leída del comprobante fuera de la ventana: se muestra para que el usuario lo entienda
        FilterChip(selected = true, onClick = {}, label = { Text("Leída: ${formatDate(draft.issueDateMillis)}") })
    }
}

// --------------------------------------------------------------------------
// Paso 3: Verificar en SUNAT y acreditar
// --------------------------------------------------------------------------

@Composable
private fun VerifyStep(
    draft: ReceiptDraft,
    attempt: Int,
    outcome: SunatOutcome?,
    isCrediting: Boolean,
    viewModel: RewardsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Text(
                text = "${draft.tipoLabel} ${draft.serie}-${draft.numero}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            SummaryRow("Comercio", draft.establishment.ifBlank { "RUC ${draft.ruc}" })
            SummaryRow("RUC", draft.ruc)
            SummaryRow("Fecha", formatDate(draft.issueDateMillis))
            SummaryRow("Importe", "S/ %.2f".format(Locale.US, draft.amount ?: 0.0))
        }

        SectionCard {
            Text("Verificación en SUNAT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Consultamos la validez del comprobante en la web oficial e-consulta.sunat.gob.pe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SunatVerificationPanel(
                draft = draft,
                attempt = attempt,
                onOutcome = { outcome -> viewModel.onSunatOutcome(attempt, outcome) }
            )
            if (outcome != null) {
                OutcomeBlock(outcome = outcome, isCrediting = isCrediting, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun OutcomeBlock(outcome: SunatOutcome, isCrediting: Boolean, viewModel: RewardsViewModel) {
    val (icon, tone, title) = when (outcome.kind) {
        SunatOutcomeKind.VALID -> Triple(Icons.Default.Verified, CampusSuccess, "SUNAT confirma que el comprobante es válido")
        SunatOutcomeKind.UNCONFIRMED -> Triple(Icons.Default.HelpOutline, CampusWarning, "No pudimos confirmarlo con SUNAT")
        SunatOutcomeKind.CANCELLED -> Triple(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error, "SUNAT indica que el comprobante fue dado de baja")
        SunatOutcomeKind.DATA_REJECTED -> Triple(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error, "SUNAT no aceptó los datos")
        SunatOutcomeKind.NOT_VALID -> Triple(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error, "SUNAT no reconoce este comprobante")
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = tone.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (outcome.detail.isNotBlank()) {
                Text(outcome.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (outcome.kind == SunatOutcomeKind.NOT_VALID || outcome.kind == SunatOutcomeKind.DATA_REJECTED) {
                Text(
                    text = "Revisa que la fecha y el importe sean exactamente los de la boleta. Si la emitieron hace minutos, puede tardar en aparecer en SUNAT.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    when (outcome.kind) {
        SunatOutcomeKind.VALID -> CreditButton(label = "Acreditar 80 StudiPuntos", isCrediting = isCrediting, onClick = viewModel::onCreditReceipt)
        SunatOutcomeKind.UNCONFIRMED -> {
            Button(onClick = viewModel::onRetrySunat, enabled = !isCrediting, modifier = Modifier.fillMaxWidth()) {
                Text("Reintentar consulta")
            }
            Text(
                "La acreditación queda disponible cuando SUNAT confirme el comprobante.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            Button(onClick = viewModel::onBackToReview, modifier = Modifier.fillMaxWidth()) { Text("Corregir datos") }
            TextButton(onClick = viewModel::onRescan, modifier = Modifier.fillMaxWidth()) { Text("Escanear otro comprobante") }
        }
    }
}

@Composable
private fun CreditButton(label: String, isCrediting: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isCrediting,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (isCrediting) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

// --------------------------------------------------------------------------
// Utilidades visuales
// --------------------------------------------------------------------------

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.65f))
    }
}

@Composable
private fun SourceBadge(source: ReceiptSource) {
    val (label, icon) = when (source) {
        ReceiptSource.QR -> "Leído del QR" to Icons.Default.QrCodeScanner
        ReceiptSource.OCR -> "Leído del ticket" to Icons.Default.QrCodeScanner
        ReceiptSource.MANUAL -> "Ingreso manual" to Icons.Default.Edit
    }
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date(millis))

private fun formatShortDate(millis: Long): String = SimpleDateFormat("dd/MM", Locale.ROOT).format(Date(millis))
