package com.example.estudihambres.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * Pantalla de inicio de sesión de CampusPass (presentation/auth).
 *
 * @param viewModel ViewModel de autenticación con validaciones y estado reactivo.
 * @param onLoginSuccess Navegación tras autenticación exitosa.
 * @param onNavigateToRegister Navegación hacia el formulario de registro de estudiante.
 * @param onBypassClick Acceso directo para pruebas locales y desarrollo rápido.
 * @param modifier Modificador Compose.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBypassClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.loginState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CampusPass \uD83C\uDF93",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Beneficios y comida universitaria al alcance de tu bolsillo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onLoginEmailChange(it) },
                    label = { Text("Correo institucional") },
                    placeholder = { Text("ejemplo@universidad.edu.pe") },
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
                    onValueChange = { viewModel.onLoginPasswordChange(it) },
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
                Button(
                    onClick = { viewModel.login(onLoginSuccess) },
                    enabled = !state.isLoading,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Iniciar Sesión")
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.bypass(onBypassClick) },
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚡ Acceso Directo (Bypass Dev)")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Regístrate aquí")
                    }
                }
            }
        }
    }
}
