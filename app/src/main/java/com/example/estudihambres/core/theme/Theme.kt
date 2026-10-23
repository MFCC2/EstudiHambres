package com.example.estudihambres.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Esquema oscuro profesional:
 * Fondo obsidiana / negro profundo, tarjetas en grafito sutil, textos en blanco nítido,
 * y acentos de color selectos para promociones (naranja) y aprobaciones (esmeralda).
 */
private val DarkColorScheme = darkColorScheme(
    primary = CampusWhite,
    onPrimary = CampusBlack,
    primaryContainer = CampusDarkSurfaceVariant,
    onPrimaryContainer = CampusDarkTextPrimary,
    secondary = CampusSecondary,
    onSecondary = CampusWhite,
    secondaryContainer = CampusSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = CampusSecondary,
    tertiary = CampusSuccess,
    onTertiary = CampusWhite,
    tertiaryContainer = CampusSuccess.copy(alpha = 0.2f),
    onTertiaryContainer = CampusSuccess,
    background = CampusBlack,
    surface = CampusDarkSurface,
    surfaceVariant = CampusDarkSurfaceVariant,
    onBackground = CampusDarkTextPrimary,
    onSurface = CampusDarkTextPrimary,
    onSurfaceVariant = CampusDarkTextSecondary,
    outline = CampusDarkBorder,
    outlineVariant = CampusDarkBorder.copy(alpha = 0.6f),
    error = CampusError,
    onError = CampusWhite
)

/**
 * Esquema claro profesional:
 * Fondo blanco limpio / gris mínimo, superficies nítidas, textos en negro azabache,
 * y acentos selectos en promociones e insignias.
 */
private val LightColorScheme = lightColorScheme(
    primary = CampusPrimary,
    onPrimary = CampusWhite,
    primaryContainer = CampusLightSurfaceVariant,
    onPrimaryContainer = CampusLightTextPrimary,
    secondary = CampusSecondary,
    onSecondary = CampusWhite,
    secondaryContainer = CampusSecondary.copy(alpha = 0.12f),
    onSecondaryContainer = CampusSecondary,
    tertiary = CampusSuccess,
    onTertiary = CampusWhite,
    tertiaryContainer = Color(0xFFECFDF5),
    onTertiaryContainer = Color(0xFF065F46),
    background = CampusLightBackground, // Blanco 100% puro
    surface = CampusLightSurface, // Superficie blanca pura
    surfaceVariant = CampusLightSurfaceVariant, // Slate-50 sutil
    onBackground = CampusLightTextPrimary, // Tinta negra azabache de máximo contraste
    onSurface = CampusLightTextPrimary,
    onSurfaceVariant = CampusLightTextSecondary, // Slate-600 nítido y legible
    outline = CampusButtonBorderContrast, // Contorno nítido de tinta oscura para botones y foco (evita grises deshabilitados)
    outlineVariant = CampusLightBorder, // Borde Slate-200 para delimitar tarjetas
    error = CampusError,
    onError = CampusWhite
)

/**
 * Tema principal de EstudiHambres con diseño sobrio y profesional.
 */
@Composable
fun EstudiHambresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CampusTypography,
        shapes = CampusShapes,
        content = content
    )
}
