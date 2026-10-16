package com.smartcity.citizen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Recycling
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartcity.citizen.data.local.SortItDatabase
import com.smartcity.citizen.data.local.entity.WasteRecordEntity
import com.smartcity.citizen.data.repository.WasteRepositoryImpl
import com.smartcity.citizen.model.WasteCategory
import com.smartcity.citizen.ui.components.CategoryBadge
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItSecondary
import com.smartcity.citizen.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Waste Sorting History & Personal Sustainability Analytics
 * Fulfills CSE226 Unit IV: Room with Flow & Offline-First Development
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    customViewModel: HistoryViewModel? = null
) {
    val context = LocalContext.current.applicationContext as android.app.Application
    val viewModel: HistoryViewModel = customViewModel ?: viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = SortItDatabase.getDatabase(context)
                val repo = WasteRepositoryImpl(db.wasteRecordDao())
                return HistoryViewModel(context, repo) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var recordToDelete by remember { mutableStateOf<WasteRecordEntity?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sorting History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.totalCount} items logged locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = SortItPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${uiState.diversionRatePercent}% Diversion",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SortItPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedFilterCategory == null,
                    onClick = { viewModel.setFilterCategory(null) },
                    label = { Text("All (${uiState.totalCount})") }
                )
            }
            items(WasteCategory.entries) { cat ->
                val count = uiState.categoryCounts[cat] ?: 0
                FilterChip(
                    selected = uiState.selectedFilterCategory == cat,
                    onClick = {
                        viewModel.setFilterCategory(
                            if (uiState.selectedFilterCategory == cat) null else cat
                        )
                    },
                    label = { Text("${cat.displayName} ($count)") }
                )
            }
        }

        // History Records List
        if (uiState.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "No sorting records yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Items identified using the AI Camera will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        dateText = dateFormatter.format(Date(record.timestamp)),
                        onDeleteClick = { recordToDelete = record }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog (CSE226 Unit I: Alert Dialogs)
    if (recordToDelete != null) {
        val record = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Log Entry?") },
            text = {
                Text("Remove '${record.detectedLabel.ifBlank { record.confirmedCategory.displayName }}' from your local history?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    record: WasteRecordEntity,
    dateText: String,
    onDeleteClick: () -> Unit
) {
    val categoryColor = Color(record.confirmedCategory.hexColor)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (record.imageUri.isNotEmpty()) {
                    AsyncImage(
                        model = record.imageUri,
                        contentDescription = "Logged item",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Outlined.Recycling,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(28.dp)
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
                        text = record.detectedLabel.ifBlank { record.confirmedCategory.displayName },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CategoryBadge(category = record.confirmedCategory)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.confirmedCategory.binColorName} • ${(record.confidence * 100).toInt()}% match",
                    style = MaterialTheme.typography.bodySmall,
                    color = categoryColor,
                    fontWeight = FontWeight.Medium
                )
                if (record.notes.isNotEmpty()) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
