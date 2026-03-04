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
import com.example.calanque.screens.PanierScreen
import com.example.calanque.screens.CarteScreen

// TOUT en iconRes Int, plus de ImageVector
sealed class Screen(
    val route:   String,
    val label:   String,
    val iconRes: Int
) {
    object Activites : Screen("activites", "Activités", R.drawable.ic_activites)
    object Panier    : Screen("panier",    "Panier",    R.drawable.ic_basket_background)
    object Compte    : Screen("compte",    "Compte",    R.drawable.ic_basket_background)
    object Carte     : Screen("carte",     "Carte",     R.drawable.ic_carte)
}

val bottomNavItems = listOf(
    Screen.Activites,
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
            startDestination = Screen.Activites.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Activites.route) { ActivitiesScreen() }
            composable(Screen.Panier.route)    { PanierScreen() }
            composable(Screen.Compte.route)    { AccountScreen() }
            composable(Screen.Carte.route)     { CarteScreen() }
        }
    }
}