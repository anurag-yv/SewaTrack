@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartcity.citizen.data.local.SortItDatabase
import com.smartcity.citizen.data.repository.WasteRepositoryImpl
import com.smartcity.citizen.model.DisposalGuideline
import com.smartcity.citizen.model.WasteCategory
import com.smartcity.citizen.ui.components.CategoryBadge
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.viewmodel.WasteGuideViewModel

/**
 * Searchable Campus Waste Segregation Guide Screen
 * Fulfills CSE226 Unit III: SavedStateHandle & Unit II: Flow combine operators
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WasteGuideScreen(

    initialCategory: WasteCategory? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember {
        WasteRepositoryImpl(SortItDatabase.getDatabase(context).wasteRecordDao())
    }
    val viewModel: WasteGuideViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val savedStateHandle = androidx.lifecycle.SavedStateHandle()
                if (initialCategory != null) {
                    savedStateHandle["key_guide_category"] = initialCategory.name
                }
                return WasteGuideViewModel(savedStateHandle, repository) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text(
                text = "Campus Segregation Guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Search Bar with clear icon
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search items (e.g. coffee cup, batteries, pizza box)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SortItPrimary) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SortItPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        // Filter Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text("All Streams") },
                    shape = RoundedCornerShape(8.dp)
                )
            }
            items(WasteCategory.entries) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = {
                        viewModel.onCategorySelected(
                            if (uiState.selectedCategory == category) null else category
                        )
                    },
                    label = { Text(category.displayName) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Content List of Guidelines
        if (uiState.guidelines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("No matching items found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Try searching for broad terms like 'plastic', 'apple', or 'paper'.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.guidelines, key = { it.category.name }) { guideline ->
                    GuideDetailCard(guideline = guideline)
                }
            }
        }
    }
}

@Composable
fun GuideDetailCard(guideline: DisposalGuideline) {
    val binColor = Color(guideline.binColorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(12.dp).background(binColor, CircleShape))
                    Text(
                        text = guideline.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                CategoryBadge(category = guideline.category)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Target: ${guideline.recommendedBin}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = binColor
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Common Campus Items:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                guideline.commonCampusExamples.forEach { example ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preparation Instructions:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            guideline.preparationSteps.forEach { step ->
                Row(
                    modifier = Modifier.padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("•", color = SortItPrimary, fontWeight = FontWeight.Bold)
                    Text(step, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (guideline.safetyWarning != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                        Text(
                            text = guideline.safetyWarning,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB71C1C)
                        )
                    }
                }
            }
        }
    }
}
