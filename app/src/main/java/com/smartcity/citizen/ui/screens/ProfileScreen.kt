package com.smartcity.citizen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.citizen.data.AuthRepository
import com.smartcity.citizen.data.ComplaintRepository
import com.smartcity.citizen.data.PreferencesDataStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val GreyBg = Color(0xFFF5F5F5)
private val GreyBorder = Color(0xFFE0E0E0)
private val GreyTextDark = Color(0xFF424242)
private val GreyTextMid = Color(0xFF757575)
private val GreyTextLight = Color(0xFF9E9E9E)
private val AccentBlue = Color(0xFF42A5F5)
private val AccentOrange = Color(0xFFFF9800)

@Composable
fun ProfileScreen(
    navController: NavHostController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesDataStore(context) }
    val complaintRepo = remember { ComplaintRepository() }
    val authRepo = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val userComplaints by complaintRepo.getComplaintsForUser(currentUserId).collectAsState(initial = emptyList())
    val complaintCount = userComplaints.size

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "Not logged in"
    var userRole by remember { mutableStateOf("citizen") }
    var firestoreName by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            userRole = authRepo.getUserRole() ?: "citizen"
            firestoreName = authRepo.getUserName() ?: ""
        }
    }

    val savedName by prefs.userName.collectAsState(initial = "Citizen")
    val displayName = if (firestoreName.isNotBlank()) firestoreName else savedName

    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(displayName) }

    val darkMode by prefs.isDarkMode.collectAsState(initial = false)
    val notificationsEnabled by prefs.notificationsEnabled.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GreyTextDark)

        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    color = AccentBlue.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(displayName.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, color = AccentBlue)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditingName) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                singleLine = true,
                                label = { Text("Display Name") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreyBorder,
                                    unfocusedBorderColor = GreyBorder,
                                    focusedLabelColor = GreyTextMid,
                                    cursorColor = GreyTextDark,
                                    focusedTextColor = GreyTextDark,
                                    unfocusedTextColor = GreyTextDark
                                )
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    prefs.setUserName(editedName)
                                    currentUser?.uid?.let { uid ->
                                        FirebaseFirestore.getInstance()
                                            .collection("users").document(uid)
                                            .update("name", editedName)
                                    }
                                    firestoreName = editedName
                                    isEditingName = false
                                    Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                                }
                            }) { Icon(Icons.Default.Check, "Save", tint = AccentBlue) }
                            IconButton(onClick = {
                                editedName = displayName
                                isEditingName = false
                            }) { Icon(Icons.Default.Close, "Cancel", tint = GreyTextLight) }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GreyTextDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                editedName = displayName
                                isEditingName = true
                            }) { Icon(Icons.Default.Edit, "Edit name", tint = AccentBlue, modifier = Modifier.size(20.dp)) }
                        }
                    }
                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = GreyTextMid)
                    Text("Role: ${userRole.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelMedium, color = GreyTextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$complaintCount Complaints Reported", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            }
        }

        HorizontalDivider(color = GreyBorder)

        Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = GreyTextDark)

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DarkMode, null, tint = GreyTextMid)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Dark Mode", modifier = Modifier.weight(1f), color = GreyTextDark)
                Switch(
                    checked = darkMode,
                    onCheckedChange = { scope.launch { prefs.setDarkMode(it) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GreyTextDark,
                        checkedTrackColor = GreyBorder,
                        uncheckedThumbColor = GreyTextMid,
                        uncheckedTrackColor = GreyBg
                    )
                )
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, null, tint = GreyTextMid)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Notifications", modifier = Modifier.weight(1f), color = GreyTextDark)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { scope.launch { prefs.setNotificationsEnabled(it) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GreyTextDark,
                        checkedTrackColor = GreyBorder,
                        uncheckedThumbColor = GreyTextMid,
                        uncheckedTrackColor = GreyBg
                    )
                )
            }
        }

        HorizontalDivider(color = GreyBorder)

        Text("Resources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = GreyTextDark)

        ListItem(
            headlineContent = { Text("App Version", color = GreyTextDark) },
            supportingContent = { Text("1.0.0", color = GreyTextMid) },
            leadingContent = { Icon(Icons.Default.Info, null, tint = GreyTextMid) },
            colors = ListItemDefaults.colors(containerColor = Color.White)
        )

        ListItem(
            headlineContent = { Text("Privacy Policy", color = GreyTextDark) },
            leadingContent = { Icon(Icons.Default.PrivacyTip, null, tint = GreyTextMid) },
            modifier = Modifier.clickable { navController.navigate("privacy_policy") },
            colors = ListItemDefaults.colors(containerColor = Color.White)
        )

        ListItem(
            headlineContent = { Text("Terms of Service", color = GreyTextDark) },
            leadingContent = { Icon(Icons.Default.Description, null, tint = GreyTextMid) },
            modifier = Modifier.clickable { navController.navigate("terms_of_service") },
            colors = ListItemDefaults.colors(containerColor = Color.White)
        )

        HorizontalDivider(color = GreyBorder)

        OutlinedButton(
            onClick = {
                scope.launch {
                    withContext(NonCancellable) {
                        authRepo.logout()
                    }
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreyTextDark),
            border = BorderStroke(1.dp, GreyBorder)
        ) {
            Icon(Icons.Filled.Logout, null, tint = GreyTextMid)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", color = GreyTextDark)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}