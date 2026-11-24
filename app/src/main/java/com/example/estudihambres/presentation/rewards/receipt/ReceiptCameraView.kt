package com.example.estudihambres.presentation.rewards.receipt

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Size
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.estudihambres.core.util.CameraScannerResources
import com.example.estudihambres.domain.model.ScannedReceipt
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Visor de cámara para comprobantes: detecta el QR SUNAT (o el texto impreso) y entrega
 * los datos una sola vez por detección.
 *
 * @param isScanning Cuando es falso el análisis se pausa (la vista previa sigue visible).
 * @param onDetected Comprobante detectado (hilo principal).
 * @param onPermissionDenied Se usa para ofrecer el ingreso manual sin cámara.
 */
@Composable
fun ReceiptCameraView(
    isScanning: Boolean,
    onDetected: (ScannedReceipt) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (hasPermission) {
            CameraPreview(isScanning = isScanning, onDetected = onDetected)
        } else {
            PermissionRequest(
                onRequest = { launcher.launch(Manifest.permission.CAMERA) },
                onManual = onPermissionDenied
            )
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun CameraPreview(isScanning: Boolean, onDetected: (ScannedReceipt) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnDetected by rememberUpdatedState(onDetected)

    val executorService = remember { Executors.newSingleThreadExecutor() }
    // Tras cerrar el visor, ML Kit puede completar tareas pendientes: se descartan en vez de fallar
    val safeExecutor = remember { Executor { task -> runCatching { executorService.execute(task) } } }
    val analyzer = remember { ReceiptAnalyzer(safeExecutor) { currentOnDetected(it) } }
    val boundUseCases = remember { mutableListOf<UseCase>() }
    val disposed = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning) {
        if (isScanning) analyzer.resume() else analyzer.pause()
    }

    DisposableEffect(Unit) {
        onDispose {
            disposed.set(true)
            runCatching { camera?.cameraControl?.enableTorch(false) }
            CameraScannerResources.unbind(context, boundUseCases)
            analyzer.close()
            executorService.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    if (disposed.get()) return@addListener
                    val provider = runCatching { providerFuture.get() }.getOrNull() ?: return@addListener
                    if (disposed.get()) return@addListener
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder()
                        .setResolutionSelector(
                            // 720p: suficiente para leer QR y texto de tickets sin saturar el procesador
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executorService, analyzer) }
                    runCatching {
                        if (disposed.get()) return@runCatching
                        camera = provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                        boundUseCases += listOf(preview, analysis)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                // Toque para enfocar: útil con tickets térmicos pequeños
                previewView.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                        runCatching {
                            camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                        }
                        view.performClick()
                    }
                    true
                }
                previewView
            },
            onRelease = { CameraScannerResources.unbind(context, boundUseCases) },
            modifier = Modifier.fillMaxSize()
        )

        ViewfinderOverlay(modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = {
                torchOn = !torchOn
                runCatching { camera?.cameraControl?.enableTorch(torchOn) }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f)) {
                Icon(
                    imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = if (torchOn) "Apagar linterna" else "Encender linterna",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Text(
                text = if (isScanning) "Apunta al código QR de la boleta" else "Comprobante leído",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

/** Oscurece el entorno y marca la zona de lectura con esquinas blancas. */
@Composable
private fun ViewfinderOverlay(modifier: Modifier) {
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val boxWidth = size.width * 0.72f
        val boxHeight = boxWidth * 0.8f
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f - 8.dp.toPx()
        val radius = 18.dp.toPx()

        drawRect(Color.Black.copy(alpha = 0.38f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = GeoSize(boxWidth, boxHeight),
            cornerRadius = CornerRadius(radius),
            blendMode = BlendMode.Clear
        )

        val arm = 26.dp.toPx()
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val right = left + boxWidth
        val bottom = top + boxHeight
        val corners = listOf(
            Triple(Offset(left, top + arm), Offset(left, top), Offset(left + arm, top)),
            Triple(Offset(right - arm, top), Offset(right, top), Offset(right, top + arm)),
            Triple(Offset(left, bottom - arm), Offset(left, bottom), Offset(left + arm, bottom)),
            Triple(Offset(right - arm, bottom), Offset(right, bottom), Offset(right, bottom - arm))
        )
        corners.forEach { (a, b, c) ->
            drawLine(Color.White, a, b, strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(Color.White, b, c, strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit, onManual: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Necesitamos la cámara para leer tu boleta",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onRequest) { Text("Permitir cámara") }
        androidx.compose.material3.TextButton(onClick = onManual) { Text("Escribir los datos a mano") }
    }
}
