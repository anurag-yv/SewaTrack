package com.smartcity.citizen.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcity.citizen.data.local.SessionManager
import com.smartcity.citizen.data.local.SortItDatabase
import com.smartcity.citizen.data.repository.CampusBinRepositoryImpl
import com.smartcity.citizen.model.BinStatus
import com.smartcity.citizen.model.CampusBin
import com.smartcity.citizen.ui.components.CategoryBadge
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItPrimaryLight
import com.smartcity.citizen.ui.theme.SortItSecondary
import kotlinx.coroutines.launch

/**
 * Campus Waste Facility Worker Dashboard
 * Fulfills CSE226: RBAC Worker View, Room CRUD status updates, Coroutines & Material 3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDashboardScreen(
    onNavigateToBins: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager.getInstance(context) }
    val currentSession by sessionManager.currentSession.collectAsState()

    val binRepository = remember {
        CampusBinRepositoryImpl(SortItDatabase.getDatabase(context).campusBinDao())
    }
    val bins by binRepository.allBinsFlow.collectAsState(initial = emptyList())

    val nearFullCount = bins.count { it.status == BinStatus.NEAR_FULL }
    val operationalCount = bins.count { it.status == BinStatus.OPERATIONAL }
    val maintenanceCount = bins.count { it.status == BinStatus.MAINTENANCE }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Worker Shift Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SortItPrimaryLight, SortItPrimary, SortItSecondary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "👷 GREEN OPERATIONS SHIFT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "ID: ${currentSession.campusId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Hello, ${currentSession.name.substringBefore(" ")}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Active Campus Route: Academic & Science Quad",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToBins,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = SortItPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Inspect Bin Map", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = onNavigateToProfile,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("My Profile", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Shift Live Stats Cards
        item {
            Text(
                text = "Collection Live Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorkerStatCard(
                    title = "Attention",
                    count = nearFullCount.toString(),
                    subtitle = "Near Full",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                WorkerStatCard(
                    title = "Operational",
                    count = operationalCount.toString(),
                    subtitle = "Emptied & Clear",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                WorkerStatCard(
                    title = "Fault / Fix",
                    count = maintenanceCount.toString(),
                    subtitle = "Maintenance",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Campus Bins to Manage
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Campus Bins Clearance List (${bins.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToGuide) {
                    Text("Streams Guide", color = SortItPrimary)
                }
            }
        }

        if (bins.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SortItPrimary)
                }
            }
        } else {
            items(bins) { bin ->
                WorkerBinActionCard(
                    bin = bin,
                    onMarkEmptied = {
                        scope.launch {
                            binRepository.updateBinStatus(bin.id, BinStatus.OPERATIONAL)
                            Toast.makeText(context, "${bin.name} marked as Emptied & Operational!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onToggleMaintenance = {
                        scope.launch {
                            val nextStatus = if (bin.status == BinStatus.MAINTENANCE) BinStatus.OPERATIONAL else BinStatus.MAINTENANCE
                            binRepository.updateBinStatus(bin.id, nextStatus)
                            Toast.makeText(context, "${bin.name} status updated to $nextStatus", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun WorkerStatCard(
    title: String,
    count: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WorkerBinActionCard(
    bin: CampusBin,
    onMarkEmptied: () -> Unit,
    onToggleMaintenance: () -> Unit
) {
    val statusColor = when (bin.status) {
        BinStatus.OPERATIONAL -> Color(0xFF10B981)
        BinStatus.NEAR_FULL -> Color(0xFFF59E0B)
        BinStatus.MAINTENANCE -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${bin.buildingCode} • ${bin.locationDescription}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = bin.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Accepted streams chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                bin.acceptedCategories.take(3).forEach { cat ->
                    CategoryBadge(category = cat)
                }
                if (bin.acceptedCategories.size > 3) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "+${bin.acceptedCategories.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkEmptied,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SortItPrimaryLight,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mark Emptied", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onToggleMaintenance,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (bin.status == BinStatus.MAINTENANCE) Icons.Default.Check else Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (bin.status == BinStatus.MAINTENANCE) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (bin.status == BinStatus.MAINTENANCE) "Resolved" else "Flag Fault",
                        fontSize = 12.sp,
                        color = if (bin.status == BinStatus.MAINTENANCE) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}
