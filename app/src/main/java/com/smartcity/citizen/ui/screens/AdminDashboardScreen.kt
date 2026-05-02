package com.smartcity.citizen.ui.screens

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.models.Complaint
import com.smartcity.citizen.data.models.IssueType
import com.smartcity.citizen.data.models.Status
import com.smartcity.citizen.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ---- Data classes ----
data class Employee(val id: String, val name: String, val email: String)
data class EmployeeStats(
    val id: String,
    val name: String,
    val email: String,
    val totalAssigned: Int,
    val submitted: Int,
    val inProgress: Int,
    val resolved: Int,
    val complaints: List<Complaint>
)
data class AdminNotification(
    val id: String,
    val title: String,
    val body: String,
    val read: Boolean,
    val timestamp: Date,
    val complaintId: String = ""
)

data class IssueCount(val type: IssueType, val count: Int)

// ✅ UNCOMMENTED Message data class
//data class Message(
//    val id: String = "",
//    val complaintId: String = "",
//    val fromUserId: String = "",
//    val fromUserName: String = "",
//    val toUserId: String = "",
//    val text: String = "",
//    val timestamp: Long = System.currentTimeMillis()
//)

// ---- Grey colour palette ----
private val GreyBg = Color(0xFFF5F5F5)
private val GreyBorder = Color(0xFFE0E0E0)
private val GreyTextDark = Color(0xFF424242)
private val GreyTextMid = Color(0xFF757575)
private val GreyTextLight = Color(0xFF9E9E9E)
private val GreenResolved = Color(0xFF2E7D32)
private val OrangeAccent = Color(0xFFFFA500)
private val BlueAccent = Color(0xFF2196F3)
private val MyMessageBubble = Color(0xFF2196F3)
private val OtherMessageBubble = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ComplaintRepository() }
    val firestore = FirebaseFirestore.getInstance()
    val helper = remember { NotificationHelper(context) }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ---- State ----
    val allComplaints by repo.getAllComplaints().collectAsState(initial = emptyList())

    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var employeesStats by remember { mutableStateOf<List<EmployeeStats>>(emptyList()) }
    var citizenNameCache = mutableMapOf<String, String>()

    suspend fun updateEmployeeStats() {
        val empStatsList = employees.map { emp ->
            val assigned = allComplaints.filter { it.assignedTo == emp.id }
            EmployeeStats(
                id = emp.id,
                name = emp.name,
                email = emp.email,
                totalAssigned = assigned.size,
                submitted = assigned.count { it.status == Status.SUBMITTED },
                inProgress = assigned.count { it.status == Status.IN_PROGRESS },
                resolved = assigned.count { it.status == Status.RESOLVED },
                complaints = assigned
            )
        }
        employeesStats = empStatsList.sortedByDescending { it.totalAssigned }
    }

    LaunchedEffect(Unit) {
        val snap = firestore.collection("users")
            .whereEqualTo("role", "employee")
            .get()
            .await()
        employees = snap.documents.mapNotNull {
            Employee(it.id, it.getString("name") ?: "Unknown Employee", it.getString("email") ?: "")
        }
        updateEmployeeStats()
    }

    LaunchedEffect(allComplaints) {
        updateEmployeeStats()
    }

    var notifications by remember { mutableStateOf<List<AdminNotification>>(emptyList()) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUid) {
        firestore.collection("notifications")
            .whereEqualTo("userId", currentUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) return@addSnapshotListener
                notifications = snap?.documents?.mapNotNull { doc ->
                    AdminNotification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        read = doc.getBoolean("read") ?: false,
                        timestamp = doc.getLong("timestamp")?.let { Date(it) } ?: Date(),
                        complaintId = doc.getString("complaintId") ?: ""
                    )
                } ?: emptyList()
                unreadCount = notifications.count { !it.read }
            }
    }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<Status?>(null) }

    val filtered by remember(allComplaints, searchQuery, statusFilter) {
        derivedStateOf {
            allComplaints.filter { c ->
                (statusFilter == null || c.status == statusFilter) &&
                        (searchQuery.isBlank() || c.issueType.name.contains(searchQuery, ignoreCase = true) ||
                                c.description.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    val total = allComplaints.size
    val submitted = allComplaints.count { it.status == Status.SUBMITTED }
    val inProgress = allComplaints.count { it.status == Status.IN_PROGRESS }
    val resolved = allComplaints.count { it.status == Status.RESOLVED }
    val totalBudget = allComplaints.sumOf { it.estimatedCost }

    val issueBreakdownList = remember(allComplaints) {
        allComplaints.groupBy { it.issueType }
            .map { IssueCount(it.key, it.value.size) }
            .sortedByDescending { it.count }
    }

    val resolutionRate = if (total > 0) (resolved.toFloat() / total * 100).toInt() else 0

    var assignTarget by remember { mutableStateOf<Complaint?>(null) }
    var budgetTarget by remember { mutableStateOf<Complaint?>(null) }
    var deleteTarget by remember { mutableStateOf<Complaint?>(null) }
    var selectedNotification by remember { mutableStateOf<AdminNotification?>(null) }
    var selectedComplaintForDetail by remember { mutableStateOf<Complaint?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }
    var selectedIssueType by remember { mutableStateOf<IssueType?>(null) }
    var showIssueDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // ---- Navigation Drawer (unchanged, same as yours) ----
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
                    Text("Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreyTextDark)
                    Text("admin@smartcity.com", style = MaterialTheme.typography.bodySmall, color = GreyTextMid)
                }
                HorizontalDivider(color = GreyBorder, modifier = Modifier.padding(vertical = 8.dp))
                Column(Modifier.fillMaxWidth()) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null, tint = GreyTextMid) },
                        label = { Text("Profile", color = GreyTextDark) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("admin_profile")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Info, contentDescription = null, tint = GreyTextMid) },
                        label = { Text("About", color = GreyTextDark) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("about")
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
                            scope.launch {
                                drawerState.close()
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
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
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = GreyTextDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    actions = {
                        IconButton(onClick = { showNotificationsDialog = true }) {
                            BadgedBox(badge = {
                                if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                            }) {
                                Icon(Icons.Default.Notifications, "Notifications", tint = GreyTextDark)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Admin Overview
                item {
                    Text("Admin Overview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreyTextDark, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Total", total.toString(), GreyTextDark, Modifier.weight(1f))
                        StatCard("Submitted", submitted.toString(), OrangeAccent, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("In Progress", inProgress.toString(), BlueAccent, Modifier.weight(1f))
                        StatCard("Resolved", resolved.toString(), GreenResolved, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Budget Card
                item {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = GreyBg), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = GreyTextMid, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Total Budget Utilised", style = MaterialTheme.typography.titleMedium, color = GreyTextDark)
                                Text(text = "₹ ${String.format("%,.2f", totalBudget.toDouble())}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = GreyTextDark)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Issue Breakdown
                item {
                    Text("Issue Breakdown", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GreyTextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (issueBreakdownList.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No data available", color = GreyTextMid)
                        }
                    }
                } else {
                    items(issueBreakdownList, key = { it.type }) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = GreyBg),
                            modifier = Modifier.clickable {
                                selectedIssueType = item.type
                                showIssueDialog = true
                            }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GreyTextDark
                                )
                                Text("${item.count}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GreyTextDark)
                            }
                        }
                    }
                }

                // Resolution Rate
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Resolution Rate", style = MaterialTheme.typography.titleMedium, color = GreyTextDark)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$resolutionRate%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = GreenResolved)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$resolved out of $total complaints resolved", color = GreyTextMid)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ==================== COMPLAINTS MANAGEMENT ====================
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by issue or description", color = GreyTextLight) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = GreyTextMid) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreyBorder,
                            unfocusedBorderColor = GreyBorder,
                            focusedLabelColor = GreyTextMid,
                            cursorColor = GreyTextDark,
                            focusedTextColor = GreyTextDark,
                            unfocusedTextColor = GreyTextDark
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("All", color = GreyTextDark) })
                        Status.entries.forEach { s ->
                            FilterChip(selected = statusFilter == s, onClick = { statusFilter = s }, label = {
                                Text(s.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, color = GreyTextDark)
                            })
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, Modifier.size(72.dp), tint = GreyTextLight)
                                Text("No complaints found", style = MaterialTheme.typography.titleLarge, color = GreyTextLight)
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { complaint ->
                        var citizenName by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(complaint.reportedBy) {
                            if (complaint.reportedBy.isNotBlank()) {
                                val cached = citizenNameCache[complaint.reportedBy]
                                if (cached != null) {
                                    citizenName = cached
                                } else {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val doc = firestore.collection("users").document(complaint.reportedBy).get().await()
                                            val name = doc.getString("name") ?: "Citizen"
                                            citizenNameCache[complaint.reportedBy] = name
                                            citizenName = name
                                        } catch (e: Exception) {
                                            citizenName = "Citizen"
                                        }
                                    }
                                }
                            } else {
                                citizenName = "Unknown"
                            }
                        }
                        AdminComplaintCard(
                            complaint = complaint,
                            citizenName = citizenName ?: "Loading...",
                            employees = employees,
                            onCardClick = { selectedComplaintForDetail = complaint },
                            onAssign = { assignTarget = complaint },
                            onStatusChange = { newStatus ->
                                scope.launch {
                                    repo.updateStatus(complaint.id, newStatus)
                                    if (newStatus == Status.RESOLVED && complaint.reportedBy.isNotBlank()) {
                                        helper.sendToUser(complaint.reportedBy, "Complaint Resolved", "Your complaint #${complaint.id.take(8)} has been resolved.", complaint.id)
                                    }
                                }
                            },
                            onBudget = { budgetTarget = complaint },
                            onDelete = { deleteTarget = complaint }
                        )
                    }
                }

                // ==================== EMPLOYEE MANAGEMENT ====================
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Employee Management", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GreyTextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (employeesStats.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No employees registered", color = GreyTextMid)
                        }
                    }
                } else {
                    items(employeesStats, key = { it.id }) { emp ->
                        var isExpanded by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(emp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = GreyTextDark)
                                        Text(emp.email, style = MaterialTheme.typography.bodySmall, color = GreyTextMid)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Tasks: ${emp.totalAssigned} | Done: ${emp.resolved} | Pending: ${emp.inProgress + emp.submitted}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GreyTextMid
                                        )
                                    }
                                    IconButton(
                                        onClick = { employeeToDelete = Employee(emp.id, emp.name, emp.email) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                    IconButton(
                                        onClick = { isExpanded = !isExpanded },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = GreyTextDark)
                                    }
                                }

                                if (isExpanded && emp.complaints.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = GreyBorder)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Assigned Complaints", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreyTextDark)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    emp.complaints.forEach { complaint ->
                                        val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(complaint.dateTime))
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = GreyBg),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(complaint.issueType.name.replace("_", " "), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = GreyTextDark)
                                                    Text(complaint.status.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = when (complaint.status) {
                                                        Status.SUBMITTED -> OrangeAccent
                                                        Status.IN_PROGRESS -> BlueAccent
                                                        Status.RESOLVED -> GreenResolved
                                                    })
                                                }
                                                Text(complaint.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = GreyTextMid)
                                                Text("Budget: ₹${complaint.estimatedCost}   |   $dateStr", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                            }
                                        }
                                    }
                                } else if (isExpanded && emp.complaints.isEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No complaints assigned", style = MaterialTheme.typography.bodySmall, color = GreyTextMid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Dialog for Issue Breakdown Click (full details) ----
    if (showIssueDialog && selectedIssueType != null) {
        val filteredComplaints = allComplaints.filter { it.issueType == selectedIssueType }
        AlertDialog(
            onDismissRequest = { showIssueDialog = false },
            title = {
                Text(
                    text = selectedIssueType!!.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    color = GreyTextDark
                )
            },
            text = {
                if (filteredComplaints.isEmpty()) {
                    Box(Modifier.padding(16.dp)) {
                        Text("No complaints of this type.", color = GreyTextMid)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredComplaints, key = { it.id }) { complaint ->
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            val dateStr = dateFormat.format(Date(complaint.dateTime))
                            val statusColor = when (complaint.status) {
                                Status.SUBMITTED -> OrangeAccent
                                Status.IN_PROGRESS -> BlueAccent
                                Status.RESOLVED -> GreenResolved
                            }
                            val assignedEmployee = employees.find { it.id == complaint.assignedTo }
                            val assignedName = assignedEmployee?.name ?: "Unassigned"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = GreyBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            complaint.issueType.name.replace("_", " "),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GreyTextDark
                                        )
                                        Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.15f)) {
                                            Text(
                                                complaint.status.name.replace("_", " "),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                color = statusColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Text(
                                        "Description: ${complaint.description}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GreyTextDark
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = GreyTextMid)
                                        Spacer(Modifier.width(4.dp))
                                        Text(complaint.location, style = MaterialTheme.typography.bodySmall, color = GreyTextMid)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Reported: $dateStr", style = MaterialTheme.typography.labelSmall, color = GreyTextLight)
                                        Text("Budget: ₹${complaint.estimatedCost}", style = MaterialTheme.typography.labelSmall, color = GreyTextDark, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp), tint = GreyTextMid)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Assigned to: $assignedName", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                    }
                                    if (complaint.status == Status.RESOLVED && complaint.rating > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Rating: ", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                            repeat(5) { index ->
                                                Icon(
                                                    imageVector = if (index < complaint.rating) Icons.Filled.Star else Icons.Outlined.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFA726),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (!complaint.resolutionNote.isNullOrBlank()) {
                                        Text(
                                            "Resolution: ${complaint.resolutionNote}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GreyTextDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIssueDialog = false }) {
                    Text("Close", color = GreyTextDark)
                }
            }
        )
    }

    // ---- Delete Employee Dialog ----
    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            title = { Text("Delete Employee", color = GreyTextDark) },
            text = { Text("Are you sure you want to delete ${employeeToDelete?.name}? This action cannot be undone.", color = GreyTextMid) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val success = repo.deleteEmployee(employeeToDelete!!.id)
                                if (success) {
                                    val assignedComplaints = allComplaints.filter { it.assignedTo == employeeToDelete!!.id }
                                    assignedComplaints.forEach { complaint ->
                                        repo.assignEmployee(complaint.id, "")
                                    }
                                    employees = employees.filter { it.id != employeeToDelete!!.id }
                                    updateEmployeeStats()
                                    Toast.makeText(context, "Employee deleted", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to delete employee", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            employeeToDelete = null
                        }
                    }
                ) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) { Text("Cancel", color = GreyTextMid) }
            }
        )
    }

    // ✅ Complaint Detail Dialog with Messaging (FULL IMPLEMENTATION)
    if (selectedComplaintForDetail != null) {
        ComplaintDetailDialogWithMessaging(
            complaint = selectedComplaintForDetail!!,
            employees = employees,
            onDismiss = { selectedComplaintForDetail = null },
            adminUid = currentUid,
            firestore = firestore
        )
    }

    // ---- Assign Dialog ----
    AssignDialog(assignTarget, employees, { assignTarget = null }) { id ->
        scope.launch {
            assignTarget?.let {
                repo.assignEmployee(it.id, id)
                helper.sendToUser(id, "New Task", "Complaint #${it.id.take(8)} assigned.", it.id)
                Toast.makeText(context, "Assigned", Toast.LENGTH_SHORT).show()
            }
            assignTarget = null
        }
    }

    // ---- Budget Dialog ----
    BudgetDialog(budgetTarget, { budgetTarget = null }) { cId, amt ->
        scope.launch {
            firestore.collection("complaints").document(cId).update("estimatedCost", amt).await()
            Toast.makeText(context, "Budget updated", Toast.LENGTH_SHORT).show()
            budgetTarget = null
        }
    }

    // ---- Delete Complaint Dialog ----
    DeleteDialog(deleteTarget, { deleteTarget = null }) {
        scope.launch {
            deleteTarget?.let { repo.deleteComplaint(it.id) }
            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
            deleteTarget = null
        }
    }

    // ---- Notifications Dialog ----
    if (showNotificationsDialog) {
        NotificationsFullScreenDialog(
            notifications = notifications,
            onDismiss = { showNotificationsDialog = false },
            onNotificationClick = { notif ->
                scope.launch {
                    firestore.collection("notifications").document(notif.id).update("read", true).await()
                    showNotificationsDialog = false
                    selectedNotification = notif
                }
            },
            onMarkAllRead = {
                scope.launch {
                    notifications.filter { !it.read }.forEach { notif ->
                        firestore.collection("notifications").document(notif.id).update("read", true).await()
                    }
                }
            }
        )
    }

    // ---- Notification Detail Dialog ----
    if (selectedNotification != null) {
        val notif = selectedNotification!!
        var complaintForNotif by remember { mutableStateOf<Complaint?>(null) }
        LaunchedEffect(notif.complaintId) {
            if (notif.complaintId.isNotBlank()) {
                complaintForNotif = repo.getComplaintById(notif.complaintId)
            }
        }
        AlertDialog(
            onDismissRequest = { selectedNotification = null },
            title = { Text(notif.title, color = GreyTextDark) },
            text = {
                Column {
                    Text(notif.body, style = MaterialTheme.typography.bodyLarge, color = GreyTextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(notif.timestamp), style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
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
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = GreyBg), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = modifier.height(96.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = GreyTextMid, fontWeight = FontWeight.Medium)
        }
    }
}

// ---------- Admin Complaint Card ----------
@Composable
fun AdminComplaintCard(
    complaint: Complaint,
    citizenName: String,
    employees: List<Employee>,
    onCardClick: () -> Unit,
    onAssign: () -> Unit,
    onStatusChange: (Status) -> Unit,
    onBudget: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(complaint.dateTime))
    val statusColor = when (complaint.status) {
        Status.SUBMITTED -> Color(0xFFBDBDBD)
        Status.IN_PROGRESS -> Color(0xFF757575)
        Status.RESOLVED -> GreenResolved
    }
    var showStatusDialog by remember { mutableStateOf(false) }
    val assignedEmployee = employees.find { it.id == complaint.assignedTo }
    val assignedName = assignedEmployee?.name ?: "Unassigned"
    val isResolvedAndRated = (complaint.status == Status.RESOLVED && complaint.rating > 0)

    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    complaint.issueType.name.replace("_", " "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreyTextDark,
                    modifier = Modifier.weight(1f)
                )
                if (complaint.status == Status.RESOLVED && complaint.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                }
                Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        complaint.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                complaint.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = GreyTextDark,
                modifier = Modifier.clickable(onClick = onCardClick)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Assigned", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                    Text(assignedName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = GreyTextDark)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Reported by", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                    Text(citizenName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = GreyTextDark)
                }
            }
            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = GreyTextMid, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(20.dp))

            if (!isResolvedAndRated) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStatusDialog = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.SwapHoriz, null, Modifier.size(18.dp), tint = GreyTextMid)
                        Spacer(Modifier.width(6.dp))
                        Text("Status", color = GreyTextDark)
                    }
                    OutlinedButton(onClick = onAssign, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp), tint = GreyTextMid)
                        Spacer(Modifier.width(6.dp))
                        Text("Assign", color = GreyTextDark)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = onBudget, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.AttachMoney, null, Modifier.size(18.dp), tint = GreyTextMid)
                        Spacer(Modifier.width(6.dp))
                        Text("Budget", color = GreyTextDark)
                    }
                    Button(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = GreyBg)) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = GreyTextDark)
                    }
                }
            }
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Status", color = GreyTextDark) },
            text = {
                Column {
                    Status.entries.forEach { status ->
                        val isSelected = complaint.status == status
                        val col = when (status) {
                            Status.SUBMITTED -> GreyTextMid
                            Status.IN_PROGRESS -> GreyTextDark
                            Status.RESOLVED -> GreenResolved
                        }
                        Surface(modifier = Modifier.fillMaxWidth().clickable { onStatusChange(status); showStatusDialog = false }, shape = RoundedCornerShape(8.dp), color = if (isSelected) col.copy(alpha = 0.08f) else Color.White) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = GreyTextDark))
                                Spacer(Modifier.width(12.dp))
                                Text(status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, color = GreyTextDark)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showStatusDialog = false }) { Text("Cancel", color = GreyTextMid) } }
        )
    }
}

// ---------- Complaint Detail Dialog with Messaging (FULL IMPLEMENTATION) ----------
// ---------- Complaint Detail Dialog with Messaging (FIXED: always chat with citizen) ----------
@Composable
fun ComplaintDetailDialogWithMessaging(
    complaint: Complaint,
    employees: List<Employee>,
    onDismiss: () -> Unit,
    adminUid: String,
    firestore: FirebaseFirestore
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(complaint.dateTime))
    val assignedEmployee = employees.find { it.id == complaint.assignedTo }
    val assignedName = assignedEmployee?.name ?: "Not assigned"
    val statusColor = when (complaint.status) {
        Status.SUBMITTED -> OrangeAccent
        Status.IN_PROGRESS -> BlueAccent
        Status.RESOLVED -> GreenResolved
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

    // Always chat with the citizen who reported the complaint
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val citizenId = complaint.reportedBy
    var citizenName by remember { mutableStateOf<String?>(null) }
    var adminName by remember { mutableStateOf("Admin") }

    LaunchedEffect(Unit) {
        val adminDoc = firestore.collection("users").document(adminUid).get().await()
        adminName = adminDoc.getString("name") ?: "Admin"
    }

    LaunchedEffect(citizenId) {
        if (citizenId.isNotBlank()) {
            val userDoc = firestore.collection("users").document(citizenId).get().await()
            citizenName = userDoc.getString("name") ?: "Citizen"
        } else {
            citizenName = "Citizen"
        }
    }

    // ✅ FILTER: only messages between admin and this citizen
    val messagesFlow = remember(complaint.id, adminUid, citizenId) {
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
                    val allMessages = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    // Keep only messages where admin and citizen are the two participants
                    val filtered = allMessages.filter { msg ->
                        (msg.fromUserId == adminUid && msg.toUserId == citizenId) ||
                                (msg.fromUserId == citizenId && msg.toUserId == adminUid)
                    }
                    trySend(filtered.sortedBy { it.timestamp })
                }
            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }
    }

    messagesFlow.collectAsState(initial = emptyList()).let { state ->
        messages = state.value
    }

    fun sendMessage() {
        if (messageText.isBlank() || citizenId.isBlank()) return
        val msg = messageText
        messageText = ""
        scope.launch {
            val newMessage = Message(
                complaintId = complaint.id,
                fromUserId = adminUid,
                fromUserName = adminName,
                toUserId = citizenId,
                text = msg,
                timestamp = System.currentTimeMillis()
            )
            try {
                firestore.collection("messages").add(newMessage).await()
                val notificationHelper = NotificationHelper(context)
                notificationHelper.sendToUser(
                    citizenId,
                    "New message from Admin",
                    "Regarding complaint #${complaint.id.take(8)}: $msg",
                    complaint.id
                )
                Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isRatedResolved = (complaint.status == Status.RESOLVED && complaint.rating > 0)
    val chatTitle = "Chat with ${citizenName ?: "Citizen"} (Citizen)"

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
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Complaint image
                item {
                    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)).background(GreyBg), contentAlignment = Alignment.Center) {
                        when {
                            isFileOrContent -> Icon(Icons.Filled.Image, "Image", modifier = Modifier.size(64.dp), tint = GreyTextMid)
                            base64Bitmap != null -> Image(bitmap = base64Bitmap!!.asImageBitmap(), contentDescription = "Complaint image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else -> Icon(Icons.Filled.BrokenImage, "No image", modifier = Modifier.size(64.dp), tint = GreyTextLight)
                        }
                    }
                }
                // Description
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Description", fontWeight = FontWeight.Bold, color = GreyTextDark)
                            Spacer(Modifier.height(4.dp))
                            Text(complaint.description.ifEmpty { "No description" }, color = GreyTextDark)
                        }
                    }
                }
                // Location
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp), tint = GreyTextMid)
                            Spacer(Modifier.width(8.dp))
                            Text(complaint.location, color = GreyTextDark)
                        }
                    }
                }
                // Reported date and budget
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
                // Reported by
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
                // Assigned to (if not resolved & rated)
                if (!isRatedResolved && complaint.assignedTo != null && complaint.assignedTo!!.isNotBlank()) {
                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = GreyBg)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp), tint = GreyTextMid)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Assigned to", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                    Text(assignedName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = GreyTextDark)
                                }
                            }
                        }
                    }
                }
                // Resolution note
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
                    Text(chatTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GreyTextDark)
                }

                // Only show messages between admin and this citizen
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.fromUserId == adminUid
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

                // Message input
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
                            enabled = messageText.isNotBlank() && citizenId.isNotBlank(),
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = GreyBorder, disabledContainerColor = GreyTextLight),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = GreyTextDark)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = GreyTextDark) }
        }
    )
}

// ---------- Assign Dialog ----------
@Composable
fun AssignDialog(complaint: Complaint?, employees: List<Employee>, onDismiss: () -> Unit, onAssign: (String) -> Unit) {
    if (complaint == null) return
    var selectedId by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign to Employee", color = GreyTextDark) },
        text = {
            if (employees.isEmpty()) Text("No employees found", color = GreyTextMid)
            else Column {
                employees.forEach { emp ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedId = emp.id }.padding(vertical = 8.dp)) {
                        RadioButton(selected = selectedId == emp.id, onClick = { selectedId = emp.id }, colors = RadioButtonDefaults.colors(selectedColor = GreyTextDark))
                        Spacer(Modifier.width(12.dp))
                        Text(emp.name, color = GreyTextDark)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (selectedId.isNotEmpty()) onAssign(selectedId) }, enabled = selectedId.isNotEmpty()) { Text("Assign", color = GreyTextDark) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GreyTextMid) } }
    )
}

// ---------- Budget Dialog ----------
@Composable
fun BudgetDialog(complaint: Complaint?, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    if (complaint == null) return
    var cost by remember { mutableStateOf(complaint.estimatedCost.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Budget", color = GreyTextDark) },
        text = {
            OutlinedTextField(
                value = cost,
                onValueChange = { cost = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Estimated Cost (₹)", color = GreyTextMid) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreyBorder,
                    unfocusedBorderColor = GreyBorder,
                    focusedLabelColor = GreyTextMid,
                    cursorColor = GreyTextDark,
                    focusedTextColor = GreyTextDark,
                    unfocusedTextColor = GreyTextDark
                )
            )
        },
        confirmButton = { TextButton(onClick = { val amt = cost.toDoubleOrNull() ?: 0.0; onSave(complaint.id, amt) }) { Text("Save", color = GreyTextDark) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GreyTextMid) } }
    )
}

// ---------- Delete Dialog ----------
@Composable
fun DeleteDialog(complaint: Complaint?, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (complaint == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Complaint", color = GreyTextDark) },
        text = { Text("Are you sure you want to permanently delete this complaint?\n\nThis action cannot be undone.", color = GreyTextMid) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", fontWeight = FontWeight.Bold, color = Color.Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GreyTextMid) } }
    )
}

// ---------- Notifications Full-Screen Dialog ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsFullScreenDialog(
    notifications: List<AdminNotification>,
    onDismiss: () -> Unit,
    onNotificationClick: (AdminNotification) -> Unit,
    onMarkAllRead: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notifications", fontWeight = FontWeight.Bold, color = GreyTextDark) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GreyTextDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    actions = {
                        TextButton(onClick = onMarkAllRead) { Text("Mark all read", color = GreyTextMid) }
                    }
                )
            }
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                if (notifications.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notifications yet", color = GreyTextMid)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notifications, key = { it.id }) { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onNotificationClick(notif) },
                                colors = CardDefaults.cardColors(containerColor = if (!notif.read) GreyBg else Color.White)
                            ) {
                                Row(Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Notifications, null, tint = if (!notif.read) GreyTextDark else GreyTextMid, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(notif.title, fontWeight = if (!notif.read) FontWeight.Bold else FontWeight.Normal, color = GreyTextDark)
                                        Text(notif.body, style = MaterialTheme.typography.bodyMedium, color = GreyTextMid)
                                        Text(notif.timestamp?.let { dateFormat.format(it) } ?: "", style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}