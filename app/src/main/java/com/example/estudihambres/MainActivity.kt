package com.example.estudihambres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.estudihambres.core.navigation.AppNavGraph
import com.example.estudihambres.core.theme.AppThemeMode
import com.example.estudihambres.core.theme.EstudiHambresTheme
import com.example.estudihambres.core.util.SessionManager

/**
 * Actividad principal de EstudiHambres.
 *
 * Configura el entorno Edge-to-Edge y monta el grafo de navegación
 * observando reactivamente el modo de apariencia visual (Claro por defecto / Oscuro / Sistema).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            val themeMode by sessionManager.themeModeFlow.collectAsState()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            EstudiHambresTheme(darkTheme = isDark) {
                AppNavGraph()
            }
        }
    }
}
