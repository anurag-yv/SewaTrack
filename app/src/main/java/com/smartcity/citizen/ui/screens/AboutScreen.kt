package com.smartcity.citizen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Grey colour palette (consistent with other screens)
private val GreyBg = Color(0xFFF5F5F5)
private val GreyBorder = Color(0xFFE0E0E0)
private val GreyTextDark = Color(0xFF424242)
private val GreyTextMid = Color(0xFF757575)
private val GreyTextLight = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GreyTextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = GreyTextDark
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // App icon – now grey instead of blue
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "App Icon",
                modifier = Modifier.size(80.dp),
                tint = GreyTextMid
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SmartCity Citizen",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GreyTextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version 1.0.0",
                fontSize = 14.sp,
                color = GreyTextMid
            )
            Spacer(modifier = Modifier.height(24.dp))

            // About card
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = GreyBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About This App",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GreyTextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SmartCity Citizen is a platform that empowers citizens to report civic issues such as potholes, garbage accumulation, streetlight outages, and water leakages. Using AI-powered detection and real-time tracking, the app ensures that complaints reach the appropriate municipal authorities quickly and efficiently.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreyTextDark,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features card
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = GreyBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Key Features",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GreyTextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Report issues with photo and location\n• AI-based issue type detection\n• Voice input for descriptions\n• Track complaint status in real time\n• Rate resolved complaints\n• Role-based dashboards (Citizen, Employee, Admin)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreyTextDark,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contact card
            Card(
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = GreyBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contact Us",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GreyTextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Email: support@smartcity.com\nWebsite: www.smartcity.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GreyTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}