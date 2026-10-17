package com.example.estudihambres.presentation.auth

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudihambres.core.theme.CampusShapes

/**
 * Pantalla de inicio de sesión de CampusPass (presentation/auth).
 * Diseñada para máxima intuitividad: incluye alternancia de visibilidad de contraseña,
 * sugerencias contextuales, atajos de teclado y acceso directo de desarrollo.
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
    var passwordVisible by remember { mutableStateOf(false) }

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
                // Logo oficial Mapache Mamey
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.estudihambres.R.drawable.app_logo),
                        contentDescription = "Logo Oficial Mapache Mamey",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Text(
                    text = "CampusPass 🎓",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Beneficios y comida universitaria al alcance de tu bolsillo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mensaje informativo contextual
                Surface(
                    shape = CampusShapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "💡 Tip: Ingresa con tu correo institucional (.edu.pe) para activar descuentos universitarios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Campo Correo Institucional
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onLoginEmailChange(it) },
                    label = { Text("Correo institucional") },
                    placeholder = { Text("alumno@continental.edu.pe") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    isError = state.emailError != null,
                    supportingText = {
                        state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
                    onValueChange = { viewModel.onLoginPasswordChange(it) },
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
                        state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.login(onLoginSuccess) }
                    ),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                // Alerta de error en caso de fallo de autenticación
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

                // Botón Iniciar Sesión
                Button(
                    onClick = { viewModel.login(onLoginSuccess) },
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
                        Text("Verificando cuenta...")
                    } else {
                        Text("Iniciar Sesión", fontWeight = FontWeight.Bold)
                    }
                }

                // Botón Acceso Directo Dev
                OutlinedButton(
                    onClick = { viewModel.bypass(onBypassClick) },
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("⚡ Acceso Directo (Bypass Dev)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Regístrate aquí", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
