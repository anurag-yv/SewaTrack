package com.smartcity.citizen.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.smartcity.citizen.ui.screens.*
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    var startDestination by remember { mutableStateOf<String?>(null) }

    if (startDestination == null) {
        SplashScreen(onDestinationReady = { dest ->
            startDestination = dest
        })
        return
    }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination!!) {
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    navController.navigate("role_router") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                navController = navController,
                onRegisterSuccess = {
                    Toast.makeText(context, "Registration successful! Please login.", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("role_router") {
            RoleRouterScreen(navController = navController)
        }

        composable("citizen_main") {
            key(FirebaseAuth.getInstance().currentUser?.uid ?: "none") {
                CitizenMainScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("admin_dashboard") {
            key(FirebaseAuth.getInstance().currentUser?.uid ?: "none") {
                AdminDashboardScreen(navController = navController)
            }
        }
        composable("admin_profile") {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("admin_analytics") {
            AdminAnalyticsScreen()
        }

        composable("employee_dashboard") {
            key(FirebaseAuth.getInstance().currentUser?.uid ?: "none") {
                EmployeeDashboardScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable("terms_of_service") {
            TermsOfServiceScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun RoleRouterScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (!doc.exists()) {
                    auth.signOut()
                    Toast.makeText(context, "Account not found. Please register.", Toast.LENGTH_LONG).show()
                    navController.navigate("register") {
                        popUpTo("role_router") { inclusive = true }
                    }
                } else {
                    val role = doc.getString("role") ?: "citizen"
                    when (role) {
                        "admin" -> navController.navigate("admin_dashboard") {
                            popUpTo("role_router") { inclusive = true }
                        }
                        "employee" -> navController.navigate("employee_dashboard") {
                            popUpTo("role_router") { inclusive = true }
                        }
                        else -> navController.navigate("citizen_main") {
                            popUpTo("role_router") { inclusive = true }
                        }
                    }
                }
            } catch (e: Exception) {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo("role_router") { inclusive = true }
                }
            }
        } else {
            navController.navigate("login") {
                popUpTo("role_router") { inclusive = true }
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}