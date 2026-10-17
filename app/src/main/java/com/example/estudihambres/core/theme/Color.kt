package com.example.estudihambres.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores oficial y extendida de CampusPass según PROJECT_RULES.md:
 * - Primario Azul/Índigo: #3344EE
 * - Verde éxito: #00C853
 * - Naranja acento: #FF6D00
 * - Tonos modernos de superficie, elevación, degradados y bordes sutiles.
 */
val CampusPrimary = Color(0xFF3344EE)
val CampusPrimaryVariant = Color(0xFF2030C8)
val CampusSecondary = Color(0xFFFF6D00)
val CampusSuccess = Color(0xFF00C853)

val CampusBackgroundLight = Color(0xFFF8F9FE)
val CampusSurfaceLight = Color(0xFFFFFFFF)
val CampusOnPrimaryLight = Color(0xFFFFFFFF)
val CampusOnSecondaryLight = Color(0xFFFFFFFF)
val CampusOnBackgroundLight = Color(0xFF0F172A)
val CampusOnSurfaceLight = Color(0xFF0F172A)

val CampusBackgroundDark = Color(0xFF0B0F19)
val CampusSurfaceDark = Color(0xFF151D2C)
val CampusOnPrimaryDark = Color(0xFFFFFFFF)
val CampusOnSecondaryDark = Color(0xFFFFFFFF)
val CampusOnBackgroundDark = Color(0xFFF1F5F9)
val CampusOnSurfaceDark = Color(0xFFF1F5F9)

// Acentos sutiles para tarjetas, bordes y categorías
val CampusCardBorder = Color(0xFFE2E8F0)
val CampusCardBorderDark = Color(0xFF2E3A52)
val CampusTagFood = Color(0xFFFF6D00)
val CampusTagTech = Color(0xFF00C853)
val CampusTagTransport = Color(0xFF0284C7)
val CampusTagCulture = Color(0xFF8B5CF6)
val CampusGold = Color(0xFFFFB300)

// Degradados premium oficiales
val CampusGradientPrimary = Brush.horizontalGradient(listOf(Color(0xFF3344EE), Color(0xFF1D4ED8)))
val CampusGradientEmerald = Brush.horizontalGradient(listOf(Color(0xFF0D6838), Color(0xFF107C41), Color(0xFF00695C)))
val CampusGradientSunset = Brush.horizontalGradient(listOf(Color(0xFFFF6D00), Color(0xFFFF8F00)))
val CampusGradientPurple = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF4F46E5)))
