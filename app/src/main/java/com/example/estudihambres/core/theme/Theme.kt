package com.example.estudihambres.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CampusPrimary,
    onPrimary = CampusOnPrimaryDark,
    secondary = CampusSecondary,
    onSecondary = CampusOnSecondaryDark,
    tertiary = CampusSuccess,
    background = CampusBackgroundDark,
    surface = CampusSurfaceDark,
    onBackground = CampusOnBackgroundDark,
    onSurface = CampusOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = CampusPrimary,
    onPrimary = CampusOnPrimaryLight,
    secondary = CampusSecondary,
    onSecondary = CampusOnSecondaryLight,
    tertiary = CampusSuccess,
    background = CampusBackgroundLight,
    surface = CampusSurfaceLight,
    onBackground = CampusOnBackgroundLight,
    onSurface = CampusOnSurfaceLight
)

/**
 * Tema principal de CampusPass en core/theme configurado con Material 3.
 */
@Composable
fun CampusPassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CampusTypography,
        shapes = CampusShapes,
        content = content
    )
}
