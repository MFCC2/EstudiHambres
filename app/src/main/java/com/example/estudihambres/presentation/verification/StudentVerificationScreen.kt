package com.example.estudihambres.presentation.verification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.model.StudentCardOcrResult
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.usecase.ParseStudentCardOcrUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executors

/**
 * Pantalla de verificación de carnet universitario con cámara embebida (CameraX) y escaneo OCR automático (ML Kit).
 *
 * Muestra el visor de la cámara directamente dentro del recuadro en pantalla. En cuanto el estudiante
 * encuadra su carnet dentro del marco, se analiza y captura de forma automática, validando sellos SUNEDU,
 * universidad, nombres y vigencia.
 */
@Composable
fun StudentVerificationScreen(
    onVerificationCompleted: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentVerificationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Se requiere permiso de cámara para el escáner embebido. Puedes subir una foto desde la galería.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Selector para cargar foto directamente desde la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    viewModel.processImageBitmap(bitmap)
                } else {
                    Toast.makeText(context, "No se pudo leer la foto de la galería", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir imagen: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Tarjeta informativa de cabecera
        Card(
            shape = CampusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Verifica tu Carnet Universitario",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enfoca tu carnet en el recuadro. El escáner detectará SUNEDU y tus datos de forma rápida y automática.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Guía interactiva de consejos para un escaneo exitoso
        var showTips by remember { mutableStateOf(false) }
        Surface(
            shape = CampusShapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTips = !showTips }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Consejos para un escaneo exitoso",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = if (showTips) "Ocultar ▲" else "Ver tips ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (showTips) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Coloca tu carnet oficial SUNEDU sobre una superficie plana.\n" +
                                "• Asegúrate de tener buena luz y evita reflejos sobre el texto.\n" +
                                "• Mantén la cámara fija dentro del marco mientras avanza la barra de verificación.\n" +
                                "• Evita comprobantes de pago o boletas (serán rechazados automáticamente).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Error banner si la validación falla o si ya tiene un carnet vinculado
        state.errorMessage?.let { error ->
            Card(
                shape = CampusShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // 2. Visor Embebido con CameraX en el recuadro guiado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(CampusShapes.medium)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (state.verificationStatus == VerificationStatus.VERIFIED) {
                // Estado Verificado: Visualización de tarjeta de éxito
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¡Carnet Validado con Éxito!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.ocrResult?.studentName ?: "Estudiante Universitario",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = state.ocrResult?.universityName ?: "Universidad Continental",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (capturedBitmap != null && state.isProcessing) {
                // Visualización de foto estática mientras se procesa
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Carnet Capturado",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Analizando texto y sellos oficiales...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (hasCameraPermission) {
                // CÁMARA EMBEBIDA ACTIVA (CameraX) con análisis en tiempo real y estabilización multi-frame
                EmbeddedCameraCardScanner(
                    onCardDetected = { verifiedResult ->
                        viewModel.onCardScannedSuccessfully(verifiedResult)
                    },
                    onManualCapture = { bitmap ->
                        capturedBitmap = bitmap
                        viewModel.processImageBitmap(bitmap)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Solicitud de permiso de cámara si no ha sido otorgado
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Permiso de cámara requerido para escanear dentro del recuadro",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = CampusShapes.small
                    ) {
                        Text("Activar Cámara")
                    }
                }
            }
        }

        // 3. Botones complementarios (Galería y Simulación)
        if (state.verificationStatus != VerificationStatus.VERIFIED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al abrir galería: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = CampusShapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Subir de Galería")
                }

                OutlinedButton(
                    onClick = { viewModel.simulateCardScan() },
                    shape = CampusShapes.small,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Simular Carnet")
                }
            }
        }

        // 4. Panel de Resultados Detallados tras análisis OCR
        state.ocrResult?.let { result ->
            Card(
                shape = CampusShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isVerified) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (result.isVerified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (result.isVerified) "¡Carnet Validado con Éxito!" else "Datos Detectados",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (result.isVerified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    result.studentName?.let {
                        Text("• Estudiante: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    result.studentDni?.let {
                        Text("• DNI / Código: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("• Universidad: ${result.universityName ?: if (result.hasUniversityKeyword) "Detectada ✓" else "Pendiente"}")
                    Text("• Sello SUNEDU / Institucional: ${if (result.hasSuneduKeyword) "Detectado ✓" else "Pendiente"}")
                    result.validityYearDetected?.let {
                        Text("• Año de Vigencia: $it ✓")
                    }

                    if (!result.isVerified) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.confirmManualVerification(onVerificationCompleted)
                            },
                            shape = CampusShapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aprobar Carnet con Texto Detectado")
                        }
                    }
                }
            }
        }

        // 5. Botón principal cuando ya está VERIFIED
        if (state.verificationStatus == VerificationStatus.VERIFIED) {
            Button(
                onClick = onVerificationCompleted,
                shape = CampusShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir a Beneficios (VERIFIED)")
            }

            val session = com.example.estudihambres.core.util.SessionManager.getInstanceOrNull()
            val isDev = session?.getUserSession()?.isDev ?: false
            if (isDev) {
                OutlinedButton(
                    onClick = {
                        capturedBitmap = null
                        viewModel.resetVerification()
                    },
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("🛠️ Escanear Otro Carnet (Modo Dev)")
                }
            } else {
                Surface(
                    shape = CampusShapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Carnet único vinculado permanentemente a tu cuenta de estudiante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 6. Botón de bypass explícito
        OutlinedButton(
            onClick = {
                viewModel.skipVerification(onSkipClick)
            },
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

/**
 * Componente que aloja el visor de CameraX con análisis continuo y estabilización multi-frame.
 * Requiere confirmación sostenida de sellos SUNEDU y datos institucionales durante 1.5 - 2s para evitar falsos positivos.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun EmbeddedCameraCardScanner(
    onCardDetected: (StudentCardOcrResult) -> Unit,
    onManualCapture: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isScanningActive by remember { mutableStateOf(true) }
    var lastAnalyzedMs by remember { mutableLongStateOf(0L) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    var consecutiveValidFrames by remember { mutableIntStateOf(0) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanStatusMessage by remember { mutableStateOf("Enfoca el carnet universitario oficial dentro del recuadro") }

    val parser = remember { ParseStudentCardOcrUseCase() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
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
                        // Throttling: analizar un frame cada 350 ms para permitir estabilización y control térmico
                        if (!isScanningActive || now - lastAnalyzedMs < 350) {
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
                                            val progress = (consecutiveValidFrames / 4f).coerceAtMost(1f)
                                            val msg = when (consecutiveValidFrames) {
                                                1 -> "Sello institucional detectado (25%). Mantén firme..."
                                                2 -> "Universidad y código identificados (50%). Verificando..."
                                                3 -> "Validando sello oficial de SUNEDU (75%)..."
                                                else -> "¡Carnet verificado correctamente! (100%)"
                                            }
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                scanProgress = progress
                                                scanStatusMessage = msg
                                            }

                                            // Requiere 4 frames consecutivos verificados (~1.5s sostenido)
                                            if (consecutiveValidFrames >= 4) {
                                                isScanningActive = false
                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    onCardDetected(parsed)
                                                }
                                            }
                                        } else {
                                            consecutiveValidFrames = 0
                                            val rawUpper = visionText.text.uppercase()
                                            val isCommercial = rawUpper.contains("BOLETA") ||
                                                    rawUpper.contains("FACTURA") ||
                                                    rawUpper.contains("PANADOL") ||
                                                    rawUpper.contains("RUC") ||
                                                    rawUpper.contains("FARMACIA")

                                            val msg = if (isCommercial) {
                                                "Comprobante no válido. Requiere carnet con sello SUNEDU"
                                            } else {
                                                "Buscando carnet oficial... Alinea el sello SUNEDU"
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
                                            scanStatusMessage = "Enfoca el carnet universitario oficial dentro del recuadro"
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

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
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

        // Overlay con marco de carnet, barra de progreso y HUD de feedback
        CardScannerOverlay(
            progress = scanProgress,
            statusText = scanStatusMessage,
            modifier = Modifier.fillMaxSize(),
            onCaptureClick = {
                val capture = imageCapture ?: return@CardScannerOverlay
                val tempFile = File(context.cacheDir, "scan_manual_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                            if (bitmap != null) {
                                onManualCapture(bitmap)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(context, "Error al capturar: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        )
    }
}

/**
 * Superposición visual que dibuja el recuadro guía, la barra de progreso de estabilización y el estado.
 */
@Composable
fun CardScannerOverlay(
    progress: Float,
    statusText: String,
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit
) {
    Box(modifier = modifier) {
        val borderColor = if (progress > 0.5f) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        }

        // Marco guía delimitador para carnet tipo ID-1
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

        // Etiqueta superior indicadora con feedback dinámico
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = CampusShapes.small
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            if (progress > 0f) {
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        // Botón rápido para forzar captura manual si la iluminación es tenue
        Button(
            onClick = onCaptureClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .height(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Capturar",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Capturar Ahora", style = MaterialTheme.typography.labelSmall)
        }
    }
}
