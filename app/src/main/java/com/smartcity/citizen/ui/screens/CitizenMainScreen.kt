package com.smartcity.citizen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.citizen.data.AuthRepository
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.PreferencesDataStore
import com.smartcity.citizen.ui.theme.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Report : Screen("report", "Report", Icons.Filled.ReportProblem)
    object Track : Screen("track", "Track", Icons.AutoMirrored.Filled.ListAlt)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object Detail : Screen("detail/{complaintId}", "Detail", Icons.Filled.Info)
    object About : Screen("about", "About", Icons.Filled.Info)
}

val bottomNavItems = listOf(Screen.Home, Screen.Report, Screen.Track, Screen.Settings)

// Data class for notification
data class CitizenNotification(
    val id: String,
    val title: String,
    val body: String,
    val read: Boolean,
    val timestamp: Date,
    val complaintId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenMainScreen(onLogout: () -> Unit = {}) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    val authRepo = remember { AuthRepository() }
    val complaintRepo = remember { ComplaintRepository() }
    val prefs = remember { PreferencesDataStore(context) }
    val firestore = FirebaseFirestore.getInstance()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val email = currentUser?.email ?: "Not logged in"
    var firestoreName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("citizen") }
    val savedName by prefs.userName.collectAsState(initial = "Citizen")

    val currentUserId = currentUser?.uid ?: ""
    val userComplaints by complaintRepo.getComplaintsForUser(currentUserId)
        .collectAsState(initial = emptyList())

    // Notifications state
    var notifications by remember { mutableStateOf<List<CitizenNotification>>(emptyList()) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }

    // For reading a notification in a dedicated dialog
    var selectedNotificationForRead by remember { mutableStateOf<CitizenNotification?>(null) }

    // Listen to notifications for this citizen
    LaunchedEffect(currentUserId) {
        if (currentUserId.isBlank()) return@LaunchedEffect
        firestore.collection("notifications")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { doc ->
                    CitizenNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        read = doc.getBoolean("read") ?: false,
                        timestamp = doc.getLong("timestamp")?.let { Date(it) } ?: Date(),
                        complaintId = doc.getString("complaintId") ?: ""
                    )
                }
                notifications = list
                unreadCount = list.count { !it.read }
            }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            role = authRepo.getUserRole() ?: "citizen"
            firestoreName = authRepo.getUserName() ?: ""
        }
    }

    val displayName = if (firestoreName.isNotBlank()) firestoreName else savedName

    fun logout() {
        scope.launch {
            withContext(NonCancellable) {
                authRepo.logout()
                onLogout()
            }
            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(24.dp))
                DrawerHeader(userName = displayName, userEmail = email)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("About") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.About.route)
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        logout()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Notification button
                        IconButton(onClick = { showNotificationsDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge {
                                            Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (screen.route == Screen.Home.route) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
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
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeScreen(navController = navController) }
                composable(Screen.Report.route) { ReportScreen(navController = navController) }
                composable(Screen.Track.route) { TrackScreen(navController = navController) }
                composable(Screen.Settings.route) { ProfileScreen(navController = navController, onLogout = onLogout) }
                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                    ComplaintDetailScreen(complaintId = complaintId, navController = navController)
                }
                composable(Screen.About.route) {
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
    }

    // Notifications Dialog (shows list, clicking opens a separate read dialog)
    if (showNotificationsDialog) {
        val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            text = {
                if (notifications.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No notifications yet", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { notif ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Instead of navigating directly, show the full message in a dialog
                                        selectedNotificationForRead = notif
                                        showNotificationsDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!notif.read) Color(0xFFF5F5F5) else Color.White
                                )
                            ) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (!notif.read) Primary else Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            notif.title,
                                            fontWeight = if (!notif.read) FontWeight.Bold else FontWeight.Normal,
                                            color = if (!notif.read) Primary else Color.DarkGray
                                        )
                                        Text(
                                            notif.body,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            dateFormat.format(notif.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dedicated dialog to read the full notification message
    if (selectedNotificationForRead != null) {
        val notif = selectedNotificationForRead!!
        AlertDialog(
            onDismissRequest = { selectedNotificationForRead = null },
            title = { Text(notif.title, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(notif.body, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(notif.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Mark as read and optionally go to complaint
                    scope.launch {
                        firestore.collection("notifications").document(notif.id)
                            .update("read", true).await()
                    }
                    selectedNotificationForRead = null
                    if (notif.complaintId.isNotBlank()) {
                        navController.navigate("detail/${notif.complaintId}")
                    }
                }) {
                    Text(if (notif.complaintId.isNotBlank()) "View Complaint" else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Just mark as read and close
                    scope.launch {
                        firestore.collection("notifications").document(notif.id)
                            .update("read", true).await()
                    }
                    selectedNotificationForRead = null
                }) {
                    Text("Close")
                }
            }
        )
    }

    var showUserInfoDialog by remember { mutableStateOf(false) }
    if (showUserInfoDialog) {
        AlertDialog(
            onDismissRequest = { showUserInfoDialog = false },
            title = { Text("Profile") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, null, tint = Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: $email", style = MaterialTheme.typography.bodyMedium)
                    Text("Role: ${role.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodyMedium)
                    Text("Complaints: ${userComplaints.size}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showUserInfoDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun DrawerHeader(userName: String, userEmail: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = userName, style = MaterialTheme.typography.titleMedium)
        Text(text = userEmail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}