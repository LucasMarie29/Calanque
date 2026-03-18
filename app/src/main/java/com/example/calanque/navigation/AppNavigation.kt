package com.example.calanque.navigation

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.example.calanque.screens.AccountScreen
import com.example.calanque.screens.ActivityDetailScreen
import com.example.calanque.screens.CarteScreen
import com.example.calanque.screens.HomeScreen
import com.example.calanque.screens.MyActivitiesListScreen
import com.example.calanque.screens.PanierScreen

sealed class Screen(
    val route:   String,
    val label:   String,
    val iconRes: Int
) {
    object Accueil    : Screen("accueil",    "Accueil", R.drawable.baseline_home_32)
    object Panier     : Screen("panier",     "Panier",  R.drawable.baseline_shopping_basket_32)
    object Compte     : Screen("compte",     "Compte",  R.drawable.baseline_person_32)
    object Carte      : Screen("carte",      "Carte",   R.drawable.baseline_map_32)
    object Activities : Screen("activities", "Activités", 0)
}

val bottomNavItems = listOf(
    Screen.Accueil,
    Screen.Panier,
    Screen.Compte,
    Screen.Carte
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val showBottomBar = currentRoute?.startsWith("activity_detail") == false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
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
                                if (screen.route == Screen.Accueil.route) {
                                    // Retour à l'accueil : vide toute la back stack
                                    navController.navigate(Screen.Accueil.route) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Accueil.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Accueil.route) {
                HomeScreen(
                    onNavigate = { navController.navigate(Screen.Activities.route) }
                )
            }

            composable(Screen.Activities.route) {
                MyActivitiesListScreen(
                    onActivityClick = { activityId ->
                        navController.navigate("activity_detail/$activityId")
                    }
                )
            }

            composable("activity_detail/{activityId}") { backStackEntry ->
                val activityId = backStackEntry.arguments
                    ?.getString("activityId")
                    ?.toIntOrNull() ?: return@composable

                ActivityDetailScreen(
                    activityId = activityId,
                    onBack     = { navController.popBackStack() }
                )
            }

            composable(Screen.Panier.route) { PanierScreen() }
            composable(Screen.Compte.route) { AccountScreen() }
            composable(Screen.Carte.route)  { CarteScreen() }
        }
    }
}