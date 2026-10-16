@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.smartcity.citizen.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartcity.citizen.data.local.SessionManager
import com.smartcity.citizen.model.UserRole
import com.smartcity.citizen.model.WasteCategory
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItPrimaryLight
import com.smartcity.citizen.ui.theme.SortItSecondary

sealed class SortItNavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : SortItNavTab("tab_home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Scan : SortItNavTab("tab_scan", "AI Scan", Icons.Filled.CameraAlt, Icons.Outlined.PhotoCamera)
    object WorkerOps : SortItNavTab("tab_worker_ops", "Shift Ops", Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices)
    object AdminCenter : SortItNavTab("tab_admin_center", "Overview", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings)
    object Bins : SortItNavTab("tab_bins", "Campus Bins", Icons.Filled.Place, Icons.Outlined.Place)
    object Guide : SortItNavTab("tab_guide", "Guide", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object History : SortItNavTab("tab_history", "History", Icons.Filled.History, Icons.Outlined.History)
}

/**
 * Top-Level App Container with Material 3 NavigationBar, Scaffold & Dynamic RBAC Roles
 * Fulfills CSE226 Unit I: Material 3 Scaffold, BottomNavigation & Responsive UI
 */
@Composable
fun SortItMainScreen() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val currentSession by sessionManager.currentSession.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedGuideCategory by remember { mutableStateOf<WasteCategory?>(null) }

    // Dynamic Navigation Tabs based on RBAC Role
    val currentTabs = remember(currentSession.role) {
        when (currentSession.role) {
            UserRole.STUDENT -> listOf(
                SortItNavTab.Home,
                SortItNavTab.Scan,
                SortItNavTab.Bins,
                SortItNavTab.Guide,
                SortItNavTab.History
            )
            UserRole.WORKER -> listOf(
                SortItNavTab.WorkerOps,
                SortItNavTab.Bins,
                SortItNavTab.Guide,
                SortItNavTab.History
            )
            UserRole.ADMIN -> listOf(
                SortItNavTab.AdminCenter,
                SortItNavTab.Bins,
                SortItNavTab.Guide,
                SortItNavTab.History
            )
        }
    }

    val isTopLevelTab = currentTabs.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (isTopLevelTab) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Recycling,
                                contentDescription = null,
                                tint = SortItPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "SortIt",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = SortItPrimary
                                    )
                                    Surface(
                                        color = SortItPrimary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { navController.navigate("profile") }
                                    ) {
                                        Text(
                                            text = when (currentSession.role) {
                                                UserRole.STUDENT -> "🎓 Student"
                                                UserRole.WORKER -> "👷 Worker"
                                                UserRole.ADMIN -> "🛡️ Admin"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SortItPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = currentSession.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("profile") }) {
                            Icon(Icons.Outlined.AccountCircle, contentDescription = "User Profile", tint = SortItPrimary)
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (isTopLevelTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    currentTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SortItPrimary,
                                selectedTextColor = SortItPrimary,
                                indicatorColor = SortItPrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = when (currentSession.role) {
                UserRole.STUDENT -> SortItNavTab.Home.route
                UserRole.WORKER -> SortItNavTab.WorkerOps.route
                UserRole.ADMIN -> SortItNavTab.AdminCenter.route
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            // Student Dashboard
            composable(SortItNavTab.Home.route) {
                HomeScreen(
                    onNavigateToScan = {
                        navController.navigate(SortItNavTab.Scan.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToGuide = { category ->
                        selectedGuideCategory = category
                        navController.navigate(SortItNavTab.Guide.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLocator = {
                        navController.navigate(SortItNavTab.Bins.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(SortItNavTab.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Campus Worker Dashboard
            composable(SortItNavTab.WorkerOps.route) {
                WorkerDashboardScreen(
                    onNavigateToBins = {
                        navController.navigate(SortItNavTab.Bins.route)
                    },
                    onNavigateToGuide = {
                        navController.navigate(SortItNavTab.Guide.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile")
                    }
                )
            }

            // Campus Admin Dashboard
            composable(SortItNavTab.AdminCenter.route) {
                AdminDashboardScreen(
                    onNavigateToBins = {
                        navController.navigate(SortItNavTab.Bins.route)
                    },
                    onNavigateToGuide = {
                        navController.navigate(SortItNavTab.Guide.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile")
                    }
                )
            }

            // Shared Feature Screens
            composable(SortItNavTab.Scan.route) {
                WasteClassifierScreen(
                    onNavigateToGuide = {
                        navController.navigate(SortItNavTab.Guide.route)
                    },
                    onNavigateBack = {
                        val homeRoute = when (currentSession.role) {
                            UserRole.STUDENT -> SortItNavTab.Home.route
                            UserRole.WORKER -> SortItNavTab.WorkerOps.route
                            UserRole.ADMIN -> SortItNavTab.AdminCenter.route
                        }
                        navController.navigate(homeRoute)
                    }
                )
            }

            composable(SortItNavTab.Bins.route) {
                CampusBinLocatorScreen()
            }

            composable(SortItNavTab.Guide.route) {
                WasteGuideScreen(
                    initialCategory = selectedGuideCategory
                )
            }

            composable(SortItNavTab.History.route) {
                HistoryScreen()
            }

            // Authentication & Account Routes
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        val homeRoute = when (currentSession.role) {
                            UserRole.STUDENT -> SortItNavTab.Home.route
                            UserRole.WORKER -> SortItNavTab.WorkerOps.route
                            UserRole.ADMIN -> SortItNavTab.AdminCenter.route
                        }
                        navController.navigate(homeRoute) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        val homeRoute = when (currentSession.role) {
                            UserRole.STUDENT -> SortItNavTab.Home.route
                            UserRole.WORKER -> SortItNavTab.WorkerOps.route
                            UserRole.ADMIN -> SortItNavTab.AdminCenter.route
                        }
                        navController.navigate(homeRoute) {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable("profile") {
                ProfileScreen(
                    onNavigateToLogin = {
                        navController.navigate("login")
                    },
                    onNavigateToPrivacy = {
                        navController.navigate("privacy_policy")
                    },
                    onNavigateToTerms = {
                        navController.navigate("terms_of_service")
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateToPrivacy = { navController.navigate("privacy_policy") },
                    onNavigateToTerms = { navController.navigate("terms_of_service") }
                )
            }

            composable("privacy_policy") {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }

            composable("terms_of_service") {
                TermsOfServiceScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
