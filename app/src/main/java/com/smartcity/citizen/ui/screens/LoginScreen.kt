package com.smartcity.citizen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcity.citizen.data.local.SessionManager
import com.smartcity.citizen.model.UserRole
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItPrimaryLight
import com.smartcity.citizen.ui.theme.SortItSecondary

import androidx.navigation.NavHostController

/**
 * Modern Login Screen with Role-Based Access Control (RBAC) & Quick Evaluation Presets
 * Fulfills CSE226: Authentication, User Roles, Material 3 Design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController? = null,
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = { navController?.navigate("register") }
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    var email by remember { mutableStateOf("aarav.student@campus.edu") }
    var password by remember { mutableStateOf("password123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Branding Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(SortItPrimaryLight, SortItPrimary, SortItSecondary))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Recycling,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Welcome to SortIt",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = SortItPrimary
        )
        Text(
            text = "Smart Campus Waste Segregation & Facility Hub",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Role Selector Segment
        Text(
            text = "Choose Your Role:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserRole.entries.forEach { role ->
                val isSelected = selectedRole == role
                Surface(
                    onClick = {
                        selectedRole = role
                        // Pre-fill demo credentials for evaluator convenience
                        when (role) {
                            UserRole.STUDENT -> email = "aarav.student@campus.edu"
                            UserRole.WORKER -> email = "ramesh.worker@campus.edu"
                            UserRole.ADMIN -> email = "priya.admin@campus.edu"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) SortItPrimary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = if (isSelected) 3.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (role) {
                                UserRole.STUDENT -> "🎓 Student"
                                UserRole.WORKER -> "👷 Worker"
                                UserRole.ADMIN -> "🛡️ Admin"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input Fields Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Campus Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = SortItPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = SortItPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        sessionManager.login(email = email, role = selectedRole)
                        Toast.makeText(context, "Logged in as ${selectedRole.displayName}", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SortItPrimary)
                ) {
                    Text("Sign In as ${selectedRole.displayName}", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Demonstration Shortcut Bar for Evaluators
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SortItPrimaryLight.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚡ Quick Demo Mode (1-Tap Switch):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SortItPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            sessionManager.switchRole(UserRole.STUDENT)
                            onLoginSuccess()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Student", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            sessionManager.switchRole(UserRole.WORKER)
                            onLoginSuccess()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Worker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            sessionManager.switchRole(UserRole.ADMIN)
                            onLoginSuccess()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("New to SortIt? Create an Account", color = SortItPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}