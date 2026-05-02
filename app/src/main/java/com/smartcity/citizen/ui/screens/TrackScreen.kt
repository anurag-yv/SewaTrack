package com.smartcity.citizen.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.models.Complaint
import com.smartcity.citizen.data.models.Status
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TrackScreen(navController: NavHostController) {
    val repo = remember { ComplaintRepository() }
    // Show only current user's complaints
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val userComplaints by repo.getComplaintsForUser(currentUserId).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<Status?>(null) }

    val filtered = userComplaints
        .filter { c ->
            (searchQuery.isBlank() || c.issueType.name.contains(searchQuery, ignoreCase = true) || c.description.contains(searchQuery, ignoreCase = true))
        }
        .filter { c -> filterStatus == null || c.status == filterStatus }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Complaints", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF424242),
                unfocusedBorderColor = Color(0xFFBDBDBD),
                cursorColor = Color(0xFF424242)
            )
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("All") })
            }
            items(Status.entries) { s ->
                FilterChip(
                    selected = filterStatus == s,
                    onClick = { filterStatus = if (filterStatus == s) null else s },
                    label = { Text(s.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No complaints found", color = Color(0xFF757575))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { complaint ->
                    TrackComplaintCard(
                        complaint = complaint,
                        onClick = { navController.navigate("detail/${complaint.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackComplaintCard(complaint: Complaint, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageUri = complaint.imageUri

    val isFileOrContent = imageUri.isNotEmpty() &&
            (imageUri.startsWith("content://") ||
                    imageUri.startsWith("file://") ||
                    File(imageUri).exists())

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
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isFileOrContent -> AsyncImage(
                        model = ImageRequest.Builder(context).data(imageUri).crossfade(true).build(),
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
                    else -> Icon(Icons.Filled.Image, "No image", modifier = Modifier.size(32.dp), tint = Color(0xFF1565C0))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = complaint.issueType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    // Show rating stars if resolved and rated
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