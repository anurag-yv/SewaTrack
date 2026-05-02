package com.smartcity.citizen.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Filled.Info)    // ← added back
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Report : Screen("report", "Report", Icons.Filled.ReportProblem)
    object Track : Screen("track", "Track", Icons.AutoMirrored.Filled.ListAlt)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object Detail : Screen("detail/{complaintId}", "Detail", Icons.Filled.Info)
    object About : Screen("about", "About", Icons.Filled.Info)
}

val bottomNavItems = listOf(Screen.Home, Screen.Report, Screen.Track, Screen.Settings)
