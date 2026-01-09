package com.example.ava.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ava.ui.screens.home.HomeScreen
import com.example.ava.ui.screens.settings.SettingsScreen
import com.example.ava.ui.screens.settings.ConnectionSettingsScreen
import com.example.ava.ui.screens.settings.InteractionSettingsScreen
import com.example.ava.ui.screens.settings.ExperimentalSettingsScreen
import com.example.ava.ui.screens.settings.ServiceSettingsScreen
import com.example.ava.ui.screens.settings.BrowserSettingsScreen
import com.example.ava.ui.screens.settings.BluetoothSettingsScreen
import com.example.ava.ui.screens.settings.ScreensaverSettingsScreen
import com.example.ava.ui.screens.settings.RootSettingsScreen
import com.example.ava.ui.screens.settings.DiagnosticSettingsScreen

object Screen {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_CONNECTION = "settings/connection"
    const val SETTINGS_INTERACTION = "settings/interaction"
    const val SETTINGS_EXPERIMENTAL = "settings/experimental"
    const val SETTINGS_SERVICE = "settings/service"
    const val SETTINGS_BROWSER = "settings/browser"
    const val SETTINGS_BLUETOOTH = "settings/bluetooth"
    const val SETTINGS_SCREENSAVER = "settings/screensaver"
    const val SETTINGS_ROOT = "settings/root"
    const val SETTINGS_DIAGNOSTIC = "settings/diagnostic"
}

@Composable
fun MainNavHost(startDestination: String = Screen.HOME) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = Screen.HOME) {
            HomeScreen(navController)
        }
        composable(route = Screen.SETTINGS) {
            SettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_CONNECTION) {
            ConnectionSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_INTERACTION) {
            InteractionSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_EXPERIMENTAL) {
            ExperimentalSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_SERVICE) {
            ServiceSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_BROWSER) {
            BrowserSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_BLUETOOTH) {
            BluetoothSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_SCREENSAVER) {
            ScreensaverSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_ROOT) {
            RootSettingsScreen(navController)
        }
        composable(route = Screen.SETTINGS_DIAGNOSTIC) {
            DiagnosticSettingsScreen(navController)
        }
    }
}