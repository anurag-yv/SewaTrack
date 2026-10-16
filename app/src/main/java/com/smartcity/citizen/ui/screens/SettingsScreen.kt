package com.smartcity.citizen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartcity.citizen.data.PreferencesDataStore
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.util.AlarmScheduler
import kotlinx.coroutines.launch

/**
 * User Settings & Accessibility Screen
 * Fulfills CSE226 Unit I (Theme customization & M3 components) & Unit II (Coroutines with DataStore)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesDataStore(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }

    val isDark by prefs.isDarkMode.collectAsState(initial = false)
    val notificationsEnabled by prefs.notificationsEnabled.collectAsState(initial = true)
    val userName by prefs.userName.collectAsState(initial = "Campus Student")

    var showCampusDialog by remember { mutableStateOf(false) }
    var selectedCampus by remember { mutableStateOf("University Main Campus") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Preferences Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SortItPrimary
                )

                // Dark Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                            contentDescription = null,
                            tint = SortItPrimary
                        )
                        Column {
                            Text("Dark Theme", fontWeight = FontWeight.SemiBold)
                            Text("High contrast dark UI mode", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { checked ->
                            scope.launch { prefs.setDarkMode(checked) }
                        }
                    )
                }

                HorizontalDivider()

                // Daily Segregation Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = SortItPrimary)
                        Column {
                            Text("Daily Eco Reminders", fontWeight = FontWeight.SemiBold)
                            Text("Lunchtime waste sorting prompt", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            scope.launch {
                                prefs.setNotificationsEnabled(checked)
                                if (checked) {
                                    // Schedule a daily prompt (AlarmManager)
                                    alarmScheduler.scheduleReminder("student_user", "Time to sort! Did you segregate today's canteen waste?", 1000 * 60 * 60 * 4)
                                    Toast.makeText(context, "Daily Eco Reminder scheduled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                HorizontalDivider()

                // Campus Location Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCampusDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = SortItPrimary)
                        Column {
                            Text("University Campus", fontWeight = FontWeight.SemiBold)
                            Text(selectedCampus, style = MaterialTheme.typography.bodySmall, color = SortItPrimary)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }

        // Academic & Course Info Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Course Details & Academic Attribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SortItPrimary
                )
                Text(
                    text = "SortIt — Smart Campus Waste Segregation App",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Course: CSE226 — Android Application Development", style = MaterialTheme.typography.bodySmall)
                Text("Architecture: 100% Kotlin, Jetpack Compose, Room Database, TFLite AI, MVVM", style = MaterialTheme.typography.bodySmall)
                Text("Version: 1.0.0 (Release Build Candidate)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        // Legal & Google Play Store Compliance
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Legal & Play Store Policies",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SortItPrimary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToPrivacy),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = SortItPrimary)
                        Text("Privacy Policy (On-Device Data Safety)", fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToTerms),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = SortItPrimary)
                        Text("Terms of Service", fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }

    // Campus selection dialog
    if (showCampusDialog) {
        val campuses = listOf("University Main Campus", "North Science Campus", "Engineering & Tech Hub")
        AlertDialog(
            onDismissRequest = { showCampusDialog = false },
            title = { Text("Select Campus") },
            text = {
                Column {
                    campuses.forEach { campus ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCampus = campus
                                    showCampusDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCampus == campus,
                                onClick = {
                                    selectedCampus = campus
                                    showCampusDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(campus, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCampusDialog = false }) { Text("Close") }
            }
        )
    }
}
