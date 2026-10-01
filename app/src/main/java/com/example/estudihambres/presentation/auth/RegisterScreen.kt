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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudihambres.core.theme.CampusShapes

/**
 * Pantalla de registro estudiantil con Material 3 (presentation/auth).
 * Solicita DNI, Código de Alumno, Universidad, Correo institucional y Contraseña.
 *
 * @param onRegisterSuccess Navegación tras registrar correctamente (lleva a la verificación de carnet).
 * @param onNavigateToLogin Navegación de retorno al login.
 * @param modifier Modificador Compose.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dni by remember { mutableStateOf("") }
    var studentCode by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                    value = dni,
                    onValueChange = { if (it.length <= 8) dni = it },
                    label = { Text("DNI (8 dígitos)") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = studentCode,
                    onValueChange = { studentCode = it },
                    label = { Text("Código de Alumno") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = university,
                    onValueChange = { university = it },
                    label = { Text("Universidad") },
                    placeholder = { Text("Ej. UNMSM, UNI, PUCP, UPC") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    singleLine = true,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo institucional") },
                    placeholder = { Text("alumno@universidad.edu.pe") },
                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onRegisterSuccess,
                    shape = CampusShapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continuar a Verificación")
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
