package com.smartcity.citizen.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.smartcity.citizen.data.AuthRepository
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.models.Status
import com.smartcity.citizen.ui.components.CustomRatingBar
import com.smartcity.citizen.ui.theme.*
import com.smartcity.citizen.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    complaintId: String,
    navController: androidx.navigation.NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ComplaintRepository() }
    val authRepo = remember { AuthRepository() }
    val firestore = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val allComplaints by repo.getAllComplaints().collectAsState(initial = emptyList())
    val complaint = allComplaints.find { it.id == complaintId }

    var rating by remember { mutableIntStateOf(0) }
    var currentRole by remember { mutableStateOf("citizen") }
    LaunchedEffect(Unit) { currentRole = authRepo.getUserRole() ?: "citizen" }

    var showAdminMessageDialog by remember { mutableStateOf(false) }
    var adminMessageText by remember { mutableStateOf("") }
    var selectedRecipient by remember { mutableStateOf("") }

    var assignedEmployeeName by remember { mutableStateOf("") }
    var isLoadingEmployee by remember { mutableStateOf(false) }
    var showRequestUpdateDialog by remember { mutableStateOf(false) }
    var requestUpdateMessage by remember { mutableStateOf("") }

    // Date formatter cache
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    // Optimized image loading
    val imageUri = complaint?.imageUri ?: ""
    val properUri = remember(imageUri) {
        when {
            imageUri.startsWith("content://") -> imageUri
            imageUri.startsWith("file://") -> imageUri
            imageUri.isNotEmpty() && File(imageUri).exists() -> "file://$imageUri"
            else -> imageUri
        }
    }
    val isFileOrContent = properUri.startsWith("content://") || properUri.startsWith("file://")
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

    LaunchedEffect(complaint) {
        if (complaint != null && complaint.assignedTo != null) {
            isLoadingEmployee = true
            try {
                val doc = firestore.collection("users").document(complaint.assignedTo!!).get().await()
                assignedEmployeeName = doc.getString("name") ?: "Employee"
            } catch (_: Exception) { }
            isLoadingEmployee = false
        }
    }

    if (complaint == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Complaint not found")
        }
        return
    }

    // Set initial rating from complaint
    LaunchedEffect(complaint.rating) {
        rating = complaint.rating
    }

    fun openMap(locationString: String) {
        if (locationString.isBlank()) return
        val numbers = Pattern.compile("[-+]?\\d*\\.?\\d+").toRegex().findAll(locationString).toList()
        if (numbers.size < 2) {
            Toast.makeText(context, "Invalid location format", Toast.LENGTH_SHORT).show()
            return
        }
        val lat = numbers[0].value
        val lng = numbers[1].value

        try {
            val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            val mapsIntent = Intent(Intent.ACTION_VIEW, geoUri).setPackage("com.google.android.apps.maps")
            if (mapsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapsIntent)
                return
            }
        } catch (_: Exception) {}

        try {
            val webUri = Uri.parse("https://www.google.com/maps?q=$lat,$lng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            val chooser = Intent.createChooser(webIntent, "Open map")
            if (chooser.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
                return
            }
        } catch (_: Exception) {}
        Toast.makeText(context, "Coordinates: $lat, $lng", Toast.LENGTH_LONG).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            when {
                isFileOrContent -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(properUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Issue photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                base64Bitmap != null -> {
                    Image(
                        bitmap = base64Bitmap!!.asImageBitmap(),
                        contentDescription = "Issue photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE3F2FD))
                            .border(2.dp, Color(0xFF42A5F5), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.ViewInAr, contentDescription = "No image", modifier = Modifier.size(72.dp), tint = Color(0xFF1565C0))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No image provided", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                            Text("The reporter did not attach a photo.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1565C0).copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        // Issue type and status
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = complaint.issueType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
            )
            Surface(shape = RoundedCornerShape(50), color = when (complaint.status) {
                Status.SUBMITTED -> Secondary.copy(alpha = 0.15f)
                Status.IN_PROGRESS -> PrimaryLight.copy(alpha = 0.15f)
                Status.RESOLVED -> Color(0xFFE8F5E9)
            }) {
                Text(
                    text = complaint.status.name.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = when (complaint.status) {
                        Status.SUBMITTED -> SecondaryDark
                        Status.IN_PROGRESS -> PrimaryDark
                        Status.RESOLVED -> Color(0xFF2E7D32)
                    }
                )
            }
        }

        // Description card
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Description, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Description", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryDark)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(complaint.description.ifEmpty { "No description provided" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Location card
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(complaint.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Date card
        val dateString = dateFormat.format(Date(complaint.dateTime))
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reported on $dateString", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Ratings section (fixed: removed 'enabled' parameter)
        if (complaint.status == Status.RESOLVED && complaint.rating == 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rate the resolution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomRatingBar(
                        rating = rating,
                        onRatingChange = { newRating ->
                            rating = newRating
                            scope.launch {
                                repo.updateRating(complaint.id, newRating)
                                Toast.makeText(context, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        } else if (complaint.rating > 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your rating", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Only onRatingChange is used to indicate read‑only; no 'enabled' parameter
                    CustomRatingBar(
                        rating = complaint.rating,
                        onRatingChange = {}
                    )
                }
            }
        }

        // Citizen enrichments
        if (currentRole == "citizen" || currentRole == "employee") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            if (complaint.assignedTo != null) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Assigned to", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(if (isLoadingEmployee) "Loading..." else assignedEmployeeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            if (complaint.estimatedCost > 0) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachMoney, null, tint = Primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Budget: ₹${complaint.estimatedCost}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (complaint.resolutionNote != null) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Resolution Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(complaint.resolutionNote, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = { showRequestUpdateDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Request Update")
            }
            OutlinedButton(onClick = {
                val shareText = """
                    Complaint #${complaint.id.take(8)}
                    Type: ${complaint.issueType.name.replace("_", " ")}
                    Status: ${complaint.status.name}
                    Description: ${complaint.description}
                    Location: ${complaint.location}
                    Reported on: $dateString
                """.trimIndent()
                val shareIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }
                context.startActivity(Intent.createChooser(shareIntent, "Share complaint"))
            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Complaint")
            }
            if (complaint.location.isNotBlank() && complaint.location.contains("Lat:")) {
                OutlinedButton(onClick = { openMap(complaint.location) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View on Map")
                }
            }
        }

        // Admin actions
        if (currentRole == "admin") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            OutlinedButton(onClick = { showAdminMessageDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Message, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Send Message")
            }
        }
    }

    // Dialogs (unchanged)
    if (showAdminMessageDialog && complaint != null) {
        val employeeId = complaint.assignedTo
        val citizenId = complaint.reportedBy
        AlertDialog(
            onDismissRequest = { showAdminMessageDialog = false },
            title = { Text("Send Message", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = adminMessageText, onValueChange = { adminMessageText = it }, label = { Text("Your message") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    if (employeeId != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedRecipient == "employee", onClick = { selectedRecipient = "employee" })
                            Text("Assigned Employee")
                        }
                    }
                    if (citizenId.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedRecipient == "citizen", onClick = { selectedRecipient = "citizen" })
                            Text("Reporting Citizen")
                        }
                    }
                    if (employeeId == null && citizenId.isBlank()) Text("No recipients available", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (adminMessageText.isNotBlank() && selectedRecipient.isNotEmpty()) {
                        scope.launch {
                            val targetId = when (selectedRecipient) { "employee" -> employeeId!!; "citizen" -> citizenId; else -> return@launch }
                            NotificationHelper(context).sendToUser(userId = targetId, title = "Message from Admin", body = adminMessageText, complaintId = complaint.id)
                            Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show()
                            showAdminMessageDialog = false; adminMessageText = ""; selectedRecipient = ""
                        }
                    } else Toast.makeText(context, "Please write a message and select a recipient", Toast.LENGTH_SHORT).show()
                }, enabled = adminMessageText.isNotBlank() && selectedRecipient.isNotEmpty()) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showAdminMessageDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRequestUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showRequestUpdateDialog = false },
            title = { Text("Request Update", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = requestUpdateMessage, onValueChange = { requestUpdateMessage = it }, label = { Text("Additional message (optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val admins = firestore.collection("users").whereEqualTo("role", "admin").get().await()
                        admins.documents.forEach { adminDoc ->
                            NotificationHelper(context).sendToUser(adminDoc.id, "Update Request", "Citizen requests update for complaint #${complaint.id.take(8)}: ${requestUpdateMessage.ifBlank { "Please check status." }}", complaint.id)
                        }
                        Toast.makeText(context, "Update requested", Toast.LENGTH_SHORT).show()
                        showRequestUpdateDialog = false; requestUpdateMessage = ""
                    }
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showRequestUpdateDialog = false }) { Text("Cancel") } }
        )
    }
}