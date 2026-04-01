package com.example.calanque.navigation

import androidx.compose.foundation.layout.padding
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
import com.example.calanque.screens.* // Import groupé pour plus de clarté

sealed class Screen(
    val route: String,
    val label: String,
    val iconRes: Int
) {
    object Accueil : Screen("accueil", "Accueil", R.drawable.baseline_home_32)
    object Panier : Screen("panier", "Panier", R.drawable.baseline_shopping_basket_32)
    object Compte : Screen("compte", "Compte", R.drawable.baseline_person_32)
    object Carte : Screen("carte", "Carte", R.drawable.baseline_map_32)
    object Auth : Screen("auth", "Connexion", R.drawable.baseline_person_48)
    object Signup : Screen("signup", "Inscription", R.drawable.baseline_person_48)
    object Activities : Screen("activities", "Activités", 0)
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
                                painter = painterResource(id = screen.iconRes),
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Accueil.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- ACCUEIL ---
            composable(Screen.Accueil.route) {
                HomeScreen(onNavigate = {
                    navController.navigate(Screen.Activities.route)
                })
            }

            // --- PANIER ---
            composable(Screen.Panier.route) { PanierScreen() }

            // --- COMPTE (CORRIGÉ) ---
            composable(Screen.Compte.route) {
                // On ne passe plus d'ID utilisateur !
                // AccountScreen se débrouillera avec le token via /api/users/me
                AccountScreen(
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }

            // --- CARTE ---
            composable(Screen.Carte.route) { CarteScreen() }

            // --- CONNEXION ---
            composable(Screen.Auth.route) {
                AuthScreen(
                    onNavigateToSignup = {
                        navController.navigate(Screen.Signup.route)
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Compte.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            // --- INSCRIPTION ---
            composable(Screen.Signup.route) {
                SignupScreen(onNavigateBack = { navController.popBackStack() })
            }

            // --- LISTE DES ACTIVITÉS ---
            composable(Screen.Activities.route) {
                MyActivitiesListScreen()
            }
        }
    }
}