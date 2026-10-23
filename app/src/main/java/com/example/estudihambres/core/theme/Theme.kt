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
    primary = CampusBlack,
    onPrimary = CampusWhite,
    primaryContainer = CampusLightSurfaceVariant,
    onPrimaryContainer = CampusLightTextPrimary,
    secondary = CampusSecondary,
    onSecondary = CampusWhite,
    secondaryContainer = CampusSecondary.copy(alpha = 0.12f),
    onSecondaryContainer = CampusSecondary,
    tertiary = CampusSuccess,
    onTertiary = CampusWhite,
    tertiaryContainer = CampusSuccess.copy(alpha = 0.12f),
    onTertiaryContainer = CampusSuccess,
    background = CampusLightBackground,
    surface = CampusLightSurface,
    surfaceVariant = CampusLightSurfaceVariant,
    onBackground = CampusLightTextPrimary,
    onSurface = CampusLightTextPrimary,
    onSurfaceVariant = CampusLightTextSecondary,
    outline = CampusLightBorder,
    outlineVariant = CampusLightBorder.copy(alpha = 0.7f),
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
