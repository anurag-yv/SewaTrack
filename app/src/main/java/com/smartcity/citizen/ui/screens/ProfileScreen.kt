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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcity.citizen.data.local.SessionManager
import com.smartcity.citizen.model.UserRole
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItPrimaryLight
import com.smartcity.citizen.ui.theme.SortItSecondary

import androidx.navigation.NavHostController

/**
 * User Profile & RBAC Role Switcher Screen
 * Fulfills CSE226: User Profile, Role Management & Navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController? = null,
    onLogout: () -> Unit = {},
    onNavigateToLogin: () -> Unit = onLogout,
    onNavigateToPrivacy: () -> Unit = { navController?.navigate("privacy_policy") },
    onNavigateToTerms: () -> Unit = { navController?.navigate("terms_of_service") },
    onBack: () -> Unit = { navController?.popBackStack() }
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val currentSession by sessionManager.currentSession.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Card Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(SortItPrimaryLight, SortItPrimary, SortItSecondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentSession.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentSession.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentSession.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = SortItPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentSession.role.badgeTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SortItPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ProfileMiniStat(title = "Campus ID", value = currentSession.campusId)
                    ProfileMiniStat(
                        title = if (currentSession.role == UserRole.WORKER) "Bins Serviced" else "Eco Score",
                        value = if (currentSession.role == UserRole.WORKER) "${currentSession.binsClearedCount} Bins" else "${currentSession.ecoScore} pts"
                    )
                }
            }
        }

        // Live Role Switcher (Crucial for Evaluation Demonstration)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SortItPrimary)
                    Text(
                        text = "Switch User Role (RBAC Demo)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Tap any role to immediately see its tailored dashboard and permissions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserRole.entries.forEach { role ->
                        val isCurrent = currentSession.role == role
                        Surface(
                            onClick = {
                                sessionManager.switchRole(role)
                                Toast.makeText(context, "Switched role to ${role.displayName}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) SortItPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = if (isCurrent) 2.dp else 0.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (role) {
                                        UserRole.STUDENT -> "🎓 Student"
                                        UserRole.WORKER -> "👷 Worker"
                                        UserRole.ADMIN -> "🛡️ Admin"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Department Information
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Institutional Affiliation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Department: ${currentSession.department}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Role Scope: ${currentSession.role.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Account & Legal Options
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                ProfileOptionRow(
                    icon = Icons.Outlined.Security,
                    title = "Privacy Policy",
                    onClick = onNavigateToPrivacy
                )
                HorizontalDivider()
                ProfileOptionRow(
                    icon = Icons.Outlined.Description,
                    title = "Terms of Service",
                    onClick = onNavigateToTerms
                )
                HorizontalDivider()
                ProfileOptionRow(
                    icon = Icons.Outlined.Login,
                    title = "Switch Account / Login",
                    onClick = onNavigateToLogin
                )
            }
        }

        // Logout Button
        Button(
            onClick = {
                sessionManager.logout()
                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                onNavigateToLogin()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileMiniStat(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = SortItPrimary, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}