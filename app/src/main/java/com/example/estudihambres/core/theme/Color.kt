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
val CampusGradientWallet = Brush.linearGradient(listOf(Color(0xFF0B6623), Color(0xFF0F8A5F), Color(0xFF1A5276)))
val CampusGradientGold = Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF8F00)))

// Colores de marcas aliadas oficiales peruanas e internacionales
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
        else -> CampusPrimary
    }
}

/**
 * Obtiene el monograma o iniciales para mostrar en la insignia del comercio aliado.
 */
fun getBrandMonogram(partnerName: String): String {
    val lower = partnerName.lowercase()
    return when {
        lower.contains("bembos") -> "B"
        lower.contains("starbucks") -> "☕"
        lower.contains("kfc") -> "KFC"
        lower.contains("cineplanet") -> "CP"
        lower.contains("cinemark") -> "CM"
        lower.contains("spotify") -> "🎵"
        lower.contains("apple") -> ""
        lower.contains("jetbrains") -> "JB"
        lower.contains("github") -> "GH"
        lower.contains("notion") -> "N"
        lower.contains("figma") -> "Fg"
        lower.contains("microsoft") -> "M"
        lower.contains("papa john") -> "PJ"
        lower.contains("crisol") -> "📚"
        lower.contains("sbs") -> "SBS"
        lower.contains("canva") -> "Cv"
        lower.contains("autodesk") -> "AD"
        lower.contains("cultura") || lower.contains("boleto") -> "🏛️"
        lower.contains("transporte") || lower.contains("atu") -> "🚌"
        else -> partnerName.take(2).uppercase()
    }
}
