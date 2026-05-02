package com.smartcity.citizen.ui.screens

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.citizen.data.AuthRepository
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.models.Complaint
import com.smartcity.citizen.data.models.Status
import com.smartcity.citizen.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ---- Message data class (same as admin side) ----
data class Message(
    val id: String = "",
    val complaintId: String = "",
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ---- Colour palette ----
private val GreyBg = Color(0xFFF5F5F5)
private val GreyBorder = Color(0xFFE0E0E0)
private val GreyTextDark = Color(0xFF424242)
private val GreyTextMid = Color(0xFF757575)
private val GreyTextLight = Color(0xFF9E9E9E)
private val AccentBlue = Color(0xFF42A5F5)
private val AccentGreen = Color(0xFF2E7D32)
private val AccentOrange = Color(0xFFFFA726)
private val MyMessageBubble = AccentBlue      // employee's own messages
private val OtherMessageBubble = GreyBorder   // admin's messages (light grey)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(onLogout: () -> Unit = {}) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "employee_main"
    ) {
        composable("employee_main") {
            EmployeeMainContent(parentNavController = navController, onLogoutAction = onLogout)
        }
        composable("detail/{complaintId}") { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            // We'll use a separate screen for detail with messaging, but we already have a dialog.
            // For simplicity, we keep using dialog. So this composable might not be used.
            // However, we'll keep it as a fallback.
            ComplaintDetailScreen(complaintId = complaintId, navController = navController)
        }
        composable("employee_profile") {
            ProfileScreen(navController = navController, onLogout = onLogout)
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable("terms_of_service") {
            TermsOfServiceScreen(onBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeMainContent(parentNavController: NavHostController, onLogoutAction: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ComplaintRepository() }
    val firestore = FirebaseFirestore.getInstance()
    val notifHelper = remember { NotificationHelper(context) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val employeeId = currentUser?.uid ?: ""

    var employeeName by remember { mutableStateOf("Employee") }
    LaunchedEffect(employeeId) {
        if (employeeId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val doc = firestore.collection("users").document(employeeId).get().await()
                    employeeName = doc.getString("name") ?: currentUser?.email?.split("@")?.first() ?: "Employee"
                } catch (e: Exception) {
                    employeeName = currentUser?.email?.split("@")?.first() ?: "Employee"
                }
            }
        }
    }

    val assignedComplaints by repo.getComplaintsForEmployee(employeeId).collectAsState(initial = emptyList())
    val sortedComplaints = assignedComplaints.sortedByDescending { it.dateTime }

    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showNotifications by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(employeeId) {
        if (employeeId.isBlank()) return@LaunchedEffect
        firestore.collection("notifications")
            .whereEqualTo("userId", employeeId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["docId"] = doc.id
                    val ts = doc.getLong("timestamp")
                    data["formattedTime"] = if (ts != null) {
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ts))
                    } else ""
                    data["complaintId"] = doc.getString("complaintId") ?: ""
                    data["read"] = doc.getBoolean("read") ?: false
                    data
                }
                notifications = list
                unreadCount = list.count { it["read"] == false }
            }
    }

    var messageComplaint by remember { mutableStateOf<Complaint?>(null) }
    var messageText by remember { mutableStateOf("") }
    var resolveComplaint by remember { mutableStateOf<Complaint?>(null) }
    var resolutionNote by remember { mutableStateOf("") }
    var selectedNotification by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedComplaintForDetail by remember { mutableStateOf<Complaint?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val total = sortedComplaints.size
    val inProgress = sortedComplaints.count { it.status == Status.IN_PROGRESS }
    val resolved = sortedComplaints.count { it.status == Status.RESOLVED }

    val logout = {
        scope.launch {
            withContext(NonCancellable) {
                AuthRepository().logout()
                onLogoutAction()
            }
            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Navigation Drawer ----
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerContentColor = GreyTextDark,
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = GreyTextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = employeeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreyTextDark
                    )
                    Text(
                        text = currentUser?.email ?: "employee@smartcity.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreyTextMid
                    )
                }
                HorizontalDivider(
                    color = GreyBorder,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Column(Modifier.fillMaxWidth()) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = GreyTextMid) },
                        label = { Text("Profile", color = GreyTextDark) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            parentNavController.navigate("employee_profile")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = GreyTextMid) },
                        label = { Text("About", color = GreyTextDark) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            parentNavController.navigate("about")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = GreyBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = GreyTextMid) },
                        label = { Text("Logout", color = GreyTextDark) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            logout()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, null, tint = GreyTextDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    actions = {
                        IconButton(onClick = { showNotifications = true }) {
                            BadgedBox(badge = { if (unreadCount > 0) Badge { Text(unreadCount.toString()) } }) {
                                Icon(Icons.Filled.Notifications, null, tint = GreyTextDark)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreyTextDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                EmployeeStatBadge("Assigned", total, GreyTextDark, Modifier.weight(1f))
                                EmployeeStatBadge("In Progress", inProgress, AccentBlue, Modifier.weight(1f))
                                EmployeeStatBadge("Resolved", resolved, AccentGreen, Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (sortedComplaints.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Assignment, null, Modifier.size(72.dp), tint = GreyTextLight)
                                Text("No tasks assigned yet", style = MaterialTheme.typography.titleLarge, color = GreyTextMid)
                            }
                        }
                    }
                } else {
                    items(sortedComplaints, key = { it.id }) { complaint ->
                        EmployeeTaskCardOptimized(
                            complaint = complaint,
                            onCardClick = { selectedComplaintForDetail = complaint },
                            onStartWork = { scope.launch { repo.updateStatus(complaint.id, Status.IN_PROGRESS) } },
                            onResolve = {
                                resolveComplaint = complaint
                                resolutionNote = complaint.resolutionNote ?: ""
                            },
                            onMessageAdmin = {
                                messageComplaint = complaint
                                messageText = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // ---- NEW: Complaint detail dialog with messaging ----
    if (selectedComplaintForDetail != null) {
        EmployeeComplaintDetailDialogWithMessaging(
            complaint = selectedComplaintForDetail!!,
            employeeId = employeeId,
            employeeName = employeeName,
            firestore = firestore,
            onDismiss = { selectedComplaintForDetail = null }
        )
    }

    // --- Legacy message dialog (kept for "Ask Help" button, but you can also integrate it into the detail dialog) ---
    if (messageComplaint != null) {
        AlertDialog(
            onDismissRequest = { messageComplaint = null },
            title = { Text("Message Admin", color = GreyTextDark) },
            text = {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Your message") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreyBorder,
                        unfocusedBorderColor = GreyBorder,
                        focusedLabelColor = GreyTextMid,
                        cursorColor = GreyTextDark
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val admins = firestore.collection("users").whereEqualTo("role", "admin").get().await()
                        val adminDoc = admins.documents.firstOrNull()
                        if (adminDoc != null) {
                            val msg = Message(
                                complaintId = messageComplaint!!.id,
                                fromUserId = employeeId,
                                fromUserName = employeeName,
                                toUserId = adminDoc.id,
                                text = messageText,
                                timestamp = System.currentTimeMillis()
                            )
                            firestore.collection("messages").add(msg).await()
                            notifHelper.sendToUser(adminDoc.id, "Message from Employee", "$employeeName: $messageText", messageComplaint!!.id)
                            Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No admin found", Toast.LENGTH_SHORT).show()
                        }
                        messageComplaint = null
                    }
                }) { Text("Send", color = GreyTextDark) }
            },
            dismissButton = { TextButton(onClick = { messageComplaint = null }) { Text("Cancel", color = GreyTextMid) } }
        )
    }

    if (resolveComplaint != null) {
        AlertDialog(
            onDismissRequest = { resolveComplaint = null },
            title = { Text("Resolve Task", color = GreyTextDark) },
            text = {
                OutlinedTextField(
                    value = resolutionNote,
                    onValueChange = { resolutionNote = it },
                    label = { Text("Resolution note") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreyBorder,
                        unfocusedBorderColor = GreyBorder,
                        focusedLabelColor = GreyTextMid,
                        cursorColor = GreyTextDark
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            repo.addResolutionNote(resolveComplaint!!.id, resolutionNote)
                            repo.updateStatus(resolveComplaint!!.id, Status.RESOLVED)
                            if (resolveComplaint!!.reportedBy.isNotBlank()) {
                                notifHelper.sendToUser(
                                    resolveComplaint!!.reportedBy,
                                    "Complaint Resolved",
                                    "Your complaint #${resolveComplaint!!.id.take(8)} has been resolved.",
                                    resolveComplaint!!.id
                                )
                            }
                            Toast.makeText(context, "Resolved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            resolveComplaint = null
                        }
                    }
                }) { Text("Submit", color = GreyTextDark) }
            },
            dismissButton = { TextButton(onClick = { resolveComplaint = null }) { Text("Cancel", color = GreyTextMid) } }
        )
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Notifications", color = GreyTextDark) },
            text = {
                if (notifications.isEmpty()) {
                    Text("No notifications", color = GreyTextMid)
                } else {
                    LazyColumn {
                        items(notifications) { notif ->
                            val docId = notif["docId"] as? String ?: return@items
                            val isRead = notif["read"] == true
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val updatedList = notifications.map { n ->
                                        if (n["docId"] == docId) n.toMutableMap().apply { this["read"] = true } else n
                                    }
                                    notifications = updatedList
                                    unreadCount = updatedList.count { it["read"] == false }
                                    scope.launch { firestore.collection("notifications").document(docId).update("read", true).await() }
                                    showNotifications = false
                                    selectedNotification = notif
                                }.padding(4.dp),
                                colors = CardDefaults.cardColors(containerColor = if (!isRead) GreyBg else Color.White)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(notif["title"].toString(), fontWeight = if (!isRead) FontWeight.Bold else FontWeight.Normal, color = GreyTextDark)
                                    Text(notif["body"].toString(), maxLines = 2, overflow = TextOverflow.Ellipsis, color = GreyTextMid)
                                    Text(notif["formattedTime"].toString(), style = MaterialTheme.typography.labelSmall, color = GreyTextLight)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("Close", color = GreyTextDark) } }
        )
    }

    if (selectedNotification != null) {
        val notif = selectedNotification!!
        val complaintId = notif["complaintId"] as? String ?: ""
        var complaintForNotif by remember { mutableStateOf<Complaint?>(null) }
        LaunchedEffect(complaintId) {
            if (complaintId.isNotBlank()) {
                complaintForNotif = repo.getComplaintById(complaintId)
            }
        }
        AlertDialog(
            onDismissRequest = { selectedNotification = null },
            title = { Text(notif["title"].toString(), color = GreyTextDark) },
            text = {
                Column {
                    Text(notif["body"].toString(), style = MaterialTheme.typography.bodyLarge, color = GreyTextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(notif["formattedTime"].toString(), style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                }
            },
            confirmButton = {
                if (complaintForNotif != null) {
                    TextButton(onClick = {
                        selectedNotification = null
                        selectedComplaintForDetail = complaintForNotif
                    }) { Text("View Complaint", color = GreyTextDark) }
                } else {
                    TextButton(onClick = { selectedNotification = null }) { Text("Close", color = GreyTextDark) }
                }
            },
            dismissButton = { TextButton(onClick = { selectedNotification = null }) { Text("Close", color = GreyTextMid) } }
        )
    }
}

@Composable
fun EmployeeStatBadge(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GreyBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
        }
    }
}

@Composable
fun EmployeeTaskCardOptimized(
    complaint: Complaint,
    onCardClick: () -> Unit,
    onStartWork: () -> Unit,
    onResolve: () -> Unit,
    onMessageAdmin: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(complaint.dateTime))
    val statusColor = when (complaint.status) {
        Status.SUBMITTED -> AccentOrange
        Status.IN_PROGRESS -> AccentBlue
        Status.RESOLVED -> AccentGreen
    }

    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val imageUri = complaint.imageUri
    LaunchedEffect(imageUri) {
        if (imageUri.isNotEmpty() && !imageUri.startsWith("http") && !imageUri.startsWith("content://") && !imageUri.startsWith("file://")) {
            withContext(Dispatchers.IO) {
                try {
                    val rawBase64 = if (imageUri.contains(",")) imageUri.substringAfter(",") else imageUri
                    val bytes = Base64.decode(rawBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) { null }
            }.let { bitmap = it }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        complaint.issueType.name.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreyTextDark
                    )
                    Text(
                        complaint.description,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = GreyTextMid
                    )
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Photo",
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (imageUri.isNotEmpty() && (imageUri.startsWith("http") || imageUri.startsWith("content://"))) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Photo",
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Location: ${complaint.location}", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                Text("Budget: ₹${complaint.estimatedCost}", style = MaterialTheme.typography.labelSmall, color = GreyTextDark)
            }
            Text(formattedDate, style = MaterialTheme.typography.labelSmall, color = GreyTextLight)
            if (complaint.resolutionNote != null) {
                Text("Note: ${complaint.resolutionNote}", style = MaterialTheme.typography.bodySmall, color = GreyTextDark, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                Text(
                    complaint.status.name.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (complaint.status) {
                    Status.SUBMITTED -> {
                        Button(
                            onClick = onStartWork,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreyBg, contentColor = GreyTextDark),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Start Work", fontWeight = FontWeight.Medium) }
                    }
                    Status.IN_PROGRESS -> {
                        Button(
                            onClick = onResolve,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Mark Resolved", fontWeight = FontWeight.Medium) }
                        OutlinedButton(
                            onClick = onMessageAdmin,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, GreyBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreyTextDark)
                        ) { Text("Ask Help") }
                    }
                    Status.RESOLVED -> {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(GreyBg).padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Completed", color = GreyTextDark, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

// ---------- NEW: Complaint detail dialog with messaging for employee ----------
@Composable
fun EmployeeComplaintDetailDialogWithMessaging(
    complaint: Complaint,
    employeeId: String,
    employeeName: String,
    firestore: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(complaint.dateTime))
    val statusColor = when (complaint.status) {
        Status.SUBMITTED -> AccentOrange
        Status.IN_PROGRESS -> AccentBlue
        Status.RESOLVED -> AccentGreen
    }
    val imageUri = complaint.imageUri
    val isFileOrContent = imageUri.isNotEmpty() &&
            (imageUri.startsWith("content://") || imageUri.startsWith("file://") || File(imageUri).exists())
    var base64Bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(imageUri) {
        if (!isFileOrContent && imageUri.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val rawBase64 = if (imageUri.contains(",")) imageUri.substringAfter(",") else imageUri
                    val bytes = Base64.decode(rawBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) { null }
            }.let { base64Bitmap = it }
        }
    }

    // Messaging state – only with admin (find the first admin)
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var adminId by remember { mutableStateOf<String?>(null) }
    var adminName by remember { mutableStateOf("Admin") }
    var recipientName by remember { mutableStateOf("Admin") }
    val recipientId = adminId ?: ""

    // Fetch admin user (first user with role "admin")
    LaunchedEffect(Unit) {
        val adminQuery = firestore.collection("users").whereEqualTo("role", "admin").limit(1).get().await()
        val adminDoc = adminQuery.documents.firstOrNull()
        if (adminDoc != null) {
            adminId = adminDoc.id
            adminName = adminDoc.getString("name") ?: "Admin"
            recipientName = adminName
        } else {
            Toast.makeText(context, "No admin found", Toast.LENGTH_SHORT).show()
        }
    }

    // Load messages for this complaint
    val messagesFlow = remember(complaint.id, employeeId) {
        callbackFlow {
            val listener = firestore.collection("messages")
                .whereEqualTo("complaintId", complaint.id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Error loading messages: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                        close(error)
                        return@addSnapshotListener
                    }
                    val msgList = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(msgList.sortedBy { it.timestamp })
                }
            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }
    }

    messagesFlow.collectAsState(initial = emptyList()).let { state ->
        messages = state.value
    }

    fun sendMessage() {
        if (messageText.isBlank() || recipientId.isBlank()) return
        val msg = messageText
        messageText = ""
        scope.launch {
            val newMessage = Message(
                complaintId = complaint.id,
                fromUserId = employeeId,
                fromUserName = employeeName,
                toUserId = recipientId,
                text = msg,
                timestamp = System.currentTimeMillis()
            )
            try {
                firestore.collection("messages").add(newMessage).await()
                val notificationHelper = NotificationHelper(context)
                notificationHelper.sendToUser(
                    recipientId,
                    "New message from Employee",
                    "$employeeName: $msg",
                    complaint.id
                )
                Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isRatedResolved = (complaint.status == Status.RESOLVED && complaint.rating > 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    complaint.issueType.name.replace("_", " "),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GreyTextDark,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (complaint.status == Status.RESOLVED && complaint.rating > 0) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < complaint.rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.15f)) {
                        Text(
                            complaint.status.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = statusColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 550.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Complaint details
                item {
                    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(GreyBg), contentAlignment = Alignment.Center) {
                        when {
                            isFileOrContent -> Icon(Icons.Filled.Image, "Image", modifier = Modifier.size(64.dp), tint = GreyTextMid)
                            base64Bitmap != null -> Image(bitmap = base64Bitmap!!.asImageBitmap(), contentDescription = "Complaint image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else -> Icon(Icons.Filled.BrokenImage, "No image", modifier = Modifier.size(64.dp), tint = GreyTextLight)
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Description", fontWeight = FontWeight.Bold, color = GreyTextDark)
                            Spacer(Modifier.height(4.dp))
                            Text(complaint.description.ifEmpty { "No description" }, color = GreyTextDark)
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp), tint = GreyTextMid)
                            Spacer(Modifier.width(8.dp))
                            Text(complaint.location, color = GreyTextDark)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Reported on", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = GreyTextDark)
                            }
                        }
                        if (!isRatedResolved) {
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg), modifier = Modifier.weight(1f)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Estimated Cost", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                    Text("₹ ${complaint.estimatedCost}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GreyTextDark)
                                }
                            }
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = GreyTextMid)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Reported by", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                Text("Citizen", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = GreyTextDark)
                            }
                        }
                    }
                }
                if (complaint.assignedTo != null && complaint.assignedTo!!.isNotBlank() && !isRatedResolved) {
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp), tint = GreyTextMid)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Assigned to", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                    Text(employeeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = GreyTextDark)
                                }
                            }
                        }
                    }
                }
                if (!complaint.resolutionNote.isNullOrBlank()) {
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Resolution Note", fontWeight = FontWeight.Bold, color = GreyTextDark)
                                Spacer(Modifier.height(4.dp))
                                Text(complaint.resolutionNote, color = GreyTextDark)
                            }
                        }
                    }
                }

                // Chat section
                item {
                    HorizontalDivider(color = GreyBorder, thickness = 1.dp)
                    Text("Chat with Admin", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GreyTextDark)
                }

                // Messages list – each message shows sender name
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.fromUserId == employeeId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MyMessageBubble else OtherMessageBubble
                            ),
                            modifier = Modifier.widthIn(max = 250.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Show sender name for incoming messages
                                if (!isMe) {
                                    Text(msg.fromUserName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreyTextDark)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(msg.text, color = if (isMe) Color.White else GreyTextDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else GreyTextLight
                                )
                            }
                        }
                    }
                }

                // Message input field
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Type a reply...", color = GreyTextLight) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreyBorder,
                                unfocusedBorderColor = GreyBorder,
                                focusedTextColor = GreyTextDark,
                                unfocusedTextColor = GreyTextDark
                            ),
                            maxLines = 3
                        )
                        Button(
                            onClick = { sendMessage() },
                            enabled = messageText.isNotBlank() && recipientId.isNotBlank(),
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = GreyBorder, disabledContainerColor = GreyTextLight),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = GreyTextDark)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = GreyTextDark) }
        }
    )
}

// Helper function that was referenced (kept for compatibility)
