package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.converter.ConverterScreen
import com.example.myapplication.ui.screens.editlist.EditListScreen
import com.example.myapplication.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Converter : Screen("converter")
    data object Settings : Screen("settings")
    data object EditList : Screen("edit_list")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Converter.route
    ) {
        composable(Screen.Converter.route) {
            ConverterScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToEditList = { navController.navigate(Screen.EditList.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EditList.route) {
            EditListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
