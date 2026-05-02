package com.smartcity.citizen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets(0.dp))
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Terms of Service",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center   // centered heading
            )
            Text(
                """
                1. Acceptance of Terms
                By using the SmartCity Citizen app, you agree to be bound by these Terms of Service. If you do not agree, please discontinue use of the app.

                2. User Responsibilities
                You are responsible for providing accurate information when submitting complaints. Deliberately false reports may lead to account suspension.

                3. Service Availability
                We strive to keep the app available at all times, but we are not liable for any temporary unavailability due to technical issues or maintenance.

                4. Intellectual Property
                All content, design, and code are the property of SmartCity Citizen. You may not reproduce or distribute any part of the app without written permission.

                5. Limitation of Liability
                We are not responsible for any indirect, incidental, or consequential damages arising from the use of our app.

                6. Termination
                We reserve the right to terminate or suspend your account at any time if you violate these terms.

                7. Changes to Terms
                We may modify these terms at any time. Continued use of the app after changes constitutes acceptance of the new terms.

                If you have questions about these terms, contact us at terms@smartcity.com.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
            )
        }
    }
}