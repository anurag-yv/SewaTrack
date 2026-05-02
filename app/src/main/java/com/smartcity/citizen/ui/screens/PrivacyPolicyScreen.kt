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
fun PrivacyPolicyScreen(onBack: () -> Unit) {
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
                "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center   // centered heading
            )
            Text(
                """
                1. Information We Collect
                We may collect personal information such as your name, email address, and location when you register or submit a complaint. We also collect non‑personal data such as device type and app usage statistics.

                2. How We Use Your Information
                The information you provide is used solely to process civic complaints, improve our services, and communicate with you regarding the status of your reports.

                3. Data Sharing
                We do not sell, trade, or otherwise transfer your personal data to third parties. Your data is shared only with the municipal authorities and service providers necessary to resolve reported issues.

                4. Data Security
                We implement industry‑standard security measures to protect your personal information. However, no method of transmission over the internet is 100% secure.

                5. Your Rights
                You may request access to, correction of, or deletion of your personal data at any time by contacting our support team.

                6. Changes to This Policy
                We may update this privacy policy from time to time. You will be notified of any changes by updating the effective date at the top of this page.

                If you have any questions about this policy, please contact us at privacy@smartcity.com.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp
            )
        }
    }
}