package com.smartcity.citizen.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.smartcity.citizen.data.local.SessionManager
import com.smartcity.citizen.model.UserRole
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItPrimaryLight
import com.smartcity.citizen.ui.theme.SortItSecondary

import androidx.navigation.NavHostController

/**
 * Modern Registration Screen with Role Assignment
 * Fulfills CSE226: User Onboarding, RBAC, Data Validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavHostController? = null,
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = { navController?.popBackStack() }
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var campusId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(SortItPrimaryLight, SortItPrimary, SortItSecondary))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Create SortIt Account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = SortItPrimary
        )
        Text(
            text = "Join your university green waste initiative",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Role Choice Cards
        Text(
            text = "Select Account Role:",
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
                    onClick = { selectedRole = role },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) SortItPrimary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

        Spacer(modifier = Modifier.height(18.dp))

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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = SortItPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = campusId,
                    onValueChange = { campusId = it },
                    label = { Text(if (selectedRole == UserRole.STUDENT) "Student Roll No / ID" else "Staff / Admin ID") },
                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = SortItPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Campus Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = SortItPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department / Academic Block") },
                    leadingIcon = { Icon(Icons.Default.Business, null, tint = SortItPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
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
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please complete required fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        sessionManager.register(
                            name = name,
                            email = email,
                            role = selectedRole,
                            campusId = campusId.ifBlank { "CU-2026" },
                            dept = department.ifBlank { "University Campus" }
                        )
                        Toast.makeText(context, "Account registered as ${selectedRole.displayName}!", Toast.LENGTH_SHORT).show()
                        onRegisterSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SortItPrimary)
                ) {
                    Text("Register Account", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already registered? Back to Login", color = SortItPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}