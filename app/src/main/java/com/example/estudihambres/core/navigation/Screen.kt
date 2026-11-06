package com.example.estudihambres.core.navigation

/**
 * Rutas de navegación principales de EstudiHambres centralizadas en core/navigation.
 *
 * @property route Identificador de la ruta para Compose Navigation.
 * @property title Etiqueta legible para la interfaz de usuario.
 */
sealed class Screen(val route: String, val title: String) {
    data object Login : Screen("login", "Iniciar Sesión")
    data object Register : Screen("register", "Registro Estudiantil")
    data object Verification : Screen("verification", "Verificar Carnet")
    data object Home : Screen("home", "Inicio")
    data object Map : Screen("map", "Radar 30km")
    data object Rewards : Screen("rewards", "Premios")
    data object Profile : Screen("profile", "Mi Cuenta")
}
