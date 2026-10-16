package com.smartcity.citizen.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.models.Complaint
import com.smartcity.citizen.data.models.Status
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ---- Grey colour palette (consistent with other screens) ----
private val GreyBg = Color(0xFFF5F5F5)
private val GreyBorder = Color(0xFFE0E0E0)
private val GreyTextDark = Color(0xFF424242)
private val GreyTextMid = Color(0xFF757575)
private val GreyTextLight = Color(0xFF9E9E9E)
private val GreenResolved = Color(0xFF2E7D32)
private val OrangeAccent = Color(0xFFFFA500)
private val BlueAccent = Color(0xFF2196F3)



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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen() {
    val repo = remember { ComplaintRepository() }
    val firestore = FirebaseFirestore.getInstance()

    val allComplaints by repo.getAllComplaints().collectAsState(initial = emptyList())
    var employees by remember { mutableStateOf<List<EmployeeStats>>(emptyList()) }

    LaunchedEffect(allComplaints) {
        val snap = firestore.collection("users")
            .whereEqualTo("role", "employee")
            .get()
            .await()
        val empList = snap.documents.mapNotNull { doc ->
            val empId = doc.id
            val name = doc.getString("name") ?: "Employee"
            val email = doc.getString("email") ?: ""
            val assigned = allComplaints.filter { it.assignedTo == empId }
            EmployeeStats(
                id = empId,
                name = name,
                email = email,
                totalAssigned = assigned.size,
                submitted = assigned.count { it.status == Status.SUBMITTED },
                inProgress = assigned.count { it.status == Status.IN_PROGRESS },
                resolved = assigned.count { it.status == Status.RESOLVED },
                complaints = assigned
            )
        }
        employees = empList.sortedByDescending { it.totalAssigned }
    }

    val total = allComplaints.size
    val submitted = allComplaints.count { it.status == Status.SUBMITTED }
    val inProgress = allComplaints.count { it.status == Status.IN_PROGRESS }
    val resolved = allComplaints.count { it.status == Status.RESOLVED }
    val totalBudget = allComplaints.sumOf { it.estimatedCost }  // Int

    val issueBreakdown = allComplaints.groupBy { it.issueType }
        .mapValues { it.value.size }

    var expandedEmployeeId by remember { mutableStateOf<String?>(null) }

    // No top bar – simple scaffold with white background
    Scaffold(
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Centered title
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Analytics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreyTextDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Overall stats cards
            item {
                Text("Overview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GreyTextDark)
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticsCard("Total", total.toString(), GreyTextDark, Modifier.weight(1f))
                    AnalyticsCard("Submitted", submitted.toString(), OrangeAccent, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticsCard("In Progress", inProgress.toString(), BlueAccent, Modifier.weight(1f))
                    AnalyticsCard("Resolved", resolved.toString(), GreenResolved, Modifier.weight(1f))
                }
            }

            // Budget card (FIX: convert totalBudget to Double)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GreyBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = GreyTextMid, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Total Budget Utilised", style = MaterialTheme.typography.titleMedium, color = GreyTextDark)
                            Text(
                                "₹ ${String.format("%,.2f", totalBudget.toDouble())}",  // ✅ fixed
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = GreyTextDark
                            )
                        }
                    }
                }
            }

            // Issue breakdown
            item {
                Text("Issue Breakdown", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GreyTextDark)
            }

            if (issueBreakdown.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No data available", color = GreyTextMid)
                    }
                }
            } else {
                items(issueBreakdown.entries.toList()) { (type, count) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GreyBg)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                color = GreyTextDark
                            )
                            Text(
                                "$count",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GreyTextDark
                            )
                        }
                    }
                }
            }

            // Resolution rate
            item {
                val resolutionRate = if (total > 0) (resolved.toFloat() / total * 100).toInt() else 0
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GreyBg)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Resolution Rate", style = MaterialTheme.typography.titleMedium, color = GreyTextDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$resolutionRate%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenResolved
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$resolved out of $total complaints resolved", color = GreyTextMid)
                    }
                }
            }

            // Employee performance
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Employee Performance", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GreyTextDark)
            }

            if (employees.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No employees registered", color = GreyTextMid)
                    }
                }
            } else {
                items(employees, key = { it.id }) { emp ->
                    val isExpanded = expandedEmployeeId == emp.id
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    expandedEmployeeId = if (isExpanded) null else emp.id
                                },
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
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = GreyTextDark
                                )
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
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    complaint.issueType.name.replace("_", " "),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = GreyTextDark
                                                )
                                                Text(
                                                    complaint.status.name.replace("_", " "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (complaint.status) {
                                                        Status.SUBMITTED -> OrangeAccent
                                                        Status.IN_PROGRESS -> BlueAccent
                                                        Status.RESOLVED -> GreenResolved
                                                    }
                                                )
                                            }
                                            Text(
                                                complaint.description,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GreyTextMid
                                            )
                                            Text(
                                                "Budget: ₹${complaint.estimatedCost}   |   $dateStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GreyTextMid
                                            )
                                            if (complaint.reportedBy.isNotBlank()) {
                                                Text(
                                                    "Citizen: ${complaint.reportedBy.take(8)}...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GreyTextLight
                                                )
                                            }
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

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GreyBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = GreyTextMid)
        }
    }
}