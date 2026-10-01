package com.example.estudihambres.presentation.verification

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.model.VerificationStatus

/**
 * Pantalla para la validación del carnet universitario mediante Google ML Kit OCR (presentation/verification).
 *
 * Ofrece captura segura con cámara (verificando permisos de tiempo de ejecución), selección de imagen
 * desde galería con GetContent, simulación para emuladores y el botón explícito "Omitir por ahora".
 *
 * @param viewModel ViewModel encargado del procesamiento OCR y persistencia.
 * @param onVerificationCompleted Navegación a Home tras verificar o continuar.
 * @param onSkipClick Navegación a Home asignando estado PENDING_VERIFICATION.
 * @param modifier Modificador Compose.
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

    // Lanzador para capturar foto directamente con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.processImageBitmap(bitmap)
        }
    }

    // Lanzador para solicitar permiso de cámara en tiempo de ejecución
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al iniciar la cámara: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Permiso de cámara no concedido. Puedes cargar la foto desde la galería.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Función segura para invocar la cámara verificando permisos y atrapando excepciones
    fun launchCameraSafely() {
        try {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo acceder a la cámara: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador para seleccionar imagen desde la galería del dispositivo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    viewModel.processImageBitmap(bitmap)
                } else {
                    Toast.makeText(context, "No se pudo decodificar la imagen seleccionada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar imagen: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Verifica tu Carnet Universitario",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Apunta la cámara a tu carnet vigente. Nuestro OCR buscará sellos de SUNEDU y datos de tu universidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Visor o contenedor guía para la foto
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = CampusShapes.medium
                )
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = CampusShapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            if (state.isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Analizando texto con ML Kit...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = "Ubica el carnet dentro del marco",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Botones de acción fotográfica y carga de archivos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { launchCameraSafely() },
                shape = CampusShapes.small,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text("Tomar Foto")
            }
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
                Text("Galería")
            }
        }

        OutlinedButton(
            onClick = { viewModel.simulateCardScan() },
            shape = CampusShapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DocumentScanner, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text("Simular Carnet (Demo / Emulador)")
        }

        // Resultados del análisis OCR
        state.ocrResult?.let { result ->
            Card(
                shape = CampusShapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isVerified) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (result.isVerified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = if (result.isVerified) "¡Carnet Validado con Éxito!" else "No se pudo verificar el carnet",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (result.isVerified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Sello SUNEDU: ${if (result.hasSuneduKeyword) "Detectado ✓" else "No detectado"}")
                    Text("• Universidad: ${result.universityName ?: if (result.hasUniversityKeyword) "Detectada ✓" else "No identificada"}")
                    result.validityYearDetected?.let {
                        Text("• Año de Vigencia: $it ✓")
                    }
                }
            }
        }

        // Si ya está verificado, botón principal para continuar
        if (state.verificationStatus == VerificationStatus.VERIFIED) {
            Button(
                onClick = onVerificationCompleted,
                shape = CampusShapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir a Beneficios (VERIFIED)")
            }
        }

        // Botón explícito: "Omitir por ahora" asignando PENDING_VERIFICATION
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
