package com.example.estudihambres.presentation.auth

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudihambres.core.theme.CampusShapes

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
    "Universidad de Lima",
    "Universidad del Pacífico",
    "Universidad San Ignacio de Loyola (USIL)",
    "Universidad Tecnológica del Perú (UTP)",
    "Universidad Ricardo Palma (URP)",
    "Universidad Científica del Sur",
    "Universidad Privada del Norte (UPN)",
    "Universidad Nacional de Trujillo (UNT)",
    "Universidad Nacional San Antonio Abad del Cusco (UNSAAC)"
)

/**
 * Universidades destacadas para selección rápida en 1 solo tap.
 */
private val popularUniversityChips = listOf(
    "Continental",
    "PUCP",
    "UNMSM",
    "UNI",
    "UTEC",
    "UNSAAC"
)

/**
 * Pantalla de registro estudiantil intuitiva con Material 3 (presentation/auth).
 * Conecta con [AuthViewModel], añade chips de selección rápida de universidad,
 * contador visual para los 8 dígitos del DNI y alternancia para visibilidad de contraseña.
 *
 * @param viewModel ViewModel de autenticación.
 * @param onRegisterSuccess Navegación tras registrar correctamente (lleva a la verificación de carnet).
 * @param onNavigateToLogin Navegación de retorno al login.
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
    var universityDropdownExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Registro Estudiantil 🎓",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Completa tus datos para activar tu perfil y acceder a beneficios universitarios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Campo DNI con contador visual y validación en tiempo real
                OutlinedTextField(
                    value = state.dni,
                    onValueChange = {
                        if (it.length <= 8 && it.all { char -> char.isDigit() }) {
                            viewModel.onRegisterDniChange(it)
                        }
                    },
                    label = { Text("DNI (8 dígitos)") },
                    placeholder = { Text("Ej. 70809010") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    trailingIcon = {
                        if (state.dni.length == 8) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "DNI Completo",
                                tint = androidx.compose.ui.graphics.Color(0xFF00C853)
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
                                text = state.dniError ?: "Debe contener exactamente 8 dígitos",
                                color = if (state.dniError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${state.dni.length}/8",
                                color = if (state.dni.length == 8) androidx.compose.ui.graphics.Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant
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
                    label = { Text("Código de Alumno") },
                    placeholder = { Text("Ej. 74829103") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = state.studentCodeError != null,
                    supportingText = {
                        state.studentCodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips de sugerencia rápida de universidades peruanas
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
                                onClick = {
                                    viewModel.onRegisterUniversityChange(fullUni)
                                },
                                label = { Text(chipName, style = MaterialTheme.typography.labelSmall) },
                                shape = CampusShapes.small
                            )
                        }
                    }
                }

                // Campo Universidad con Autocomplete y Filtrado Interactivo
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
                        label = { Text("Universidad") },
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
                    label = { Text("Correo institucional") },
                    placeholder = { Text("alumno@continental.edu.pe") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    isError = state.emailError != null,
                    supportingText = {
                        state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } ?: Text("Debe pertenecer a un dominio universitario (.edu.pe)", style = MaterialTheme.typography.bodySmall)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo Contraseña con botón de visibilidad interactivo
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { viewModel.onRegisterPasswordChange(it) },
                    label = { Text("Contraseña") },
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = CampusShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ ${state.errorMessage}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { viewModel.register(onRegisterSuccess) },
                    enabled = !state.isLoading,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creando cuenta...")
                    } else {
                        Text("Continuar a Verificación", fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes una cuenta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
