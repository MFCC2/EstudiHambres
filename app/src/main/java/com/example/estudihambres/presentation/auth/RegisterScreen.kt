package com.example.estudihambres.presentation.auth

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.estudihambres.core.theme.CampusShapes
import com.example.estudihambres.domain.usecase.ParseStudentCardOcrUseCase
import com.example.estudihambres.domain.usecase.ValidateStudentCredentialsUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Listado oficial de sugerencias universitarias peruanas para autocompletado en el registro.
 */
private val peruvianUniversities = listOf(
    "Universidad Continental",
    "Pontificia Universidad Católica del Perú (PUCP)",
    "Universidad Nacional Mayor de San Marcos (UNMSM)",
    "Universidad Nacional de Ingeniería (UNI)",
    "Universidad Peruana de Ciencias Aplicadas (UPC)",
    "Universidad de Ingeniería y Tecnología (UTEC)",
    "Universidad de San Martín de Porres (USMP)",
    "Universidad Católica de Santa María (UCSM)",
    "Universidad Nacional de San Agustín (UNSA)",
    "Universidad Privada San Juan Bautista",
    "Universidad Nacional San Antonio Abad del Cusco (UNSAAC)",
    "Universidad Nacional de Trujillo (UNT)",
    "Universidad Tecnológica del Perú (UTP)",
    "Universidad Privada del Norte (UPN)"
)

private val popularUniversityChips = listOf(
    "Continental",
    "PUCP",
    "UNMSM",
    "UNI",
    "UNSAAC",
    "San Juan Bautista"
)

/**
 * Pantalla de registro estudiantil con diseño profesional sobrio (Blanco y Negro con acentos selectos).
 *
 * Flujo optimizado según especificaciones:
 * - Modo 1 ("Escanear Carnet"): Llama directamente al escáner de carnet con la cámara del dispositivo.
 *   Los campos de llenado manual NO aparecen abajo mientras no se haya escaneado el carnet.
 *   Una vez escaneado y validado por SUNEDU, se presentan los datos extraídos para confirmar email y contraseña.
 * - Modo 2 ("Ingreso Manual"): Permite el llenado manual si el estudiante no porta el carnet.
 *   Al presionar "Crear Cuenta y Continuar", se despliega un diálogo de advertencia indicando que
 *   deberá escanear su carnet posteriormente para acceder a los descuentos y beneficios.
 *
 * @param viewModel ViewModel de autenticación.
 * @param onRegisterSuccess Navegación tras registrar la cuenta.
 * @param onNavigateToLogin Retorno a la pantalla de inicio de sesión.
 * @param modifier Modificador Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.registerState.collectAsState()
    val context = LocalContext.current
    var universityDropdownExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isOcrProcessing by remember { mutableStateOf(false) }
    var showManualPendingWarningDialog by remember { mutableStateOf(false) }

    val parser = remember { ParseStudentCardOcrUseCase() }
    val validator = remember { ValidateStudentCredentialsUseCase() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Launcher de cámara para tomar foto directa del carnet y ejecutar OCR instantáneo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isOcrProcessing = true
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    isOcrProcessing = false
                    val parsed = parser(visionText.text)
                    if (parsed.isVerified) {
                        viewModel.onCarnetScannedInRegister(parsed)
                        Toast.makeText(context, "¡Carnet SUNEDU validado! Datos completados con éxito.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "No se detectaron los sellos oficiales de SUNEDU. Por favor enfoca nítidamente el anverso de tu carnet.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { error ->
                    isOcrProcessing = false
                    Toast.makeText(context, "Error al procesar carnet: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para escanear el carnet universitario.", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCarnetScanner: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Diálogo de aviso cuando se registra con ingreso manual
    if (showManualPendingWarningDialog) {
        AlertDialog(
            onDismissRequest = { showManualPendingWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Carnet Universitario Pendiente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Has seleccionado registrarte de forma manual.\n\n" +
                            "Ten en cuenta que tu cuenta quedará en estado PENDIENTE DE VERIFICACIÓN. " +
                            "Para poder acceder a los descuentos exclusivos de comercios aliados, canjear cupones y recibir los +500 StudiPuntos de bienvenida, deberás escanear tu carnet universitario oficial SUNEDU.\n\n" +
                            "¿Deseas continuar con el registro o prefieres escanear tu carnet ahora con la cámara?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualPendingWarningDialog = false
                        viewModel.register(onRegisterSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Continuar de todos modos")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showManualPendingWarningDialog = false
                        viewModel.setRegistrationMode(RegistrationMode.SCANNER)
                        launchCarnetScanner()
                    }
                ) {
                    Text("Escanear Carnet Ahora")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = CampusShapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header institucional
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "REGISTRO ESTUDIANTIL · SUNEDU 1:1",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Crear Cuenta Estudiantil",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Acredita tu carnet universitario para acceder a descuentos reales y +500 StudiPuntos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selector de modo de registro: Escanear Carnet vs Ingreso Manual
                PrimaryTabRow(
                    selectedTabIndex = if (state.registrationMode == RegistrationMode.SCANNER) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = state.registrationMode == RegistrationMode.SCANNER,
                        onClick = { viewModel.setRegistrationMode(RegistrationMode.SCANNER) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Escanear Carnet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                    Tab(
                        selected = state.registrationMode == RegistrationMode.MANUAL,
                        onClick = { viewModel.setRegistrationMode(RegistrationMode.MANUAL) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ingreso Manual", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    )
                }

                // =======================================================
                // CASO 1: MODO ESCANEAR CARNET
                // =======================================================
                if (state.registrationMode == RegistrationMode.SCANNER) {
                    Card(
                        shape = CampusShapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(
                            1.dp,
                            if (state.isCardScanned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (state.isCardScanned) Icons.Default.CheckCircle else Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = if (state.isCardScanned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.isCardScanned) "Carnet Oficial SUNEDU Verificado ✓" else "Escáner de Carnet Universitario",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (state.isCardScanned) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (state.isCardScanned) "100% VÁLIDO" else "RECOMENDADO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.isCardScanned) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            if (!state.isCardScanned) {
                                Text(
                                    text = "Enfoca el anverso de tu carnet universitario oficial SUNEDU. La cámara detectará y validará automáticamente tu DNI, código, nombres y universidad para evitar errores de tipeo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = launchCarnetScanner,
                                    enabled = !isOcrProcessing,
                                    shape = CampusShapes.small,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    if (isOcrProcessing) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Escaneando carnet con OCR...", style = MaterialTheme.typography.labelMedium)
                                    } else {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Escanear Carnet con Cámara", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = CampusShapes.small,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Reconocimiento seguro: Tus datos se completarán automáticamente una vez que escanees tu carnet.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                // Desglose de campos extraídos y validados
                                state.scannedCardResult?.let { card ->
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Estudiante:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text(card.studentName ?: state.fullName, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("DNI extraído:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text("${card.studentDni ?: state.dni} ✓", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Universidad:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text(card.universityName ?: state.university, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Código:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text(card.studentCode ?: state.studentCode, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Facultad / Carrera:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text("${card.faculty ?: "Facultad"} • ${card.career ?: "Carrera"}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Sello SUNEDU:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text("Verificado ✓", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = launchCarnetScanner,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Volver a escanear carnet", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // En modo escáner, si aún no escaneó, NO mostrar los campos de texto abajo
                    if (!state.isCardScanned) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "¿No tienes tu carnet físico a la mano?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { viewModel.setRegistrationMode(RegistrationMode.MANUAL) }) {
                                Text(
                                    text = "Llenar datos manualmente",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        // Una vez escaneado, mostrar campos para completar la cuenta (Correo y Contraseña)
                        Surface(
                            shape = CampusShapes.small,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Datos de carnet completados. Ingresa tu correo institucional y contraseña para activar tu cuenta con +500 StudiPuntos:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Campo Correo Institucional
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = { viewModel.onRegisterEmailChange(it) },
                            label = { Text("Correo institucional (.edu.pe)") },
                            placeholder = { Text("alumno@continental.edu.pe") },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                            isError = state.emailError != null,
                            supportingText = {
                                state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } ?: Text("Debe ser institucional (.edu / .pe)", style = MaterialTheme.typography.bodySmall)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = CampusShapes.small,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Campo Contraseña
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { viewModel.onRegisterPasswordChange(it) },
                            label = { Text("Contraseña Segura") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = state.passwordError != null,
                            supportingText = {
                                state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } ?: Text("Mínimo 6 caracteres", style = MaterialTheme.typography.bodySmall)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.register(onRegisterSuccess) }
                            ),
                            shape = CampusShapes.small,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (state.errorMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                shape = CampusShapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ ${state.errorMessage}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Botón de registro con carnet verificado
                        Button(
                            onClick = { viewModel.register(onRegisterSuccess) },
                            enabled = !state.isLoading,
                            shape = CampusShapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Activando cuenta...")
                            } else {
                                Text("Crear Cuenta y Activar VIP (+500 pts)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // =======================================================
                // CASO 2: MODO INGRESO MANUAL (SI NO PORTA EL CARNET)
                // =======================================================
                if (state.registrationMode == RegistrationMode.MANUAL) {
                    Card(
                        shape = CampusShapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ingreso manual: Tu cuenta se creará en estado pendiente. Deberás escanear tu carnet más adelante para desbloquear los cupones y beneficios.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Campo Nombre Completo
                    OutlinedTextField(
                        value = state.fullName,
                        onValueChange = { viewModel.onRegisterFullNameChange(it) },
                        label = { Text("Nombre Completo (según carnet / DNI)") },
                        placeholder = { Text("Ej. Rodrigo Mendoza Peña") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = CampusShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo DNI con validación en tiempo real y contador 8 dígitos
                    OutlinedTextField(
                        value = state.dni,
                        onValueChange = { viewModel.onRegisterDniChange(it) },
                        label = { Text("DNI (8 dígitos)") },
                        placeholder = { Text("Ej. 70809010") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        trailingIcon = {
                            if (state.dni.length == 8) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "DNI Completo",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        },
                        isError = state.dniError != null,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.dniError ?: "Exactamente 8 dígitos numéricos",
                                    color = if (state.dniError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${state.dni.length}/8",
                                    color = if (state.dni.length == 8) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = CampusShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo Código de Alumno
                    OutlinedTextField(
                        value = state.studentCode,
                        onValueChange = { viewModel.onRegisterStudentCodeChange(it) },
                        label = { Text("Código de Alumno Universitario") },
                        placeholder = { Text("Ej. 74829103 o 0020186238") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        isError = state.studentCodeError != null,
                        supportingText = {
                            state.studentCodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = CampusShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sugerencias de universidad
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sugerencias frecuentes:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            popularUniversityChips.forEach { chipName ->
                                val fullUni = peruvianUniversities.firstOrNull { it.contains(chipName, ignoreCase = true) } ?: chipName
                                FilterChip(
                                    selected = state.university.contains(chipName, ignoreCase = true),
                                    onClick = { viewModel.onRegisterUniversityChange(fullUni) },
                                    label = { Text(chipName, style = MaterialTheme.typography.labelSmall) },
                                    shape = CampusShapes.small
                                )
                            }
                        }
                    }

                    // Campo Universidad con Autocomplete
                    val filteredUniversities = remember(state.university) {
                        if (state.university.isBlank()) {
                            peruvianUniversities
                        } else {
                            peruvianUniversities.filter {
                                it.contains(state.university, ignoreCase = true)
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = universityDropdownExpanded,
                        onExpandedChange = { universityDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = state.university,
                            onValueChange = {
                                viewModel.onRegisterUniversityChange(it)
                                universityDropdownExpanded = true
                            },
                            label = { Text("Universidad / Instituto") },
                            placeholder = { Text("Ej. Universidad Continental, PUCP, UNMSM") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = universityDropdownExpanded)
                            },
                            isError = state.universityError != null,
                            supportingText = {
                                state.universityError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = CampusShapes.small,
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                                .fillMaxWidth()
                        )

                        if (filteredUniversities.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = universityDropdownExpanded,
                                onDismissRequest = { universityDropdownExpanded = false }
                            ) {
                                filteredUniversities.take(6).forEach { uni ->
                                    DropdownMenuItem(
                                        text = { Text(uni) },
                                        onClick = {
                                            viewModel.onRegisterUniversityChange(uni)
                                            universityDropdownExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }
                    }

                    // Campo Correo Institucional
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.onRegisterEmailChange(it) },
                        label = { Text("Correo institucional (.edu.pe)") },
                        placeholder = { Text("alumno@continental.edu.pe") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        isError = state.emailError != null,
                        supportingText = {
                            state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } ?: Text("Debe ser institucional (.edu / .pe)", style = MaterialTheme.typography.bodySmall)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = CampusShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo Contraseña
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.onRegisterPasswordChange(it) },
                        label = { Text("Contraseña Segura") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = state.passwordError != null,
                        supportingText = {
                            state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) } ?: Text("Mínimo 6 caracteres", style = MaterialTheme.typography.bodySmall)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val cleanDni = state.dni.trim()
                                val cleanCode = state.studentCode.trim()
                                val cleanUni = state.university.trim()
                                val cleanEmail = state.email.trim()
                                val cleanPass = state.password
                                val dniVal = validator.validateDni(cleanDni)
                                val emailVal = validator.validateUniversityEmail(cleanEmail)
                                val passVal = validator.validatePassword(cleanPass)

                                if (dniVal.isValid && emailVal.isValid && passVal.isValid && cleanCode.isNotBlank() && cleanUni.isNotBlank()) {
                                    showManualPendingWarningDialog = true
                                } else {
                                    viewModel.register(onRegisterSuccess)
                                }
                            }
                        ),
                        shape = CampusShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            shape = CampusShapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ ${state.errorMessage}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Botón principal de registro en modo manual con diálogo de advertencia
                    Button(
                        onClick = {
                            val cleanDni = state.dni.trim()
                            val cleanCode = state.studentCode.trim()
                            val cleanUni = state.university.trim()
                            val cleanEmail = state.email.trim()
                            val cleanPass = state.password
                            val dniVal = validator.validateDni(cleanDni)
                            val emailVal = validator.validateUniversityEmail(cleanEmail)
                            val passVal = validator.validatePassword(cleanPass)

                            if (dniVal.isValid && emailVal.isValid && passVal.isValid && cleanCode.isNotBlank() && cleanUni.isNotBlank()) {
                                showManualPendingWarningDialog = true
                            } else {
                                viewModel.register(onRegisterSuccess)
                            }
                        },
                        enabled = !state.isLoading,
                        shape = CampusShapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creando cuenta...")
                        } else {
                            Text("Crear Cuenta y Continuar", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes una cuenta?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
