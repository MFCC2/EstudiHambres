package com.example.estudihambres.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas y bordes redondeados según el manual de diseño de CampusPass:
 * Tarjetas principales con bordes de 16.dp (RoundedCornerShape(16.dp)).
 */
val CampusShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
