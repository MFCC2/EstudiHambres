package com.example.estudihambres.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.estudihambres.presentation.auth.LoginScreen
import com.example.estudihambres.presentation.auth.RegisterScreen
import com.example.estudihambres.presentation.home.HomeScreen
import com.example.estudihambres.presentation.map.MapScreen
import com.example.estudihambres.presentation.verification.OcrScanScreen

/**
 * Elemento de la barra inferior de navegación.
 */
private data class NavigationItem(
    val screen: Screen,
    val icon: ImageVector
)

/**
 * Grafo principal de navegación alojado en core/navigation.
 * Administra las 5 rutas principales: Login, Register, Verification, Home y Map.
 *
 * @param navController Controlador de navegación de Jetpack Compose.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val bottomNavItems = listOf(
        NavigationItem(Screen.Home, Icons.Default.Home),
        NavigationItem(Screen.Map, Icons.Default.LocationOn),
        NavigationItem(Screen.Verification, Icons.Default.QrCodeScanner)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ocultar barra inferior en pantallas de autenticación
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Map.route,
        Screen.Verification.route
    )

    val authViewModel: com.example.estudihambres.presentation.auth.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = item.icon, contentDescription = item.screen.title)
                            },
                            label = {
                                Text(text = item.screen.title)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onBypassClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Verification.route)
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Verification.route) {
                com.example.estudihambres.presentation.verification.StudentVerificationScreen(
                    onVerificationCompleted = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Verification.route) { inclusive = true }
                        }
                    },
                    onSkipClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Verification.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
        }
    }
}
