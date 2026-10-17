package com.example.estudihambres.presentation.home

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.R
import com.example.estudihambres.core.theme.CampusGold
import com.example.estudihambres.core.theme.CampusGradientEmerald
import com.example.estudihambres.core.theme.CampusGradientPrimary
import com.example.estudihambres.core.theme.CampusGradientPurple
import com.example.estudihambres.core.theme.CampusGradientSunset
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.core.theme.CampusTagCulture
import com.example.estudihambres.core.theme.CampusTagFood
import com.example.estudihambres.core.theme.CampusTagTech
import com.example.estudihambres.core.theme.CampusTagTransport
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.domain.model.Promotion
import com.example.estudihambres.domain.model.RedeemedCoupon
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.presentation.common.RedemptionVoucherDialog
import java.io.File

/**
 * Dashboard principal de CampusPass (presentation/home).
 * Totalmente intuitivo: incluye búsqueda con chips de sugerencia rápida, banner interactivo
 * de validación de carnet, modal con instrucciones de canje y diálogo de confirmación de logout.
 *
 * @param onNavigateToProfile Callback para acceder a la pantalla de configuración de perfil.
 * @param onNavigateToVerification Callback para abrir el escáner de carnet universitario.
 * @param onNavigateToMap Callback para abrir el radar geográfico a 30 km.
 * @param onLogout Callback invocado al confirmar el cierre de sesión en la TopAppBar.
 * @param viewModel ViewModel encargado del estado y filtrado del dashboard.
 * @param modifier Modificador Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToVerification: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedPromoForDetail by remember { mutableStateOf<Promotion?>(null) }
    var redeemedCouponForDialog by remember { mutableStateOf<RedeemedCoupon?>(null) }

    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            // Manejo preventivo si no hay navegador disponible
        }
    }

    Scaffold(
        topBar = {
            TopAppBarDashboard(
                studentName = state.studentName,
                profilePhotoPath = state.profilePhotoPath,
                onAvatarClick = onNavigateToProfile,
                onLogout = onLogout
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de Modo Desarrollador (Vista Genérica)
            if (state.isDev) {
                item {
                    DevModeNoticeCard()
                }
            }

            // 1. Saludo y perfil universitario
            item {
                StudentGreetingHeader(
                    studentName = state.studentName,
                    university = state.university,
                    career = state.career,
                    verificationStatus = state.verificationStatus,
                    profilePhotoPath = state.profilePhotoPath,
                    onAvatarClick = onNavigateToProfile
                )
            }

            // Banner destacado para validar carnet si aún no está VERIFIED
            if (state.verificationStatus != VerificationStatus.VERIFIED) {
                item {
                    Card(
                        shape = CampusShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToVerification() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎓 Valida tu Carnet Universitario",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Desbloquea hasta 50% de descuento y beneficios tech oficiales.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onNavigateToVerification,
                                shape = CampusShapes.small,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Validar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Tarjeta de Ahorro Acumulado
            item {
                SavingsHighlightCard(accumulatedSavings = state.accumulatedSavings)
            }

            // 3. Barra de Búsqueda Funcional con chips sugeridos
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Buscar marcas, comida, apps...") },
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

                    // Búsquedas sugeridas en 1 solo tap
                    val quickSearchTags = listOf("Bembos", "Cineplanet", "Spotify", "Apple", "GitHub", "KFC", "JetBrains", "2x1")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickSearchTags.forEach { tag ->
                            val isSelected = state.searchQuery.equals(tag, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        viewModel.onSearchQueryChange("")
                                    } else {
                                        viewModel.onSearchQueryChange(tag)
                                    }
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                shape = CampusShapes.small
                            )
                        }
                    }
                }
            }

            // 4. Carrusel Horizontal de Categorías
            item {
                CategoryCarousel(
                    categories = state.categories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelect(it) }
                )
            }

            val isSearching = state.searchQuery.isNotBlank() || state.selectedCategory != "Todas"

            if (isSearching) {
                // Modo búsqueda / filtro por categoría
                item {
                    Text(
                        text = "Resultados encontrados (${state.filteredPromotions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (state.filteredPromotions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
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
                        DetailedPromotionCard(
                            promotion = promo,
                            onCardClick = { selectedPromoForDetail = promo },
                            onOpenUrl = { openUrl(promo.url) }
                        )
                    }
                }
            } else {
                // 5. SECCIÓN 1: "✨ Para ti (Más Populares)" (LazyRow)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "✨ Para ti",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ofertas seleccionadas para tu carrera",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.onCategorySelect("🔥 Destacados") }) {
                                Text("Ver todo")
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.popularPromotions, key = { it.id }) { promo ->
                                FeaturedPromoCard(
                                    promotion = promo,
                                    onCardClick = { selectedPromoForDetail = promo },
                                    onActivate = { openUrl(promo.url) }
                                )
                            }
                        }
                    }
                }

                // 6. SECCIÓN 2: "📍 Cerca de tu campus" (LazyRow)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📍 Cerca de tu campus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "A menos de 10 min a pie",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onNavigateToMap) {
                                Text("Ver en mapa ›", fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.nearbyPromotions, key = { it.id }) { promo ->
                                NearbyPromoCard(
                                    promotion = promo,
                                    onCardClick = { selectedPromoForDetail = promo },
                                    onRedeem = { openUrl(promo.url) }
                                )
                            }
                        }
                    }
                }

                // 7. SECCIÓN 3: "💻 Beneficios Virtuales & Tech" (LazyColumn items)
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "💻 Beneficios Virtuales & Tech",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Licencias 100% gratuitas con tu correo universitario",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(state.digitalPromotions, key = { it.id }) { promo ->
                    DigitalBenefitCard(
                        promotion = promo,
                        onCardClick = { selectedPromoForDetail = promo },
                        onClaim = { openUrl(promo.url) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Modal BottomSheet con el detalle completo del beneficio e instrucciones
        selectedPromoForDetail?.let { promo ->
            PromotionDetailSheet(
                promotion = promo,
                onDismiss = { selectedPromoForDetail = null },
                onOpenUrl = openUrl,
                onNavigateToMap = onNavigateToMap,
                onRedeemCoupon = { p ->
                    val sessionManager = SessionManager.getInstance(context)
                    val coupon = sessionManager.redeemCoupon(
                        title = p.title,
                        partnerName = p.partnerName,
                        category = p.category,
                        discountBadge = p.discountTag,
                        couponId = p.id
                    )
                    selectedPromoForDetail = null
                    redeemedCouponForDialog = coupon
                    viewModel.refreshSavings()
                }
            )
        }

        // Diálogo de comprobante oficial de cupón canjeado
        redeemedCouponForDialog?.let { coupon ->
            RedemptionVoucherDialog(
                coupon = coupon,
                onDismiss = {
                    redeemedCouponForDialog = null
                    viewModel.refreshSavings()
                },
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }
}

/**
 * TopAppBar que incluye el avatar, la marca de CampusPass y diálogo seguro de confirmación al cerrar sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarDashboard(
    studentName: String,
    profilePhotoPath: String?,
    onAvatarClick: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val avatarBitmap = remember(profilePhotoPath) {
        if (!profilePhotoPath.isNullOrBlank()) {
            val file = File(profilePhotoPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } else null
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("¿Cerrar Sesión?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Tendrás que volver a ingresar tus credenciales para acceder a tus beneficios universitarios.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    CenterAlignedTopAppBar(
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Ir a Mi Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Ir a Mi Perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CampusPass 🎓",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "INICIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onAvatarClick) {
                if (avatarBitmap != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Image(
                            bitmap = avatarBitmap.asImageBitmap(),
                            contentDescription = "Configurar Perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Configurar Perfil",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Cerrar Sesión",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * Encabezado de saludo personalizado y estado del estudiante.
 */
@Composable
private fun StudentGreetingHeader(
    studentName: String,
    university: String,
    career: String,
    verificationStatus: VerificationStatus,
    profilePhotoPath: String?,
    onAvatarClick: () -> Unit
) {
    val avatarBitmap = remember(profilePhotoPath) {
        if (!profilePhotoPath.isNullOrBlank()) {
            val file = File(profilePhotoPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } else null
    }

    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAvatarClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BIENVENIDO(A)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "¡Hola, ${studentName.split(" ").firstOrNull() ?: "Estudiante"}! 👋",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$university · $career",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                VerificationBadge(status = verificationStatus)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Editar ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Indicador visual del estado del carnet según las reglas de CampusPass.
 */
@Composable
private fun VerificationBadge(status: VerificationStatus) {
    val (badgeText, badgeColor) = when (status) {
        VerificationStatus.VERIFIED -> "Verificado" to Color(0xFF00C853)
        VerificationStatus.PENDING_VERIFICATION -> "Pendiente" to Color(0xFFFF9800)
        VerificationStatus.UNVERIFIED -> "No verif." to Color.Gray
        VerificationStatus.REJECTED -> "Rechazado" to MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = badgeColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Tarjeta destacada con el cálculo acumulativo de ahorro en Soles.
 */
@Composable
private fun SavingsHighlightCard(accumulatedSavings: Double) {
    Card(
        shape = CampusShapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF81C784).copy(alpha = 0.5f),
                        Color(0xFF64B5F6).copy(alpha = 0.5f)
                    )
                ),
                shape = CampusShapes.medium
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0D6838),
                            Color(0xFF107C41),
                            Color(0xFF00695C)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TU AHORRO ACUMULADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "🪙", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "S/ ${"%.2f".format(accumulatedSavings)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "CampusPass Ahorro",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Activo ›",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
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
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

/**
 * Tarjeta horizontal de "✨ Para ti (Más Populares)".
 * Diseñada con degradados por categoría, badges de ahorro acumulado y estética fintech moderna.
 */
@Composable
private fun FeaturedPromoCard(
    promotion: Promotion,
    onCardClick: () -> Unit = {},
    onActivate: () -> Unit
) {
    val bannerGradient = when {
        promotion.category.contains("Tech", ignoreCase = true) -> CampusGradientEmerald
        promotion.category.contains("Comida", ignoreCase = true) -> CampusGradientSunset
        promotion.category.contains("Cine", ignoreCase = true) -> CampusGradientPurple
        else -> CampusGradientPrimary
    }

    val categoryEmoji = when {
        promotion.category.contains("Comida", ignoreCase = true) || promotion.partnerName.contains("Bembos", ignoreCase = true) || promotion.partnerName.contains("KFC", ignoreCase = true) -> "🍔"
        promotion.category.contains("Cine", ignoreCase = true) || promotion.partnerName.contains("Cine", ignoreCase = true) -> "🎬"
        promotion.category.contains("Tech", ignoreCase = true) || promotion.partnerName.contains("JetBrains", ignoreCase = true) || promotion.partnerName.contains("Apple", ignoreCase = true) || promotion.partnerName.contains("Microsoft", ignoreCase = true) -> "💻"
        promotion.category.contains("Transporte", ignoreCase = true) || promotion.partnerName.contains("ATU", ignoreCase = true) -> "🚌"
        else -> "⭐"
    }

    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        modifier = Modifier
            .width(265.dp)
            .clickable { onCardClick() }
    ) {
        Column {
            // Header con degradado temático, categoría y badge de descuento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .background(bannerGradient)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = promotion.discountTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = "$categoryEmoji ${promotion.category}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f)),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "🪙 Ahorro ~S/ ${"%.2f".format(promotion.estimatedSavingsSoles)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = promotion.partnerName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = promotion.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = promotion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onActivate,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Activar beneficio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tarjeta horizontal de "📍 Cerca de tu campus" con indicador de caminata a pie y precios contrastados.
 */
@Composable
private fun NearbyPromoCard(
    promotion: Promotion,
    onCardClick: () -> Unit = {},
    onRedeem: () -> Unit
) {
    val tagColor = when {
        promotion.category.contains("Comida", ignoreCase = true) -> CampusTagFood
        promotion.category.contains("Tech", ignoreCase = true) -> CampusTagTech
        promotion.category.contains("Transporte", ignoreCase = true) -> CampusTagTransport
        else -> CampusTagCulture
    }

    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        modifier = Modifier
            .width(255.dp)
            .clickable { onCardClick() }
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
                    color = tagColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = promotion.discountTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tagColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🚶", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = promotion.distanceText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = promotion.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = promotion.partnerName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = promotion.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (promotion.originalPrice.isNotBlank()) {
                        Text(
                            text = promotion.originalPrice,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        text = promotion.discountedPrice.ifBlank { "S/ 12.90" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onRedeem,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Canjear", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tarjeta individual de "💻 Beneficios Virtuales & Tech" con estética para desarrolladores y estudiantes.
 */
@Composable
private fun DigitalBenefitCard(
    promotion: Promotion,
    onCardClick: () -> Unit = {},
    onClaim: () -> Unit
) {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, CampusTagTech.copy(alpha = 0.30f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
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
                    shape = RoundedCornerShape(8.dp),
                    color = CampusTagTech.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = promotion.discountTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CampusTagTech,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = promotion.distanceText.ifBlank { "Licencia Estudiantil" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CampusTagTech.copy(alpha = 0.12f))
                        .border(1.dp, CampusTagTech.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "💻", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = promotion.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = promotion.partnerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = promotion.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClaim,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("Reclamar licencia", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta para resultados de búsqueda o catálogo general.
 */
@Composable
private fun DetailedPromotionCard(
    promotion: Promotion,
    onCardClick: () -> Unit = {},
    onOpenUrl: () -> Unit
) {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
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
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = promotion.discountTag,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "🪙 Ahorras ~S/ ${"%.2f".format(promotion.estimatedSavingsSoles)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = promotion.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = promotion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onOpenUrl,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Ir al beneficio", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * ModalBottomSheet con el detalle completo de la promoción e instrucciones claras de canje.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromotionDetailSheet(
    promotion: Promotion,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onNavigateToMap: () -> Unit,
    modifier: Modifier = Modifier,
    onRedeemCoupon: (Promotion) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = CampusShapes.large,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CampusShapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = promotion.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = CampusShapes.small,
                    color = Color(0xFFFF6D00)
                ) {
                    Text(
                        text = promotion.discountTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = promotion.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Aliado: ${promotion.partnerName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = promotion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tarjeta de requisitos y canje
            Surface(
                shape = CampusShapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "📋 Instrucciones de canje:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("1. Presenta tu carnet universitario físico o digital.", style = MaterialTheme.typography.bodySmall)
                    Text("2. En convenios web, regístrate con tu correo .edu.pe institucional.", style = MaterialTheme.typography.bodySmall)
                    Text("3. El beneficio aplica según disponibilidad del establecimiento.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Botón Principal: Canjear cupón y generar token
            Button(
                onClick = {
                    onRedeemCoupon(promotion)
                },
                shape = CampusShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("🎟️ Canjear Cupón y Generar Token", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenUrl(promotion.url)
                },
                shape = CampusShapes.small,
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ir al beneficio oficial", fontWeight = FontWeight.Bold)
            }

            if (promotion.category.contains("Comida", ignoreCase = true) || promotion.category.contains("Café", ignoreCase = true)) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToMap()
                    },
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver locales aliados en radar de 30 km")
                }
            }
        }
    }
}

/**
 * Tarjeta informativa destacada visible únicamente cuando se ingresa con el Bypass de Desarrollador.
 */
@Composable
fun DevModeNoticeCard() {
    Card(
        shape = CampusShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🛠️", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MODO DESARROLLADOR (VISTA GENÉRICA)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sesión de testing de laboratorio activa. En este modo puedes realizar escaneos ilimitados y comprobar la arquitectura sin vincular una cuenta estudiantil personal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
            )
        }
    }
}
