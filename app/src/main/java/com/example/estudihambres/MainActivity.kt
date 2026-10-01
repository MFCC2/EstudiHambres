package com.example.estudihambres

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.estudihambres.core.navigation.AppNavGraph
import com.example.estudihambres.core.theme.CampusPassTheme
import com.example.estudihambres.core.util.SessionManager

/**
 * Actividad principal de CampusPass (EstudiHambres).
 *
 * Configura el entorno Edge-to-Edge y monta el grafo de navegación alojado en core/navigation
 * utilizando el tema corporativo de core/theme.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            CampusPassTheme {
                AppNavGraph()
            }
        }
    }
}