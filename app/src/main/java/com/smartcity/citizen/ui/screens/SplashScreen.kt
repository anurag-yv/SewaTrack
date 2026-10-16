package com.smartcity.citizen.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcity.citizen.ui.theme.SortItBackground
import com.smartcity.citizen.ui.theme.SortItPrimary
import com.smartcity.citizen.ui.theme.SortItPrimaryLight
import com.smartcity.citizen.ui.theme.SortItSecondary
import kotlinx.coroutines.delay

/**
 * SortIt First-Launch Animated Splash Screen
 * Fulfills User Requirement: "on opening app first screen should be name SortIt with logo then after other pages"
 * Fulfills CSE226 Unit I: Compose Animation APIs (animateFloatAsState, infiniteTransition)
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "ContentAlpha"
    )

    // Gentle infinite breathing ring
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200) // 2.2s splash duration
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SortItBackground,
                        Color(0xFFE8F8F0),
                        Color(0xFFD1F2E0)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Glowing Outer Ring
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(SortItPrimaryLight.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Main Logo Badge
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .scale(logoScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SortItPrimaryLight, SortItPrimary, SortItSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = "SortIt Logo",
                        tint = Color.White,
                        modifier = Modifier.size(62.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name Branding
            Text(
                text = "SortIt",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = SortItPrimary,
                letterSpacing = 1.sp,
                modifier = Modifier.scale(if (startAnimation) 1f else 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Smart Campus Waste Segregation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // University Institutional Identity
            Surface(
                color = SortItPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Eco,
                        contentDescription = null,
                        tint = SortItPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Lovely Professional University (LPU)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SortItPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Progress Indicator
            CircularProgressIndicator(
                color = SortItPrimary,
                trackColor = SortItPrimary.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Initializing AI & Campus Sensor Network...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Bottom Footer
        Text(
            text = "CSE226 • Android Application Deployment",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}