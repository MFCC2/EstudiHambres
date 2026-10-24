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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.model.StudentCardOcrResult
import com.example.estudihambres.domain.model.VerificationStatus
import com.example.estudihambres.domain.usecase.ParseStudentCardOcrUseCase
import com.example.estudihambres.presentation.common.LiveCarnetScannerView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executors

/**
 * Pantalla de verificación de carnet universitario con diseño sobrio y profesional (Blanco y Negro).
 *
 * Mejoras implementadas:
 * - Ocultación total de la barra inferior de navegación para evitar brechas de seguridad.
 * - Reconocimiento y visualización detallada de los 9 datos clave de SUNEDU:
 *   1. Código de estudiante.
 *   2. DNI (con validación de 8 dígitos y corrección de errores de registro).
 *   3. Universidad.
 *   4. Nombres y Apellidos.
 *   5. Facultad.
 *   6. Carrera.
 *   7. Sello oficial de SUNEDU (membrete, marca de agua de seguridad guilloché y holograma).
 *   8. Año de vigencia y fecha exacta de caducidad.
 *   9. Estado del estudiante (Vigente / Regular o Postgrado).
 * - Modal para ingreso manual de datos en caso de no portar el carnet físico.
 *
 * @param onVerificationCompleted Navegación hacia Home al culminar la acreditación.
 * @param onSkipClick Navegación hacia Home en estado PENDING_VERIFICATION.
 * @param modifier Modificador Compose.
 * @param viewModel ViewModel de verificación estudiantil.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var showExplanationModal by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Se requiere permiso de cámara para el escáner. Puedes seleccionar una foto de tu galería.",
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera de navegación segura (sin barra inferior)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSkipClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Acreditación SUNEDU",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { showExplanationModal = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Información del Sello",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Tarjeta informativa minimalista
        Card(
            shape = CampusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Escaneo Oficial de Carnet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Enfoca el anverso con membrete de SUNEDU",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Error banner si la validación falla
        state.errorMessage?.let { error ->
            Card(
                shape = CampusShapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // 2. Visor Embebido con CameraX en recuadro profesional
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF09090B))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (state.verificationStatus == VerificationStatus.VERIFIED) {
                // Estado Verificado: Visualización de éxito nítida
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "¡Carnet SUNEDU Acreditado!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.ocrResult?.studentName ?: "Estudiante Universitario",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.ocrResult?.universityName ?: "Universidad Nacional",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (capturedBitmap != null && state.isProcessing) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Carnet Capturado",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Analizando sellos SUNEDU y datos...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else if (hasCameraPermission) {
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Permiso de cámara necesario para enfocar el carnet",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = CampusShapes.small
                    ) {
                        Text("Activar Cámara")
                    }
                }
            }
        }

        // 3. Acciones rápidas (Galería / Ingreso Manual / Simulación)
        if (state.verificationStatus != VerificationStatus.VERIFIED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al abrir galería: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = CampusShapes.small,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galería", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.toggleManualInputSheet(true) },
                    shape = CampusShapes.small,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ingreso Manual", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }


            }
        }

        // 4. Panel de Resultados Detallados con los 9 campos institucionales
        state.ocrResult?.let { result ->
            Card(
                shape = CampusShapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (result.isVerified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (result.isVerified) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (result.isVerified) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (result.isVerified) "Datos de Carnet Verificado" else "Datos Detectados",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (result.isVigente) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (result.isVigente) "VIGENTE ✓" else "CADUCADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (result.isVigente) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 1. Estudiante (Nombres y Apellidos)
                    CarnetDataRow("Estudiante:", result.studentName ?: "Pendiente de captura")

                    // 2. DNI
                    CarnetDataRow("DNI:", result.studentDni?.let { "$it (Validado 8 dígitos)" } ?: "No detectado")

                    // 3. Código Universitario
                    CarnetDataRow("Código:", result.studentCode ?: "No detectado")

                    // 4. Universidad
                    CarnetDataRow("Universidad:", result.universityName ?: "Universidad Nacional / Privada")

                    // 5. Facultad
                    CarnetDataRow("Facultad:", result.faculty ?: "Facultad Universitaria")

                    // 6. Carrera
                    CarnetDataRow("Carrera:", result.career ?: "Carrera Profesional")

                    // 7. Sello SUNEDU
                    CarnetDataRow(
                        "Sello SUNEDU:",
                        if (result.hasSuneduSeal || result.hasSuneduKeyword) "Verificado (Holograma + Membrete oficial)" else "Pendiente de encuadre"
                    )

                    // 8. Año de vigencia y expiración
                    CarnetDataRow(
                        "Vigencia:",
                        result.expirationDate ?: (result.validityYearDetected ?: "2026")
                    )

                    // 9. Estado de estudiante
                    CarnetDataRow("Estado:", result.studentStatus)

                    if (!result.isVerified) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.confirmManualVerification(onVerificationCompleted) },
                            shape = CampusShapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aprobar y Vincular Carnet a Cuenta")
                        }
                    }
                }
            }
        }

        // 5. Botón principal tras acreditación
        if (state.verificationStatus == VerificationStatus.VERIFIED) {
            Button(
                onClick = onVerificationCompleted,
                shape = CampusShapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continuar a StudiHambres VIP (+500 StudiPuntos)", fontWeight = FontWeight.Bold)
            }
        }

        // 6. Botón de omitir paso temporalmente
        OutlinedButton(
            onClick = { viewModel.skipVerification(onSkipClick) },
            shape = CampusShapes.small,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Omitir por ahora (Acceso Pendiente de Validación)", fontWeight = FontWeight.SemiBold)
        }
    }

    // Modal para ingreso manual de datos (si no portan carnet físico)
    if (state.showManualInputSheet) {
        ManualStudentDataBottomSheet(
            onDismiss = { viewModel.toggleManualInputSheet(false) },
            onSubmit = { name, dni, code, uni, faculty, career, yr ->
                viewModel.submitManualStudentData(
                    fullName = name,
                    dni = dni,
                    studentCode = code,
                    university = uni,
                    faculty = faculty,
                    career = career,
                    validityYear = yr,
                    onSuccess = {
                        Toast.makeText(context, "Datos guardados. Podrás escanear tu carnet oficial más adelante.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    // Diálogo informativo sobre el Sello de SUNEDU
    if (showExplanationModal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExplanationModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿Cuál es el Sello de SUNEDU?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "El Carnet Universitario oficial expedido por SUNEDU incorpora 3 elementos de autenticidad:\n\n" +
                            "1. Membrete Superior: Texto oficial 'República del Perú' y 'Superintendencia Nacional de Educación Superior Universitaria' acompañado del Escudo Nacional.\n\n" +
                            "2. Sello Holográfico e Isotipo SUNEDU: Marca de agua de seguridad y logotipo oficial impresos en tramas guilloché iridiscentes al fondo de la tarjeta.\n\n" +
                            "3. Franja Inferior Holográfica: Título 'CARNÉ UNIVERSITARIO' con el año de vigencia (ej. 2025/2026) y código de control serial alfanumérico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExplanationModal = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun CarnetDataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Bottom Sheet modal para ingreso manual de datos cuando no se porta el carnet físico.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualStudentDataBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (name: String, dni: String, code: String, uni: String, faculty: String, career: String, yr: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fullName by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var studentCode by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("Universidad Continental") }
    var faculty by remember { mutableStateOf("Ingeniería") }
    var career by remember { mutableStateOf("Ingeniería de Sistemas") }
    var validityYear by remember { mutableStateOf("2026") }

    var dniError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ingreso Manual de Datos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Text(
                text = "Ingresa tus datos institucionales tal como aparecen en tu matrícula universitaria.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // DNI con validación estricta de 8 dígitos
            OutlinedTextField(
                value = dni,
                onValueChange = {
                    if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                        dni = it
                        dniError = if (it.length == 8) null else "Debe contener 8 dígitos"
                    }
                },
                label = { Text("DNI (8 dígitos)") },
                placeholder = { Text("Ej. 70809010") },
                isError = dniError != null,
                trailingIcon = {
                    if (dni.length == 8) {
                        Icon(Icons.Default.Check, contentDescription = "Válido", tint = MaterialTheme.colorScheme.tertiary)
                    }
                },
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dniError ?: "Obligatorio", color = if (dniError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${dni.length}/8")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Nombres y Apellidos
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nombres y Apellidos") },
                placeholder = { Text("Ej. Rodrigo Mendoza Peña") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Código
            OutlinedTextField(
                value = studentCode,
                onValueChange = { studentCode = it },
                label = { Text("Código de Alumno") },
                placeholder = { Text("Ej. 20230148") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Universidad
            OutlinedTextField(
                value = university,
                onValueChange = { university = it },
                label = { Text("Universidad / Instituto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Facultad
            OutlinedTextField(
                value = faculty,
                onValueChange = { faculty = it },
                label = { Text("Facultad") },
                placeholder = { Text("Ej. Humanidades / Ingeniería / Post Grado") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Carrera
            OutlinedTextField(
                value = career,
                onValueChange = { career = it },
                label = { Text("Carrera Profesional") },
                placeholder = { Text("Ej. Comunicación / Medicina Humana") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Año de Vigencia
            OutlinedTextField(
                value = validityYear,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) validityYear = it },
                label = { Text("Año de Vigencia (Carnet)") },
                placeholder = { Text("2026") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    if (dni.length != 8) {
                        dniError = "El DNI debe tener exactamente 8 dígitos"
                        return@Button
                    }
                    onSubmit(fullName, dni, studentCode, university, faculty, career, validityYear)
                },
                enabled = dni.length == 8 && fullName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CampusShapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Guardar Datos (Pendiente de Validación)")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Componente visor de CameraX con análisis continuo y estabilización multi-frame.
 * Delega al componente unificado de producción [LiveCarnetScannerView].
 */
@Composable
fun EmbeddedCameraCardScanner(
    onCardDetected: (StudentCardOcrResult) -> Unit,
    onManualCapture: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    LiveCarnetScannerView(
        onCardDetected = onCardDetected,
        onManualCapture = onManualCapture,
        modifier = modifier
    )
}
