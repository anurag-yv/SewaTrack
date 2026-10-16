package com.smartcity.citizen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcity.citizen.model.BinStatus
import com.smartcity.citizen.model.CampusBin
import com.smartcity.citizen.model.DisposalGuideline
import com.smartcity.citizen.model.WasteCategory
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItSecondary

/**
 * Reusable Category Badge with Distinct Segregation Colors
 */
@Composable
fun CategoryBadge(
    category: WasteCategory,
    modifier: Modifier = Modifier
) {
    val categoryColor = Color(category.hexColor)
    Surface(
        modifier = modifier,
        color = categoryColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(categoryColor, CircleShape)
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = categoryColor
            )
        }
    }
}

/**
 * Animated Confidence Gauge
 * Visualizes model certainty: Green for >75%, Amber for 60-75%, Warning Red for <60%
 */
@Composable
fun ConfidenceGauge(
    confidence: Float,
    isUncertain: Boolean,
    modifier: Modifier = Modifier
) {
    val percentage = (confidence * 100).toInt()
    val animatedProgress by animateFloatAsState(
        targetValue = confidence,
        label = "confidenceGauge"
    )

    val gaugeColor = when {
        isUncertain -> Color(0xFFD32F2F)
        confidence >= 0.75f -> Color(0xFF2E7D32)
        else -> Color(0xFFF57C00)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = if (isUncertain) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = gaugeColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isUncertain) "Low Confidence (Uncertain)" else "Model Confidence",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = gaugeColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = gaugeColor,
            trackColor = gaugeColor.copy(alpha = 0.2f)
        )
    }
}

/**
 * Uncertainty Warning Banner
 * Notifies students when the model is uncertain and prompts manual lookup
 */
@Composable
fun UncertaintyWarningBanner(
    onCheckGuideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // Light Amber container
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Uncertainty Warning",
                tint = Color(0xFFE65100),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Prediction is Uncertain",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "This item has complex materials or low visual clarity. Please verify using the Campus Segregation Guide before disposing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D4037)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onCheckGuideClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFFFE082),
                        contentColor = Color(0xFF4E342E)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Browse Manual Guide", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Recommended Bin Card with Actionable Steps
 */
@Composable
fun RecommendedBinCard(
    guideline: DisposalGuideline,
    modifier: Modifier = Modifier
) {
    val binColor = Color(guideline.binColorHex)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(binColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Bin",
                        tint = binColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "Recommended Disposal Bin",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = guideline.recommendedBin,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = binColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Preparation Steps:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            guideline.preparationSteps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = SortItPrimary
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (guideline.safetyWarning != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = "Safety", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
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

/**
 * Modern Stat Card for Student Sustainability Metrics
 */
@Composable
fun CampusStatCard(
    title: String,
    value: Int,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$value",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
