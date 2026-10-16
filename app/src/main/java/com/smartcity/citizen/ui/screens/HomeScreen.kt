package com.smartcity.citizen.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartcity.citizen.data.local.SortItDatabase
import com.smartcity.citizen.data.local.entity.WasteRecordEntity
import com.smartcity.citizen.data.repository.WasteRepository
import com.smartcity.citizen.data.repository.WasteRepositoryImpl
import com.smartcity.citizen.model.WasteCategory
import com.smartcity.citizen.ui.components.CampusStatCard
import com.smartcity.citizen.ui.components.CategoryBadge
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItPrimaryLight
import com.smartcity.citizen.ui.theme.SortItSecondary

/**
 * SortIt Home Dashboard Screen
 * Fulfills CSE226 Unit I: Material 3, Compose Animations, Custom Components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit,
    onNavigateToGuide: (WasteCategory?) -> Unit,
    onNavigateToLocator: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val repository: WasteRepository = remember {
        WasteRepositoryImpl(SortItDatabase.getDatabase(context).wasteRecordDao())
    }

    val totalRecordsCount by repository.recordCountFlow.collectAsState(initial = 0)
    val recentRecords by repository.recentRecordsFlow.collectAsState(initial = emptyList())
    val todayTip = remember { repository.getTodayTip() }

    val animatedCount by animateIntAsState(
        targetValue = totalRecordsCount,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "countAnim"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Sustainability Banner & AI Scan CTA
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Eco,
                                contentDescription = null,
                                tint = Color(0xFFA5D6A7),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "SortIt • Lovely Professional University (LPU)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFA5D6A7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Sort Smarter,\nWaste Less.",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 32.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "AI-powered waste identification & segregation guide for LPU campus quads, Uni-Mall & hostels.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE8F5E9)
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = onNavigateToScan,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = SortItPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Identify Waste (AI Camera)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Campus Sustainability Impact Stats
        item {
            Text(
                text = "Your Segregation Impact",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampusStatCard(
                    title = "Items Sorted",
                    value = animatedCount,
                    subtitle = "diverted from landfill",
                    color = SortItPrimary,
                    modifier = Modifier.weight(1f)
                )
                CampusStatCard(
                    title = "Campus Bins",
                    value = 6,
                    subtitle = "monitored stations",
                    color = SortItSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Category Exploration Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waste Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigateToGuide(null) }) {
                    Text("View All Guide", color = SortItPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(WasteCategory.entries) { category ->
                    val color = Color(category.hexColor)
                    Surface(
                        modifier = Modifier.clickable { onNavigateToGuide(category) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Daily Campus Eco Tip & Challenge
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SortItPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = SortItPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Daily Eco Tip & Student Challenge",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SortItPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = todayTip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todayTip.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(18.dp))
                            Text(
                                text = "Challenge: ${todayTip.challenge}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Quick Navigation Shortcuts (Bins & History)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedCard(
                    onClick = onNavigateToLocator,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Map, contentDescription = null, tint = SortItSecondary)
                        Column {
                            Text("Campus Bins", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Find nearest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedCard(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = SortItPrimary)
                        Column {
                            Text("History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Review items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Recent Scans
        if (recentRecords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Classifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Text("See All", color = SortItPrimary)
                    }
                }
            }
            items(recentRecords, key = { it.id }) { record ->
                RecentScanCard(record = record)
            }
        }
    }
}

@Composable
fun RecentScanCard(record: WasteRecordEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(record.confirmedCategory.hexColor).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (record.imageUri.isNotEmpty()) {
                    AsyncImage(
                        model = record.imageUri,
                        contentDescription = "Waste photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Recycling,
                        contentDescription = null,
                        tint = Color(record.confirmedCategory.hexColor)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.detectedLabel.ifBlank { record.confirmedCategory.displayName },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${record.confirmedCategory.binColorName} • ${(record.confidence * 100).toInt()}% match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CategoryBadge(category = record.confirmedCategory)
        }
    }
}