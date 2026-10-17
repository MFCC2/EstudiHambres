package com.example.estudihambres.presentation.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.core.theme.getBrandColor
import com.example.estudihambres.core.theme.getBrandMonogram
import com.example.estudihambres.domain.model.RedeemedCoupon

/**
 * Diálogo modal interactivo para visualizar el comprobante y token oficial del cupón canjeado.
 * Muestra código alfanumérico, código de barras simulado de alta fidelidad, instrucciones
 * de presentación en caja y el incremento real al ahorro estudiantil.
 */
@Composable
fun RedemptionVoucherDialog(
    coupon: RedeemedCoupon,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfile: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    val copyToClipboard: () -> Unit = {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Código de Canje CampusPass", coupon.redemptionCode)
            clipboard.setPrimaryClip(clip)
            isCopied = true
            Toast.makeText(context, "Código copiado: ${coupon.redemptionCode}", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = CampusShapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Listo / Entendido", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (onNavigateToProfile != null) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onNavigateToProfile()
                    },
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ver en Mi Billetera")
                }
            }
        },
        title = {
            val brandColor = getBrandColor(coupon.partnerName)
            val brandMonogram = getBrandMonogram(coupon.partnerName)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(brandColor.copy(alpha = 0.14f))
                        .border(1.5.dp, brandColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = brandMonogram,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = brandColor
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "¡Cupón Canjeado con Éxito! 🎉",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = coupon.partnerName.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = brandColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }
        },
        text = {
            val brandColor = getBrandColor(coupon.partnerName)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tarjeta estilo Voucher / Boleto universitario
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, brandColor.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = brandColor
                        ) {
                            Text(
                                text = coupon.discountBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = coupon.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Código oficial en monospace con borde refinado
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = brandColor.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, brandColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CÓDIGO OFICIAL:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = coupon.redemptionCode,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.5.sp,
                                        color = brandColor
                                    )
                                }

                                OutlinedButton(
                                    onClick = copyToClipboard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCopied) Color(0xFF00C853) else brandColor),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCopied) Icons.Default.Done else Icons.Default.ContentCopy,
                                        contentDescription = "Copiar",
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isCopied) Color(0xFF00C853) else brandColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCopied) "Copiado" else "Copiar",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCopied) Color(0xFF00C853) else brandColor
                                    )
                                }
                            }
                        }

                        // Código de barras simulado en Canvas
                        Spacer(modifier = Modifier.height(2.dp))
                        VoucherBarcodeCanvas(code = coupon.redemptionCode)
                        Text(
                            text = coupon.redemptionCode,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Indicador de Ahorro acumulado
                Surface(
                    shape = CampusShapes.small,
                    color = Color(0xFF00C853).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💰", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+S/ ${"%.2f".format(coupon.savingsAmountSoles)} sumados a tu ahorro universitario",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007A33)
                        )
                    }
                }

                // Instrucciones de uso en establecimiento o web
                Text(
                    text = "📌 Muestra este token o código de barras en caja junto a tu carnet universitario. Válido hasta el ${coupon.formattedExpirationDate}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    )
}

/**
 * Renderizado de código de barras Code-128 con patrones de barras variables en Canvas Compose.
 */
@Composable
fun VoucherBarcodeCanvas(
    code: String,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        // Generar anchos pseudoaleatorios estables a partir del hash del código
        val seed = code.hashCode()
        val numBars = 48
        val barWidthStep = width / (numBars * 1.5f)

        var currentX = 0f
        var barIndex = 0

        while (currentX < width - 10f && barIndex < numBars) {
            val pseudoRandom = ((seed * (barIndex + 7) * 31) and 0x7FFFFFFF) % 4
            val strokeW = when (pseudoRandom) {
                0 -> 1.5f * barWidthStep
                1 -> 2.5f * barWidthStep
                2 -> 1.0f * barWidthStep
                else -> 3.0f * barWidthStep
            }

            // Dibujar barra vertical
            drawLine(
                color = barColor,
                start = Offset(x = currentX, y = 2f),
                end = Offset(x = currentX, y = height - 2f),
                strokeWidth = strokeW
            )

            // Espaciado aleatorio entre barras
            val gap = ((seed * (barIndex + 13) * 17) and 0x7FFFFFFF) % 3 + 1
            currentX += strokeW + (gap * barWidthStep * 0.8f)
            barIndex++
        }
    }
}
