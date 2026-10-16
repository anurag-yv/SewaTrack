package com.smartcity.citizen.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*

import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartcity.citizen.model.WasteCategory
import com.smartcity.citizen.ui.components.CategoryBadge
import com.smartcity.citizen.ui.components.ConfidenceGauge
import com.smartcity.citizen.ui.components.RecommendedBinCard
import com.smartcity.citizen.ui.components.UncertaintyWarningBanner
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItSecondary
import com.smartcity.citizen.ui.viewmodel.ClassificationUiState
import com.smartcity.citizen.ui.viewmodel.WasteClassifierViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genuine AI Waste Identification Screen
 * Fulfills Mandatory AI Requirement & CSE226 Units I, II, III
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteClassifierScreen(
    customViewModel: WasteClassifierViewModel? = null,
    onNavigateToGuide: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: WasteClassifierViewModel = customViewModel ?: viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val db = com.smartcity.citizen.data.local.SortItDatabase.getDatabase(app)
                val repo = com.smartcity.citizen.data.repository.WasteRepositoryImpl(db.wasteRecordDao())
                val classifier = com.smartcity.citizen.ai.WasteClassifier(app)
                return WasteClassifierViewModel(app, repo, classifier) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showCategoryCorrectionDialog by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }

    // Listen to ViewModel events (Toasts & notifications)
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Camera capture setup
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && viewModel.currentCapturedUri != null) {
            viewModel.onImageSelected(viewModel.currentCapturedUri!!)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempImageUri(context)
            viewModel.currentCapturedUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is needed to scan waste items.", Toast.LENGTH_LONG).show()
        }
    }

    // Gallery Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = createTempImageUri(context)
            viewModel.currentCapturedUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Animated scan line effect during classification
    val infiniteTransition = rememberInfiniteTransition(label = "scanLaser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPosition"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "AI Waste Classifier",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Action Buttons: Camera & Gallery
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Capture or Select Waste Item",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Take a clear picture of the item to determine its correct segregation bin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { launchCamera() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SortItPrimary)
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Camera")
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gallery")
                    }
                }
            }
        }

        // State Machine Handling (Idle, Classifying, ImageSelected, Success, Error)
        when (val state = uiState) {
            is ClassificationUiState.Idle -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FilterCenterFocus,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = SortItPrimary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No photo selected yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Snap a photo using the camera above",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            is ClassificationUiState.Classifying -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = SortItPrimary)
                            Text(
                                text = "Analyzing item with on-device AI...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Running tensor preprocessing & feature extraction",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is ClassificationUiState.ImageSelected -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = state.previewBitmap.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Animated laser scan effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (240 * laserY).dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color(0xFF00E676), Color.Transparent)
                                )
                            )
                    )
                }
            }

            is ClassificationUiState.Success -> {
                // Image Preview with Result Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = state.imageUri,
                        contentDescription = "Classified Item",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Detected: ${state.result.detectedLabel}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Confidence Gauge
                ConfidenceGauge(
                    confidence = state.result.confidence,
                    isUncertain = state.result.isUncertain
                )

                // Uncertainty Warning Banner if confidence is low
                if (state.result.isUncertain) {
                    UncertaintyWarningBanner(
                        onCheckGuideClick = onNavigateToGuide
                    )
                }

                // Classification Result Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Classified Category",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = state.confirmedCategory.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(state.confirmedCategory.hexColor)
                                )
                            }
                            CategoryBadge(category = state.confirmedCategory)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.confirmedCategory.iconDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showCategoryCorrectionDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Incorrect? Change Category", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Recommended Bin and Preparation Guidelines
                RecommendedBinCard(guideline = state.result.guideline)

                // Optional Student Notes & Save Action
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Item notes (e.g. Empty juice bottle from library cafe)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Button(
                    onClick = { viewModel.saveClassificationRecord(notesText) },
                    enabled = !state.isSaved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isSaved) Color(0xFF388E3C) else SortItPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (state.isSaved) Icons.Outlined.Check else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isSaved) "Saved to Sorting History" else "Confirm & Save Disposal",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Another Item")
                }
            }

            is ClassificationUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Classification Failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.reset() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Manual Category Correction Dialog (Fulfills CSE226 Unit I: Dialogs)
    if (showCategoryCorrectionDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryCorrectionDialog = false },
            title = { Text("Select Correct Waste Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "If the AI misidentified the material, select the true category to keep your campus history accurate:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    WasteCategory.entries.forEach { cat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateConfirmedCategory(cat)
                                    showCategoryCorrectionDialog = false
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(Color(cat.hexColor), CircleShape))
                                Column {
                                    Text(cat.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                    Text(cat.binColorName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryCorrectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun createTempImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.cacheDir
    val file = File.createTempFile("SORTIT_${timeStamp}_", ".jpg", storageDir).apply {
        createNewFile()
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
