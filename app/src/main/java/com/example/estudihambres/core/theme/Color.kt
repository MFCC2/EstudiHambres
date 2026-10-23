package com.example.estudihambres.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores minimalista, profesional y de alto contraste (Blanco, Negro y Acentos selectos)
 * para EstudiHambres.
 *
 * Enfoque de diseño:
 * - Base monocromática profesional (Negro profundo, blancos puros y escalas de gris grafito).
 * - Menos saturación visual para reducir sobrecarga cognitiva.
 * - Acento vibrante naranja/ámbar reservado exclusivamente para promociones, descuentos y botones clave.
 * - Verde esmeralda sutil para validaciones exitosas de SUNEDU y ahorro acumulado.
 */

// Monocromo Principal Profesional
val CampusBlack = Color(0xFF09090B)
val CampusDarkSurface = Color(0xFF141417)
val CampusDarkSurfaceVariant = Color(0xFF1C1C21)
val CampusDarkBorder = Color(0xFF27272A)
val CampusDarkTextPrimary = Color(0xFFFAFAFA)
val CampusDarkTextSecondary = Color(0xFFA1A1AA)

val CampusWhite = Color(0xFFFFFFFF)
val CampusLightBackground = Color(0xFFF9FAFB)
val CampusLightSurface = Color(0xFFFFFFFF)
val CampusLightSurfaceVariant = Color(0xFFF4F4F6)
val CampusLightBorder = Color(0xFFE5E7EB)
val CampusLightTextPrimary = Color(0xFF111827)
val CampusLightTextSecondary = Color(0xFF6B7280)

// Acentos selectos para promociones, ofertas y botones
val CampusPrimary = Color(0xFF09090B) // Monocromo dominante
val CampusPrimaryVariant = Color(0xFF18181B)
val CampusSecondary = Color(0xFFF97316) // Naranja vibrante para promociones y ofertas
val CampusSuccess = Color(0xFF10B981) // Verde éxito / SUNEDU verificado
val CampusWarning = Color(0xFFF59E0B) // Ámbar de advertencia / pendiente
val CampusError = Color(0xFFEF4444)

// Compatibilidad con tokens previos de la app
val CampusBackgroundLight = CampusLightBackground
val CampusSurfaceLight = CampusLightSurface
val CampusOnPrimaryLight = CampusWhite
val CampusOnSecondaryLight = CampusWhite
val CampusOnBackgroundLight = CampusLightTextPrimary
val CampusOnSurfaceLight = CampusLightTextPrimary

val CampusBackgroundDark = CampusBlack
val CampusSurfaceDark = CampusDarkSurface
val CampusOnPrimaryDark = CampusWhite
val CampusOnSecondaryDark = CampusWhite
val CampusOnBackgroundDark = CampusDarkTextPrimary
val CampusOnSurfaceDark = CampusDarkTextPrimary

// Bordes y acentos sutiles
val CampusCardBorder = CampusLightBorder
val CampusCardBorderDark = CampusDarkBorder
val CampusTagFood = Color(0xFFF97316)
val CampusTagTech = Color(0xFF10B981)
val CampusTagTransport = Color(0xFF0284C7)
val CampusTagCulture = Color(0xFF8B5CF6)
val CampusGold = Color(0xFFF59E0B)

// Degradados profesionales refinados (sutiles y no sobrecargados)
val CampusGradientPrimary = Brush.horizontalGradient(listOf(Color(0xFF18181B), Color(0xFF09090B)))
val CampusGradientEmerald = Brush.horizontalGradient(listOf(Color(0xFF065F46), Color(0xFF047857)))
val CampusGradientSunset = Brush.horizontalGradient(listOf(Color(0xFFEA580C), Color(0xFFF97316)))
val CampusGradientPurple = Brush.horizontalGradient(listOf(Color(0xFF4C1D95), Color(0xFF6D28D9)))
val CampusGradientWallet = Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFF27272A)))
val CampusGradientGold = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B)))

// Colores de marcas aliadas
val BrandSpotify = Color(0xFF1DB954)
val BrandKFC = Color(0xFFE4002B)
val BrandStarbucks = Color(0xFF00704A)
val BrandBembos = Color(0xFF002F6C)
val BrandBembosRed = Color(0xFFDA291C)
val BrandCineplanet = Color(0xFF005CB9)
val BrandCinemark = Color(0xFFD6181F)
val BrandApple = Color(0xFF1D1D1F)
val BrandGitHub = Color(0xFF24292F)
val BrandJetBrains = Color(0xFF8B5CF6)
val BrandNotion = Color(0xFF2D3748)
val BrandFigma = Color(0xFFF24E1E)
val BrandMicrosoft = Color(0xFF00A4EF)
val BrandPapaJohns = Color(0xFF006A4E)
val BrandCrisol = Color(0xFF00843D)
val BrandAtu = Color(0xFF0033A0)
val BrandCanva = Color(0xFF00C4CC)
val BrandAutodesk = Color(0xFF0696D7)

/**
 * Obtiene el color de acento característico para una marca o comercio aliado.
 */
fun getBrandColor(partnerName: String): Color {
    val lower = partnerName.lowercase()
    return when {
        lower.contains("spotify") -> BrandSpotify
        lower.contains("kfc") -> BrandKFC
        lower.contains("starbucks") -> BrandStarbucks
        lower.contains("bembos") -> BrandBembosRed
        lower.contains("cineplanet") -> BrandCineplanet
        lower.contains("cinemark") -> BrandCinemark
        lower.contains("apple") -> BrandApple
        lower.contains("jetbrains") -> BrandJetBrains
        lower.contains("github") -> BrandGitHub
        lower.contains("notion") -> BrandNotion
        lower.contains("figma") -> BrandFigma
        lower.contains("microsoft") -> BrandMicrosoft
        lower.contains("papa john") -> BrandPapaJohns
        lower.contains("crisol") -> BrandCrisol
        lower.contains("canva") -> BrandCanva
        lower.contains("autodesk") -> BrandAutodesk
        lower.contains("atu") || lower.contains("transporte") -> BrandAtu
        lower.contains("cultura") || lower.contains("boleto") -> CampusTagCulture
        else -> CampusSecondary
    }
}

/**
 * Obtiene el monograma o iniciales para mostrar en la insignia del comercio aliado.
 */
fun getBrandMonogram(partnerName: String): String {
    val clean = partnerName.trim()
    val words = clean.split(" ").filter { it.isNotBlank() }
    return when {
        clean.contains("KFC", ignoreCase = true) -> "KFC"
        clean.contains("Bembos", ignoreCase = true) -> "BB"
        clean.contains("Starbucks", ignoreCase = true) -> "SB"
        clean.contains("Spotify", ignoreCase = true) -> "SP"
        clean.contains("Cineplanet", ignoreCase = true) -> "CP"
        clean.contains("Cinemark", ignoreCase = true) -> "CM"
        clean.contains("Apple", ignoreCase = true) -> "AP"
        clean.contains("Microsoft", ignoreCase = true) -> "MS"
        clean.contains("GitHub", ignoreCase = true) -> "GH"
        clean.contains("Figma", ignoreCase = true) -> "FG"
        clean.contains("ATU", ignoreCase = true) -> "ATU"
        words.size >= 2 -> "${words[0].first().uppercase()}${words[1].first().uppercase()}"
        words.isNotEmpty() -> words[0].take(2).uppercase()
        else -> "SH"
    }
}
