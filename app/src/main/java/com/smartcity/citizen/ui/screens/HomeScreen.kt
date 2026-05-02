package com.smartcity.citizen.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.PreferencesDataStore
import com.smartcity.citizen.data.models.Complaint
import com.smartcity.citizen.data.models.IssueType
import com.smartcity.citizen.data.models.Status
import com.smartcity.citizen.navigation.Screen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { PreferencesDataStore(context) }
    val userName by prefs.userName.collectAsState(initial = "")
    val complaintRepo = remember { ComplaintRepository() }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val userComplaints by complaintRepo.getComplaintsForUser(currentUserId)
        .collectAsState(initial = emptyList())

    val animatedReported by animateIntAsState(
        targetValue = userComplaints.size,
        animationSpec = spring(dampingRatio = 0.6f)
    )
    val animatedInProgress by animateIntAsState(
        targetValue = userComplaints.count { it.status == Status.IN_PROGRESS },
        animationSpec = spring(dampingRatio = 0.6f)
    )
    val animatedResolved by animateIntAsState(
        targetValue = userComplaints.count { it.status == Status.RESOLVED },
        animationSpec = spring(dampingRatio = 0.6f)
    )

    var startAnimation by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(500)
    )
    LaunchedEffect(Unit) { startAnimation = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF9F9F9), Color(0xFFEEEEEE))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = contentAlpha),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Know Your City card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = Color(0xFF3949AB),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Know Your Space",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3949AB)
                            )
                            Text(
                                text = "Explore latest civic updates and projects near you.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }
            }

            // Stats
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard("Reported", animatedReported, Color(0xFFFFA726), Modifier.weight(1f))
                    StatCard("In Progress", animatedInProgress, Color(0xFF42A5F5), Modifier.weight(1f))
                    StatCard("Resolved", animatedResolved, Color(0xFF66BB6A), Modifier.weight(1f))
                }
            }

            // Report button
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            navController.navigate(Screen.Report.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF424242),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Report an Issue", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Issue Categories
            item {
                Text(
                    "Issue Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242)
                )
            }
            item {
                val types = IssueType.entries.filter { it != IssueType.OTHER }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in types.indices step 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryChip(types[i], onClick = { navController.navigate(Screen.Report.route) }, Modifier.weight(1f))
                            if (i + 1 < types.size) {
                                CategoryChip(types[i + 1], onClick = { navController.navigate(Screen.Report.route) }, Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Recent Complaints
            if (userComplaints.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Complaints",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF424242)
                        )
                        TextButton(onClick = {
                            navController.navigate(Screen.Track.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Text("View All", color = Color(0xFF424242))
                        }
                    }
                }
                items(userComplaints.take(5), key = { it.id }) { complaint ->
                    ComplaintCardModern(complaint = complaint, onClick = { navController.navigate("detail/${complaint.id}") })
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No complaints yet", color = Color(0xFF757575))
                    }
                }
            }
        }
    }
}

// ---------- Stat Card ----------
@Composable
fun StatCard(title: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF757575))
        }
    }
}

// ---------- Category Chip ----------
@Composable
fun CategoryChip(type: IssueType, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = when (type) {
        IssueType.POTHOLE -> Color(0xFFFFA726)
        IssueType.GARBAGE -> Color(0xFFEF5350)
        IssueType.STREETLIGHT -> Color(0xFF42A5F5)
        IssueType.WATER_LEAKAGE -> Color(0xFF66BB6A)
        else -> Color(0xFFBDBDBD)
    }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(8.dp).background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF424242),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------- Optimized Complaint Card ----------
@Composable
fun ComplaintCardModern(complaint: Complaint, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageUri = complaint.imageUri

    // Cache the file/content check
    val isFileOrContent = remember(imageUri) {
        imageUri.isNotEmpty() &&
                (imageUri.startsWith("content://") ||
                        imageUri.startsWith("file://") ||
                        File(imageUri).exists())
    }

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

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isFileOrContent -> AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Complaint photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    base64Bitmap != null -> Image(
                        bitmap = base64Bitmap!!.asImageBitmap(),
                        contentDescription = "Complaint photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Icon(
                        Icons.Filled.Image,
                        contentDescription = "No image",
                        modifier = Modifier.size(28.dp),
                        tint = Color(0xFF1565C0)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = complaint.issueType.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    if (complaint.status == Status.RESOLVED && complaint.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index ->
                                Icon(
                                    imageVector = if (index < complaint.rating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA726),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = complaint.description.ifEmpty { "No description" },
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = complaint.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (complaint.status) {
                        Status.SUBMITTED -> Color(0xFFFFA726)
                        Status.IN_PROGRESS -> Color(0xFF42A5F5)
                        Status.RESOLVED -> Color(0xFF66BB6A)
                    }
                )
            }
        }
    }
}