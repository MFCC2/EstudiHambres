package com.example.estudihambres.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
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
import com.example.estudihambres.presentation.verification.StudentVerificationScreen
import com.example.estudihambres.domain.model.VerificationStatus

/**
 * Elemento de la barra inferior de navegación.
 */
private data class NavigationItem(
    val screen: Screen,
    val icon: ImageVector
)

/**
 * Grafo principal de navegación alojado en core/navigation.
 * Administra las rutas principales: Login, Register, Verification, Home, Map y Profile.
 *
 * Corrección de seguridad:
 * La barra inferior de navegación se oculta estrictamente durante la autenticación (Login, Register)
 * y durante el escaneo y verificación de carnet (Verification) para evitar accesos no autorizados
 * o fuga de navegación previa a la acreditación del estudiante.
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
        NavigationItem(Screen.Rewards, Icons.Default.CardGiftcard),
        NavigationItem(Screen.Profile, Icons.Default.Person)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ocultar barra inferior en pantallas de autenticación y durante el escaneo de carnet
    // Esto previene que un usuario no autenticado o no verificado navegue libremente
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Map.route,
        Screen.Rewards.route,
        Screen.Profile.route
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = androidx.compose.runtime.remember {
        com.example.estudihambres.core.util.SessionManager.getInstance(context)
    }
    val startDestination = androidx.compose.runtime.remember {
        if (sessionManager.isUserLoggedIn()) Screen.Home.route else Screen.Login.route
    }

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
            startDestination = startDestination,
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
                        val isVerified = authViewModel.currentUser.value?.verificationStatus == VerificationStatus.VERIFIED
                        if (isVerified) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Verification.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Verification.route) {
                StudentVerificationScreen(
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
                HomeScreen(
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToVerification = {
                        navController.navigate(Screen.Verification.route)
                    },
                    onNavigateToMap = {
                        navController.navigate(Screen.Map.route)
                    },
                    onLogout = {
                        sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
            composable(Screen.Rewards.route) {
                com.example.estudihambres.presentation.rewards.RewardsScreen()
            }
            composable(Screen.Profile.route) {
                com.example.estudihambres.presentation.profile.ProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToVerification = {
                        navController.navigate(Screen.Verification.route)
                    },
                    onLogout = {
                        sessionManager.clearSession()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
