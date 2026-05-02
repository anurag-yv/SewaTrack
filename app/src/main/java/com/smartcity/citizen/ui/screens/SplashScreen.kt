package com.smartcity.citizen.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@Composable
fun SplashScreen(
    onDestinationReady: (String) -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "iconScale"
    )
    val textAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "textAlpha"
    )
    val textOffsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "textOffsetY"
    )
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 3000),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        delay(3000)
        val currentUser = FirebaseAuth.getInstance().currentUser
        val destination = if (currentUser == null) {
            "login"
        } else {
            try {
                val uid = currentUser.uid
                val doc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                if (!doc.exists()) {
                    FirebaseAuth.getInstance().signOut()
                    "register"
                } else {
                    val role = doc.getString("role") ?: "citizen"
                    when (role) {
                        "admin" -> "admin_dashboard"
                        "employee" -> "employee_dashboard"
                        else -> "citizen_main"
                    }
                }
            } catch (e: Exception) {
                "login"
            }
        }
        onDestinationReady(destination)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSkyline(baseY = size.height * 0.75f)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(100.dp)
                    .scale(iconScale)
            ) {
                drawCenteredCityIcon()
            }
            // Zero spacing between icon and title
            Text(
                text = "SewaTrack",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242),
                modifier = Modifier
                    .graphicsLayer(alpha = textAlpha)
                    .offset(y = (textOffsetY * 16).dp),
                textAlign = TextAlign.Center
            )
            // Zero spacing between title and subtitle
            Text(
                text = "Connect. Report. Improve.",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF616161),
                modifier = Modifier
                    .graphicsLayer(alpha = textAlpha)
                    .offset(y = (textOffsetY * 12).dp),
                textAlign = TextAlign.Center
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter),
            color = Color(0xFF757575),
            trackColor = Color(0xFFBDBDBD)
        )
    }
}

// Perfectly centered skyline
private fun DrawScope.drawSkyline(baseY: Float) {
    val buildingWidths = listOf(40f, 70f, 55f, 90f, 65f, 80f, 50f)
    val totalBuildingWidth = buildingWidths.sum()
    val gapCount = buildingWidths.size + 1
    val gap = (size.width - totalBuildingWidth) / gapCount

    var x = gap
    buildingWidths.forEach { width ->
        val height = 100f + Random.nextFloat() * 100f
        val buildingColor = Color(0xFFBDBDBD).copy(alpha = 0.25f)
        drawRect(
            color = buildingColor,
            topLeft = Offset(x, baseY - height),
            size = Size(width, height)
        )
        if (width > 20f) {
            for (row in 0 until 4) {
                for (col in 0 until (width / 12).toInt()) {
                    if ((col + row) % 2 == 0) {
                        drawRect(
                            color = Color(0xFF757575).copy(alpha = 0.2f),
                            topLeft = Offset(
                                x + col * 14f + 6f,
                                baseY - height + row * 18f + 10f
                            ),
                            size = Size(8f, 12f)
                        )
                    }
                }
            }
        }
        x += width + gap
    }
}

// Draws the city icon perfectly centered inside the 100dp canvas
private fun DrawScope.drawCenteredCityIcon() {
    // Logical size: 200x200 (centered)
    val logicalSize = 200f
    val scale = size.width / logicalSize  // canvas is square (100dp)
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    // Transform from logical coordinates (0..200) to actual canvas coordinates
    fun x(logicalX: Float) = centerX + (logicalX - logicalSize / 2) * scale
    fun y(logicalY: Float) = centerY + (logicalY - logicalSize / 2) * scale

    val buildingColor = Color(0xFF424242)

    // Left building (logical coordinates: x 60..100, y 60..150)
    val leftPath = Path().apply {
        moveTo(x(60f), y(150f))
        lineTo(x(60f), y(60f))
        lineTo(x(100f), y(60f))
        lineTo(x(100f), y(150f))
        close()
    }
    // Right building (logical: x 120..160, y 30..150)
    val rightPath = Path().apply {
        moveTo(x(120f), y(150f))
        lineTo(x(120f), y(30f))
        lineTo(x(160f), y(30f))
        lineTo(x(160f), y(150f))
        close()
    }

    drawPath(leftPath, color = buildingColor)
    drawPath(rightPath, color = buildingColor.copy(alpha = 0.8f))

    // Yellow circle inside left building (logical: center at 80, 90)
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = 14f * scale,
        center = Offset(x(80f), y(90f))
    )
}