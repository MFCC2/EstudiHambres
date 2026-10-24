package com.example.estudihambres.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.model.StudentCardOcrResult
import com.example.estudihambres.domain.usecase.ParseStudentCardOcrUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executors

/**
 * Escáner de carnet universitario CameraX en tiempo real con estabilización multi-frame,
 * reconocimiento de sellos SUNEDU mediante Google ML Kit y retroalimentación háptica.
 *
 * Diseñado bajo el principio de Zero Data Leakage (Ley N° 29733): los cuadros de video
 * se procesan exclusivamente en memoria volátil sin persistir imágenes del estudiante.
 *
 * @param onCardDetected Callback ejecutado al validar exitosamente el carnet SUNEDU (100% verificado).
 * @param onManualCapture Callback opcional para captura fotográfica manual en ambientes de baja luminosidad.
 * @param modifier Modificador Compose aplicado al contenedor del escáner.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun LiveCarnetScannerView(
    onCardDetected: (StudentCardOcrResult) -> Unit,
    modifier: Modifier = Modifier,
    onManualCapture: ((Bitmap) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var isScanningActive by remember { mutableStateOf(true) }
    var lastAnalyzedMs by remember { mutableLongStateOf(0L) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }

    var consecutiveValidFrames by remember { mutableIntStateOf(0) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanStatusMessage by remember { mutableStateOf("Enfoca el anverso de tu carnet universitario") }

    val parser = remember { ParseStudentCardOcrUseCase() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Animación continua del haz láser de escaneo
    val infiniteTransition = rememberInfiniteTransition(label = "LaserScanCarnet")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserYProgress"
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // 1. Visor de CameraX en tiempo real
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            if (!isScanningActive || now - lastAnalyzedMs < 280) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            lastAnalyzedMs = now

                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        if (isScanningActive && visionText.text.isNotBlank()) {
                                            val parsed = parser(visionText.text)
                                            if (parsed.isVerified) {
                                                consecutiveValidFrames++
                                                val progress = (consecutiveValidFrames / 3f).coerceAtMost(1f)
                                                val msg = when (consecutiveValidFrames) {
                                                    1 -> "Membrete detectado (33%). Mantén quieto..."
                                                    2 -> "Validando código y sello SUNEDU (66%)..."
                                                    else -> "¡Carnet verificado correctamente! (100%)"
                                                }
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    scanProgress = progress
                                                    scanStatusMessage = msg
                                                }

                                                if (consecutiveValidFrames >= 3) {
                                                    isScanningActive = false
                                                    ContextCompat.getMainExecutor(ctx).execute {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onCardDetected(parsed)
                                                    }
                                                }
                                            } else {
                                                consecutiveValidFrames = 0
                                                val rawUpper = visionText.text.uppercase()
                                                val isCommercial = rawUpper.contains("BOLETA") ||
                                                        rawUpper.contains("FACTURA") ||
                                                        rawUpper.contains("RUC") ||
                                                        rawUpper.contains("TICKET")

                                                val msg = if (isCommercial) {
                                                    "Comprobante no válido. Requiere carnet con sello SUNEDU"
                                                } else {
                                                    "Alinea el membrete de tu universidad y el carnet"
                                                }
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    scanProgress = 0f
                                                    scanStatusMessage = msg
                                                }
                                            }
                                        } else if (isScanningActive) {
                                            consecutiveValidFrames = 0
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                scanProgress = 0f
                                                scanStatusMessage = "Enfoca el anverso de tu carnet universitario"
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture,
                            analysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Línea láser animada de escaneo
        if (isScanningActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * laserProgress
                val brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF00E676).copy(alpha = 0.85f),
                        Color.Transparent
                    )
                )
                drawLine(
                    brush = brush,
                    start = Offset(x = 16.dp.toPx(), y = y),
                    end = Offset(x = size.width - 16.dp.toPx(), y = y),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        // 3. Marco guía delimitador para credencial ID-1
        val borderColor = if (scanProgress > 0.6f) {
            Color(0xFF00E676)
        } else {
            Color.White.copy(alpha = 0.75f)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // 4. Panel superior de estado y avance
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scanStatusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (scanProgress > 0f) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00E676),
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }

        // 5. Botón de linterna / flash (Esquina superior derecha)
        IconButton(
            onClick = {
                val newState = !isTorchEnabled
                isTorchEnabled = newState
                camera?.cameraControl?.enableTorch(newState)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isTorchEnabled) Color(0xFFFFD600) else Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Linterna",
                        tint = if (isTorchEnabled) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 6. Botón de captura manual (opcional, en parte inferior)
        if (onManualCapture != null) {
            Button(
                onClick = {
                    val capture = imageCapture ?: return@Button
                    val tempFile = File(context.cacheDir, "scan_carnet_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                                tempFile.delete()
                                if (bitmap != null) {
                                    onManualCapture(bitmap)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Toast.makeText(context, "Error de captura: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.75f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Capturar",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Capturar foto", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
            }
        }
    }
}
