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
import androidx.compose.foundation.layout.height
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
    viewModel: StudentVerificationViewModel = viewModel(),
    onVerificationCompleted: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
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

        // 2. Visor Embebido con CameraX en el recuadro guiado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
                            text = "Analizando texto con ML Kit...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (hasCameraPermission) {
                // CÁMARA EMBEBIDA ACTIVA (CameraX) con análisis en tiempo real
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
                Text("Escanear Otro Carnet")
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
 * Componente que aloja el visor de CameraX con análisis en tiempo real en segundo plano.
 * Identifica patrones de carnet universitario de forma automática y rápida.
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
    var lastAnalyzedMs by remember { mutableStateOf(0L) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

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
                        // Throttling: analizar un frame cada 300 ms para máxima fluidez y bajo consumo
                        if (!isScanningActive || now - lastAnalyzedMs < 300) {
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
                                            isScanningActive = false
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onCardDetected(parsed)
                                            }
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

        // Overlay con marco de carnet y esquinas destacadas
        CardScannerOverlay(
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
 * Superposición visual que dibuja el recuadro guía y el botón rápido de captura manual.
 */
@Composable
fun CardScannerOverlay(
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit
) {
    Box(modifier = modifier) {
        // Marco guía delimitador para carnet tipo ID-1
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // Etiqueta superior indicadora
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = CampusShapes.small,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            Text(
                text = "Enmarca tu carnet aquí • Escaneo inteligente activo",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
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
