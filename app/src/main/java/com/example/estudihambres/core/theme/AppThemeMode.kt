package com.example.estudihambres.core.theme

/**
 * Modos de apariencia visual para EstudiHambres.
 *
 * [LIGHT]: Tema claro con fondo blanco 100% puro, limpio y sobrio (Predeterminado).
 * [DARK]: Tema oscuro con fondo negro obsidiana y superficies grafito de bajo brillo.
 * [SYSTEM]: Sincronizado dinámicamente con la configuración del sistema Android.
 */
enum class AppThemeMode(val title: String, val description: String) {
    LIGHT("Claro", "Fondo blanco limpio y alto contraste (Recomendado)"),
    DARK("Oscuro", "Fondo obsidiana y superficies grafito"),
    SYSTEM("Sistema", "Sigue el modo activo de Android")
}
