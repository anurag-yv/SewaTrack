@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.smartcity.citizen.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.smartcity.citizen.model.BinStatus
import com.smartcity.citizen.model.CampusBin
import com.smartcity.citizen.model.WasteCategory
import com.smartcity.citizen.ui.components.CategoryBadge
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItSecondary
import com.smartcity.citizen.ui.viewmodel.BinLocatorUiState
import com.smartcity.citizen.ui.viewmodel.BinWithDistance
import com.smartcity.citizen.ui.viewmodel.CampusBinLocatorViewModel

/**
 * Campus Waste Bin Locator Screen with Google Maps & ModalBottomSheet
 * Fulfills CSE226 Unit V: Location Services & Google Maps Compose, Unit I: Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CampusBinLocatorScreen(
    customViewModel: CampusBinLocatorViewModel? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: CampusBinLocatorViewModel = customViewModel ?: viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val db = com.smartcity.citizen.data.local.SortItDatabase.getDatabase(app)
                val repo = com.smartcity.citizen.data.repository.CampusBinRepositoryImpl(db.campusBinDao())
                return CampusBinLocatorViewModel(app, repo) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // Lovely Professional University (LPU, Phagwara, Punjab) Campus Center (Block 34 CSE)
    val campusCenter = LatLng(31.2536, 75.7037)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(campusCenter, 16.5f)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchUserLocation()
        } else {
            Toast.makeText(context, "Location permission allows sorting bins by proximity to you.", Toast.LENGTH_SHORT).show()
        }
    }

    // Lifecycle effect: fetch GPS on initial load
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.fetchUserLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Modal Bottom Sheet for detailed bin information (CSE226 Unit I: Bottom Sheet)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showBottomSheet && uiState.selectedBin != null) {
        val bin = uiState.selectedBin!!
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                viewModel.selectBin(null)
            },
            sheetState = sheetState
        ) {
            BinDetailSheetContent(
                bin = bin,
                onOpenDirections = {
                    val geoUri = Uri.parse("geo:${bin.latitude},${bin.longitude}?q=${bin.latitude},${bin.longitude}(${Uri.encode(bin.name)})")
                    val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=${bin.latitude},${bin.longitude}")
                        )
                        context.startActivity(browserIntent)
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header & View Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Campus Waste Bins",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.bins.size} collection stations registered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.fetchUserLocation() }
                ) {
                    if (uiState.isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.MyLocation, contentDescription = "My Location", tint = SortItPrimary)
                    }
                }

                FilledTonalIconButton(
                    onClick = { viewModel.toggleViewMode() }
                ) {
                    Icon(
                        imageVector = if (uiState.isMapMode) Icons.Outlined.List else Icons.Outlined.Map,
                        contentDescription = "Toggle View"
                    )
                }
            }
        }

        // Stream Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All Streams") }
                )
            }
            items(WasteCategory.entries) { category ->
                FilterChip(
                    selected = uiState.selectedCategoryFilter == category,
                    onClick = {
                        viewModel.setCategoryFilter(
                            if (uiState.selectedCategoryFilter == category) null else category
                        )
                    },
                    label = { Text(category.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main View: Map or List
        if (uiState.isMapMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = false
                    )
                ) {
                    uiState.bins.forEach { binWithDist ->
                        val bin = binWithDist.bin
                        val hue = when (bin.acceptedCategories.firstOrNull()) {
                            WasteCategory.ORGANIC_WET -> BitmapDescriptorFactory.HUE_GREEN
                            WasteCategory.RECYCLABLE_DRY, WasteCategory.PAPER_CARDBOARD -> BitmapDescriptorFactory.HUE_BLUE
                            WasteCategory.PLASTIC_METAL -> BitmapDescriptorFactory.HUE_ORANGE
                            WasteCategory.E_WASTE -> BitmapDescriptorFactory.HUE_RED
                            WasteCategory.GLASS -> BitmapDescriptorFactory.HUE_CYAN
                            else -> BitmapDescriptorFactory.HUE_ROSE
                        }

                        Marker(
                            state = MarkerState(position = LatLng(bin.latitude, bin.longitude)),
                            title = bin.name,
                            snippet = "${binWithDist.formattedDistance} • Tap for details",
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                viewModel.selectBin(bin)
                                showBottomSheet = true
                                true
                            }
                        )
                    }
                }

                // Map Legend Overlay
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem("Green", "Organic", Color(0xFF2E7D32))
                        LegendItem("Blue", "Recycle", Color(0xFF1565C0))
                        LegendItem("Red", "E-Waste", Color(0xFFC62828))
                    }
                }
            }
        } else {
            // Proximity List View sorted by GPS Distance
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.bins, key = { it.bin.id }) { binWithDist ->
                    CampusBinCard(
                        binWithDist = binWithDist,
                        onClick = {
                            viewModel.selectBin(binWithDist.bin)
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(colorName: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CampusBinCard(
    binWithDist: BinWithDistance,
    onClick: () -> Unit
) {
    val bin = binWithDist.bin
    val primaryColor = Color(bin.acceptedCategories.firstOrNull()?.hexColor ?: 0xFF1565C0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = bin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = binWithDist.formattedDistance,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SortItPrimary
                    )
                }
                Text(
                    text = "${bin.buildingCode} • ${bin.locationDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    bin.acceptedCategories.take(3).forEach { cat ->
                        CategoryBadge(category = cat)
                    }
                }
            }
        }
    }
}

@Composable
fun BinDetailSheetContent(
    bin: CampusBin,
    onOpenDirections: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = bin.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${bin.buildingCode} • Lat: ${String.format("%.4f", bin.latitude)}, Lng: ${String.format("%.4f", bin.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = when (bin.status) {
                    BinStatus.OPERATIONAL -> Color(0xFFE8F5E9)
                    BinStatus.NEAR_FULL -> Color(0xFFFFF3E0)
                    BinStatus.MAINTENANCE -> Color(0xFFFFEBEE)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = bin.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (bin.status) {
                        BinStatus.OPERATIONAL -> Color(0xFF2E7D32)
                        BinStatus.NEAR_FULL -> Color(0xFFE65100)
                        BinStatus.MAINTENANCE -> Color(0xFFC62828)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "Location Description:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = bin.locationDescription,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Accepted Segregation Streams:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bin.acceptedCategories.forEach { cat ->
                CategoryBadge(category = cat)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onOpenDirections,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SortItPrimary)
        ) {
            Icon(Icons.Outlined.Directions, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Open Directions in Maps", fontWeight = FontWeight.Bold)
        }
    }
}
