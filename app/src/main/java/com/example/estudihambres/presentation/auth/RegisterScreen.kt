package com.example.estudihambres.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudihambres.core.theme.CampusShapes

/**
 * Pantalla de registro estudiantil con Material 3 (presentation/auth).
 * Conecta con [AuthViewModel] para validaciones de 8 dígitos de DNI, correo institucional y campos requeridos.
 *
 * @param viewModel ViewModel de autenticación.
 * @param onRegisterSuccess Navegación tras registrar correctamente (lleva a la verificación de carnet).
 * @param onNavigateToLogin Navegación de retorno al login.
 * @param modifier Modificador Compose.
 */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.registerState.collectAsState()

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
                    text = "Registro Estudiantil",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Completa tus datos para activar tu perfil y acceder a beneficios universitarios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = state.dni,
                    onValueChange = { viewModel.onRegisterDniChange(it) },
                    label = { Text("DNI (8 dígitos)") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    isError = state.dniError != null,
                    supportingText = {
                        state.dniError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.studentCode,
                    onValueChange = { viewModel.onRegisterStudentCodeChange(it) },
                    label = { Text("Código de Alumno") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = state.studentCodeError != null,
                    supportingText = {
                        state.studentCodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.university,
                    onValueChange = { viewModel.onRegisterUniversityChange(it) },
                    label = { Text("Universidad") },
                    placeholder = { Text("Ej. UNMSM, UNI, PUCP, UPC") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    isError = state.universityError != null,
                    supportingText = {
                        state.universityError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onRegisterEmailChange(it) },
                    label = { Text("Correo institucional") },
                    placeholder = { Text("alumno@universidad.edu.pe") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    isError = state.emailError != null,
                    supportingText = {
                        state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { viewModel.onRegisterPasswordChange(it) },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.passwordError != null,
                    supportingText = {
                        state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { viewModel.register(onRegisterSuccess) },
                    enabled = !state.isLoading,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Continuar a Verificación")
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes una cuenta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Iniciar Sesión")
                    }
                }
            }
        }
    }
}
