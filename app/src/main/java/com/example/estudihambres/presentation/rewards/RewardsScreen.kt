package com.example.estudihambres.presentation.rewards

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.core.legal.LegalConstants
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.model.PointTransaction
import com.example.estudihambres.domain.model.PointTransactionType
import com.example.estudihambres.domain.model.RedeemedReward
import com.example.estudihambres.domain.model.RedemptionMethod
import com.example.estudihambres.domain.model.RewardItem
import com.example.estudihambres.domain.usecase.ValidateSunatReceiptUseCase
import com.example.estudihambres.presentation.rewards.components.LiveReceiptScannerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal de la Tienda de StudiPuntos (Apartado Premios) de EstudiHambres.
 *
 * Estructurada en 3 pestañas principales de Material 3:
 * 1. [Catálogo Oficial]: Beneficios y tarjetas de regalo de convenios universitarios reales.
 * 2. [Mis Vales]: Billetera permanente de vales canjeados con QR, código de barras y temporizador anti-screenshot.
 * 3. [Historial & Misiones]: Libro mayor contable con encadenamiento SHA-256 y misiones de lealtad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    modifier: Modifier = Modifier,
    viewModel: RewardsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Premios & StudiPuntos",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openLegalDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Normativa SBS y Legal",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Tarjeta Cabecera de Billetera de Puntos (Siempre visible)
            WalletHeaderCard(
                studiPuntos = state.studiPuntos,
                streakText = state.activeStreakText,
                isScanning = state.isScanningReceipt,
                onOpenReceiptDialog = { viewModel.openReceiptScanner() },
                onOpenLegal = { viewModel.openLegalDialog() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 2. Barra de Navegación por Pestañas (Material 3 PrimaryTabRow)
            PrimaryTabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabSelect(0) },
                    text = { Text("🎁 Catálogo", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabSelect(1) },
                    text = {
                        Text(
                            text = if (state.redeemedRewards.isNotEmpty()) "🎟️ Mis Vales (${state.redeemedRewards.size})" else "🎟️ Mis Vales",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.onTabSelect(2) },
                    text = { Text("⚡ Historial", fontWeight = FontWeight.Bold) }
                )
            }

            // 3. Contenedor de la Pestaña Activa
            when (state.selectedTab) {
                0 -> CatalogTabContent(
                    state = state,
                    onCategorySelect = { viewModel.onCategorySelect(it) },
                    onRedeem = { viewModel.onRedeemReward(it) }
                )
                1 -> MyVouchersTabContent(
                    vouchers = state.redeemedRewards,
                    onMarkAsUsed = { viewModel.onMarkRewardAsUsed(it) }
                )
                2 -> HistoryAndMissionsTabContent(
                    transactions = state.pointTransactions,
                    onOpenReceiptDialog = { viewModel.openReceiptScanner() }
                )
            }
        }
    }

    // Modal de confirmación al canjear
    state.activeRedeemedReward?.let { redeemed ->
        RewardRedemptionDialog(
            redeemed = redeemed,
            onDismiss = {
                viewModel.dismissRedemptionDialog()
                viewModel.onTabSelect(1) // Cambia a pestaña 'Mis Vales'
            }
        )
    }

    // Modal de Escáner en tiempo real con visor vertical de gran escala para boletas
    if (state.showReceiptScanner) {
        LiveReceiptScannerDialog(
            onDismiss = { viewModel.dismissReceiptScanner() },
            onQrDetected = { qrText ->
                viewModel.onRegisterReceiptQr(qrText)
            },
            onManualOcrDetected = { ruc, serie, correlativo, monto ->
                viewModel.onRegisterReceiptManual(ruc, serie, correlativo, monto)
            },
            onSwitchToManualInput = {
                viewModel.switchToManualReceipt()
            }
        )
    }

    // Modal para registrar Boleta SUNAT
    if (state.showReceiptDialog) {
        ReceiptRegisterDialog(
            isProcessing = state.isScanningReceipt,
            onDismiss = { viewModel.dismissReceiptDialog() },
            onSubmitManual = { ruc, serie, correlativo, amount ->
                viewModel.onRegisterReceiptManual(ruc, serie, correlativo, amount)
            },
            onSubmitQr = { qrText ->
                viewModel.onRegisterReceiptQr(qrText)
            },
            onSwitchToScanner = {
                viewModel.switchToScannerReceipt()
            }
        )
    }

    // Modal informativo de Normativa SBS & INDECOPI
    if (state.showLegalDialog) {
        SbsLegalDisclaimerDialog(
            onDismiss = { viewModel.dismissLegalDialog() }
        )
    }
}

/**
 * Pestaña 1: Catálogo Oficial de Convenios Universitarios.
 */
@Composable
private fun CatalogTabContent(
    state: RewardsUiState,
    onCategorySelect: (String) -> Unit,
    onRedeem: (RewardItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tasa y Filtros
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tarjetas de Regalo & Vales Oficiales",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = state.conversionRateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Carrusel de Filtros por Categoría
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.categories.forEach { category ->
                    val isSelected = category == state.selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category, style = MaterialTheme.typography.labelSmall) },
                        shape = CampusShapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Lista de Recompensas
        if (state.filteredRewards.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay beneficios disponibles en esta categoría.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.filteredRewards, key = { it.id }) { reward ->
                RewardItemCard(
                    reward = reward,
                    userPoints = state.studiPuntos,
                    onRedeem = { onRedeem(reward) }
                )
            }
        }
    }
}

/**
 * Pestaña 2: Mis Vales Canjeados con Anti-Screenshot y Códigos Persistentes.
 */
@Composable
private fun MyVouchersTabContent(
    vouchers: List<RedeemedReward>,
    onMarkAsUsed: (String) -> Unit
) {
    if (vouchers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "🎟️", fontSize = 48.sp)
                Text(
                    text = "Aún no tienes vales canjeados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Canjea entradas de cine, comida o suscripciones con tus StudiPuntos en la pestaña Catálogo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Tus Vales Activos e Históricos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(vouchers, key = { it.id }) { voucher ->
                MyVoucherCard(
                    voucher = voucher,
                    onMarkAsUsed = { onMarkAsUsed(voucher.id) }
                )
            }
        }
    }
}

/**
 * Pestaña 3: Historial Contable Criptográfico (PointLedger) & Misiones.
 */
@Composable
private fun HistoryAndMissionsTabContent(
    transactions: List<PointTransaction>,
    onOpenReceiptDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tarjeta explicativa de misiones para ganar más puntos
        item {
            MissionsGuideCard(onScanReceiptClick = onOpenReceiptDialog)
        }

        // Título de Historial
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Libro Mayor de StudiPuntos (SHA-256)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auditado",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin movimientos registrados en tu billetera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionItemRow(tx = tx)
            }
        }
    }
}

/**
 * Cabecera superior que muestra el saldo de StudiPuntos y acceso a boletas.
 */
@Composable
private fun WalletHeaderCard(
    studiPuntos: Int,
    streakText: String,
    isScanning: Boolean,
    onOpenReceiptDialog: () -> Unit,
    onOpenLegal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BILLETERA DE RECOMPENSAS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Protegido SBS",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "%,d".format(studiPuntos),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "StudiPuntos 🪙",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onOpenLegal() }
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎁", fontSize = 22.sp)
                    }
                }
            }

            // Racha activa y botón de registro de boleta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = streakText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable(enabled = !isScanning) { onOpenReceiptDialog() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "+ Registrar Boleta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual de beneficio en el catálogo.
 */
@Composable
private fun RewardItemCard(
    reward: RewardItem,
    userPoints: Int,
    onRedeem: () -> Unit
) {
    val canAfford = userPoints >= reward.pointsCost
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = reward.iconEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = reward.partnerName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%,d pts".format(reward.pointsCost),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF59E0B)
                    )

                    Button(
                        onClick = onRedeem,
                        enabled = canAfford,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (canAfford) "Canjear" else "Faltan pts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de un vale canjeado en "Mis Vales", con código visible, código de barras y botón de uso.
 */
@Composable
private fun MyVoucherCard(
    voucher: RedeemedReward,
    onMarkAsUsed: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (voucher.isUsed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (voucher.isUsed) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = voucher.iconEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = voucher.rewardTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${voucher.partnerName} · ${voucher.redeemedAtFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (voucher.isUsed) MaterialTheme.colorScheme.outlineVariant else Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (voucher.isUsed) "UTILIZADO" else "VIGENTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (voucher.isUsed) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Código y Código de Barras (Canvas)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = voucher.redemptionCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Código de barras simulado en Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(32.dp)
                    ) {
                        val barWidth = 3.dp.toPx()
                        val spacing = 2.dp.toPx()
                        var x = 0f
                        val hash = voucher.redemptionCode.hashCode()
                        var index = 0
                        while (x < size.width) {
                            val isThick = ((hash shr (index % 16)) and 1) == 1
                            val currentWidth = if (isThick) barWidth * 1.8f else barWidth
                            drawLine(
                                color = if (voucher.isUsed) Color.Gray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.85f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = currentWidth
                            )
                            x += currentWidth + spacing
                            index++
                        }
                    }
                }
            }

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(voucher.redemptionCode))
                        isCopied = true
                        Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                    },
                    shape = CampusShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isCopied) "Copiado" else "Copiar", style = MaterialTheme.typography.labelSmall)
                }

                if (!voucher.isUsed) {
                    Button(
                        onClick = onMarkAsUsed,
                        shape = CampusShapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Usar en Caja", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (voucher.officialUrl.isNotBlank() && voucher.officialUrl.startsWith("http")) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(voucher.officialUrl))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Abrir portal oficial",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila de transacción en el libro mayor contable.
 */
@Composable
private fun TransactionItemRow(tx: PointTransaction) {
    val isPositive = tx.pointsDelta > 0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = if (isPositive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (tx.type) {
                                PointTransactionType.WELCOME_BONUS -> "🎓"
                                PointTransactionType.ID_VERIFICATION -> "🪪"
                                PointTransactionType.COUPON_REDEEMED -> "🍔"
                                PointTransactionType.RECEIPT_SCAN -> "🧾"
                                PointTransactionType.REWARD_PURCHASE -> "🎁"
                            },
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Hash: ${tx.txHash.take(12)}...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = if (isPositive) "+%,d pts".format(tx.pointsDelta) else "-%,d pts".format(kotlin.math.abs(tx.pointsDelta)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
            )
        }
    }
}

/**
 * Tarjeta didáctica con las misiones para ganar más puntos.
 */
@Composable
private fun MissionsGuideCard(onScanReceiptClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "¿Cómo ganar más StudiPuntos?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            MissionItemRow("1. Registro Estudiantil", "Bono de bienvenida inicial", "+200 pts")
            MissionItemRow("2. Carnet SUNEDU (OCR)", "Acreditar identidad oficial", "+300 pts")
            MissionItemRow("3. Canje de Cupones", "Por cada descuento de comida o transporte", "+50 pts")
            MissionItemRow("4. Boletas SUNAT", "Escanear comprobantes en comercios aliados", "+80 pts")

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onScanReceiptClick,
                shape = CampusShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Registrar Boleta Ahora (+80 pts)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MissionItemRow(title: String, subtitle: String, points: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = points,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF10B981),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Diálogo modal para registrar y auditar una Boleta de Venta Electrónica SUNAT.
 */
@Composable
private fun ReceiptRegisterDialog(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onSubmitManual: (String, String, String, Double) -> Unit,
    onSubmitQr: (String) -> Unit,
    onSwitchToScanner: () -> Unit = {}
) {
    var selectedPartner by remember { mutableStateOf("Delosi (Starbucks / KFC / Burger King)") }
    var selectedRuc by remember { mutableStateOf("20100107214") }
    var serie by remember { mutableStateOf("B001") }
    var correlativo by remember { mutableStateOf("") }
    var montoStr by remember { mutableStateOf("18.50") }
    var qrInput by remember { mutableStateOf("") }
    var isQrMode by remember { mutableStateOf(false) }

    val partners = listOf(
        "Delosi (Starbucks/KFC/BK)" to "20100107214",
        "Bembos (InRetail)" to "20252391498",
        "Cineplanet" to "20429683581",
        "Tiendas Tambo" to "20508565934",
        "Librerías Crisol" to "20100084761"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Boleta SUNAT", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Acredita +80 StudiPuntos registrando tu comprobante oficial de consumo universitario (antigüedad máx. 72h).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selector de Modo (Manual asistido, Pegar QR oficial o Escáner con Cámara)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isQrMode,
                        onClick = { isQrMode = false },
                        label = { Text("Manual") }
                    )
                    FilterChip(
                        selected = isQrMode,
                        onClick = { isQrMode = true },
                        label = { Text("Pegar QR") }
                    )
                    FilterChip(
                        selected = false,
                        onClick = onSwitchToScanner,
                        leadingIcon = {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        label = { Text("Cámara") }
                    )
                }

                if (isQrMode) {
                    OutlinedTextField(
                        value = qrInput,
                        onValueChange = { qrInput = it },
                        label = { Text("Pegar cadena QR SUNAT") },
                        placeholder = { Text("20100107214|03|B001|00049281|...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Selector de Comercio Aliado
                    Text("Comercio Aliado:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        partners.forEach { (name, ruc) ->
                            val isSel = selectedRuc == ruc
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedRuc = ruc
                                    selectedPartner = name
                                },
                                label = { Text(name.take(16)) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = serie,
                            onValueChange = { serie = it.uppercase() },
                            label = { Text("Serie") },
                            placeholder = { Text("B001") },
                            singleLine = true,
                            modifier = Modifier.weight(0.4f)
                        )
                        OutlinedTextField(
                            value = correlativo,
                            onValueChange = { correlativo = it },
                            label = { Text("Número (1-8 dígitos)") },
                            placeholder = { Text("49281") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    OutlinedTextField(
                        value = montoStr,
                        onValueChange = { montoStr = it },
                        label = { Text("Monto Total S/ (mín. S/ 5.00)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isQrMode) {
                        onSubmitQr(qrInput)
                    } else {
                        val amount = montoStr.toDoubleOrNull() ?: 18.50
                        onSubmitManual(selectedRuc, serie, correlativo, amount)
                    }
                },
                enabled = !isProcessing && (if (isQrMode) qrInput.isNotBlank() else correlativo.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Verificar y Acreditar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo explicativo de cumplimiento regulatorio SBS y Protección de Datos Personales (Ley N° 29733).
 */
@Composable
private fun SbsLegalDisclaimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Marco Legal & SBS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Regulación SBS (Ley N° 29985):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = LegalConstants.SBS_LEGAL_DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Protección de Datos (Ley N° 29733):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = LegalConstants.DATA_PROTECTION_POLICY,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Diálogo modal tras canjear un premio, mostrando el código y felicitaciones.
 */
@Composable
private fun RewardRedemptionDialog(
    redeemed: RedeemedReward,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎉", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¡Beneficio Canjeado!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = redeemed.rewardTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "-%,d StudiPuntos".format(redeemed.pointsCost),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CÓDIGO OFICIAL GUARDADO EN 'MIS VALES'",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = redeemed.redemptionCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(redeemed.redemptionCode))
                        isCopied = true
                    },
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCopied) "¡Código Copiado!" else "Copiar Código")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = CampusShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir a Mis Vales", fontWeight = FontWeight.Bold)
            }
        }
    )
}
