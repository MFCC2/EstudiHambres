package com.example.estudihambres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.estudihambres.core.navigation.AppNavGraph
import com.example.estudihambres.core.theme.AppThemeMode
import com.example.estudihambres.core.theme.EstudiHambresTheme
import com.example.estudihambres.core.util.NetworkMonitor
import com.example.estudihambres.core.util.SessionManager
import com.example.estudihambres.presentation.common.NoInternetBlockingScreen

/**
 * Actividad principal de EstudiHambres.
 *
 * Configura el entorno Edge-to-Edge y monta el grafo de navegación
 * observando reactivamente el modo de apariencia visual (Claro por defecto / Oscuro / Sistema)
 * y supervisando la conectividad de red para bloquear la app si no hay internet activo.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager.getInstance(applicationContext)
        val networkMonitor = NetworkMonitor.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            val isOnline by networkMonitor.isOnline.collectAsState()
            val themeMode by sessionManager.themeModeFlow.collectAsState()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            EstudiHambresTheme(darkTheme = isDark) {
                // La app no funciona sin internet. El bloqueo se dibuja ENCIMA del grafo de
                // navegación: antes lo reemplazaba y, al volver la conexión, el estudiante perdía
                // la pantalla en la que estaba (y el formulario de boleta a medio llenar).
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                    if (!isOnline) {
                        BackHandler(enabled = true) { /* sin conexión no se navega */ }
                        NoInternetBlockingScreen(
                            onRetry = { networkMonitor.checkConnection() }
                        )
                    }
                }
            }
        }
    }
}
