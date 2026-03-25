package com.example.calanque.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calanque.R
import com.example.calanque.screens.AccountScreen
import com.example.calanque.screens.ActivitiesScreen
import com.example.calanque.screens.CarteScreen
import androidx.compose.foundation.layout.padding
import com.example.calanque.screens.HomeScreen
import com.example.calanque.screens.PanierScreen
import com.example.calanque.screens.AuthScreen
import com.example.calanque.screens.SignupScreen

sealed class Screen(
    val route:   String,
    val label:   String,
    val iconRes: Int
) {
    object Accueil : Screen("accueil", "Accueil", R.drawable.baseline_home_48)
    object Panier    : Screen("panier",    "Panier",    R.drawable.baseline_shopping_basket_48)
    object Compte    : Screen("compte",    "Compte",    R.drawable.baseline_person_48)
    object Carte     : Screen("carte",     "Carte",     R.drawable.baseline_map_48)
    object Auth : Screen("auth", "Connexion", R.drawable.baseline_person_48)
    object Signup  : Screen("signup",  "Inscription", R.drawable.baseline_person_48)
}

val bottomNavItems = listOf(
    Screen.Accueil,
    Screen.Panier,
    Screen.Compte,
    Screen.Carte
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter            = painterResource(id = screen.iconRes),
                                contentDescription = screen.label
                            )
                        },
                        label    = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Accueil.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Accueil.route) { HomeScreen() }

            composable(Screen.Panier.route)  { PanierScreen() }

            composable(Screen.Compte.route) {
                // On donne l'ID de la session. Si c'est null, on met 0 par sécurité.
                AccountScreen(
                    userId = UserSession.userId ?: 0,
                    onNavigateToAuth = { navController.navigate(Screen.Auth.route) }
                )
            }

            composable(Screen.Carte.route) { CarteScreen() }

            // --- LA CORRECTION EST ICI ---
            composable(Screen.Auth.route) {
                AuthScreen(
                    onNavigateToSignup = {
                        navController.navigate(Screen.Signup.route)
                    },
                    onLoginSuccess = {
                        // On navigue vers le compte
                        navController.navigate(Screen.Compte.route) {
                            // On vide la pile pour éviter de revenir en arrière sur l'Auth
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            // ------------------------------

            composable(Screen.Signup.route) {
                SignupScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}