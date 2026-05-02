package com.smartcity.citizen.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.smartcity.citizen.ai.AIDescriptionGenerator
import com.smartcity.citizen.ai.ImageClassifier
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.PreferencesDataStore
import com.smartcity.citizen.data.models.Complaint
import com.smartcity.citizen.data.models.IssueType
import com.smartcity.citizen.data.models.Status
import com.smartcity.citizen.navigation.Screen
import com.smartcity.citizen.ui.components.*
import com.smartcity.citizen.ui.theme.*
import com.smartcity.citizen.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val complaintRepo = remember { ComplaintRepository() }
    val prefs = remember { PreferencesDataStore(context) }
    val notificationsEnabled by prefs.notificationsEnabled.collectAsState(initial = true)

    var imageUri by remember { mutableStateOf<Uri?>(null) }       // preview only
    var imageBase64 by remember { mutableStateOf<String?>(null) } // compressed Base64 string
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(IssueType.POTHOLE) }
    var automaticLocation by remember { mutableStateOf("Fetching location...") }
    var manualLocation by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var isImageProcessing by remember { mutableStateOf(false) }
    var showAiGeneration by remember { mutableStateOf(false) }
    var voiceInputText by remember { mutableStateOf("") }

    val aiGenerator = remember { AIDescriptionGenerator() }
    val imageClassifier = remember { ImageClassifier(context) }

    // ---------- Camera ----------
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            isImageProcessing = true
            scope.launch {
                compressAndEncodeAsync(context, tempCameraUri!!) { base64 ->
                    imageBase64 = base64
                    imageUri = tempCameraUri
                    isImageProcessing = false
                }
            }
        } else {
            isImageProcessing = false
            Toast.makeText(context, "Camera cancelled or failed", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File.createTempFile(
                "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_",
                ".jpg",
                context.cacheDir
            ).apply { createNewFile() }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Location ----------
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (isLocationEnabled(context)) {
                fetchLocationSafely(context) { loc -> automaticLocation = loc ?: "Location unavailable" }
            } else {
                automaticLocation = "Location not available (GPS off)"
                Toast.makeText(context, "Please turn on location services", Toast.LENGTH_LONG).show()
            }
        } else {
            automaticLocation = "Location permission denied"
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) Toast.makeText(context, "Notifications disabled", Toast.LENGTH_SHORT).show()
    }

    // ---------- Gallery ----------
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isImageProcessing = true
            scope.launch {
                compressAndEncodeAsync(context, uri) { base64 ->
                    imageBase64 = base64
                    imageUri = uri
                    isImageProcessing = false
                }
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            voiceInputText = matches?.firstOrNull() ?: ""
        }
    }

    // Initial location fetch
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (isLocationEnabled(context)) {
                fetchLocationSafely(context) { loc -> automaticLocation = loc ?: "Location unavailable" }
            } else {
                automaticLocation = "Location not available (GPS off)"
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val photoFile = File.createTempFile(
                "JPEG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_",
                ".jpg",
                context.cacheDir
            ).apply { createNewFile() }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ---------- Map ----------
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

    // ============= UI =============
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Report Civic Issue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
        }

        // Image capture card
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Attach Photo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { openCamera() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Camera")
                    }
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Gallery")
                    }
                }
                if (isImageProcessing) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Processing image...", style = MaterialTheme.typography.bodySmall)
                }
                if (imageUri != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        AsyncImage(model = imageUri, contentDescription = "Selected photo",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        IconButton(onClick = { imageUri = null; imageBase64 = null }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Filled.Close, "Remove", tint = Error)
                        }
                    }
                    // Optimised AI classification – runs on background thread
                    TextButton(onClick = {
                        scope.launch {
                            isProcessing = true
                            val uri = imageUri!!
                            withContext(Dispatchers.IO) {
                                try {
                                    val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                                        }
                                    } else {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val bmp = BitmapFactory.decodeStream(inputStream)
                                        inputStream?.close()
                                        bmp
                                    }
                                    bitmap?.let { imageClassifier.classify(it) }
                                } catch (e: Exception) { null }
                            }?.let { newType ->
                                withContext(Dispatchers.Main) {
                                    selectedType = newType
                                    Toast.makeText(context, "Detected: ${newType.name}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            isProcessing = false
                        }
                    }) {
                        Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Auto-detect Issue (AI)")
                    }
                }
            }
        }

        IssueTypeSpinner(selected = selectedType, onSelected = { selectedType = it })

        OutlinedTextField(
            value = description.ifEmpty { voiceInputText },
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Filled.Description, null) },
            supportingText = { Text("Describe the issue or use voice") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Primary.copy(alpha = 0.4f))
        )

        OutlinedButton(onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            voiceLauncher.launch(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(8.dp)); Text("Use Voice Input")
        }

        // Location card
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Background)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = Primary)
                    Spacer(Modifier.width(8.dp))
                    if (automaticLocation.contains("Lat:") && automaticLocation.contains("Lng:")) {
                        Text(
                            text = automaticLocation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            modifier = Modifier.clickable { openMap(automaticLocation) }
                        )
                    } else {
                        Text(
                            text = if (automaticLocation.startsWith("Location")) automaticLocation else automaticLocation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualLocation,
                    onValueChange = { manualLocation = it },
                    label = { Text("Enter location manually") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            if (isLocationEnabled(context)) {
                                fetchLocationSafely(context) { loc -> automaticLocation = loc ?: "Location unavailable" }
                            } else {
                                Toast.makeText(context, "Please turn on GPS", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }) {
                        Icon(Icons.Filled.MyLocation, null); Text("Retry")
                    }
                    Text("Or type above", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showAiGeneration, onCheckedChange = { showAiGeneration = it })
            Text("Enhance description with AI")
        }

        if (showAiGeneration) {
            GradientButton(
                text = "Generate AI Description",
                onClick = {
                    scope.launch {
                        isProcessing = true
                        try {
                            val generated = withContext(Dispatchers.IO) {
                                aiGenerator.generateDescription(
                                    rawDescription = description.ifEmpty { voiceInputText },
                                    issueType = selectedType,
                                    location = automaticLocation
                                )
                            }
                            description = generated
                        } catch (e: Exception) {
                            Toast.makeText(context, "AI generation failed: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally { isProcessing = false }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    if (isImageProcessing) {
                        Toast.makeText(context, "Please wait, image still processing...", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (imageUri != null && imageBase64.isNullOrEmpty()) {
                        Toast.makeText(context, "Image processing failed. Please retry.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (imageUri == null && description.isBlank()) {
                        Toast.makeText(context, "Please add a photo or description", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    isProcessing = true
                    try {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val finalLocation = if (automaticLocation == "Location unavailable" || automaticLocation.startsWith("Location"))
                            manualLocation.ifBlank { automaticLocation }
                        else automaticLocation

                        val complaint = Complaint(
                            imageUri = imageBase64 ?: "",
                            issueType = selectedType,
                            description = description.ifEmpty { voiceInputText },
                            location = finalLocation,
                            status = Status.SUBMITTED,
                            reportedBy = currentUserId
                        )
                        complaintRepo.addComplaint(complaint)

                        if (notificationsEnabled) {
                            NotificationHelper(context).sendToUser(
                                userId = currentUserId,
                                title = "Complaint Submitted",
                                body = "Your complaint #${complaint.id.take(8)} has been recorded.",
                                complaintId = complaint.id
                            )
                        }
                        Toast.makeText(context, "Complaint submitted!", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.Track.route) { popUpTo(Screen.Home.route) }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Submission failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally { isProcessing = false }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing && !isImageProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color(0xFF424242)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Submit Complaint", fontWeight = FontWeight.Medium)
        }
    }
    if (isProcessing) LoadingOverlay()
}

// ---------- Helper functions (moved to background) ----------
private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun fetchLocationSafely(context: Context, callback: (String?) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            callback("Lat: ${loc.latitude}, Lng: ${loc.longitude}")
        }.addOnFailureListener {
            callback(null)
        }
    } catch (e: SecurityException) {
        callback(null)
    } catch (e: Exception) {
        callback(null)
    }
}

/**
 * Async image compression – runs on IO dispatcher, returns result on main thread via callback.
 */
private suspend fun compressAndEncodeAsync(context: Context, uri: Uri, onResult: (String?) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val bitmap = when {
                Build.VERSION.SDK_INT >= 28 -> {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                    }
                }
                else -> {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bmp
                }
            } ?: return@withContext

            val maxSize = 800
            val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else bitmap

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val bytes = baos.toByteArray()
            baos.close()
            scaledBitmap.recycle()
            if (bitmap != scaledBitmap) bitmap.recycle()

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            withContext(Dispatchers.Main) {
                onResult(base64)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(null)
            }
        }
    }
}