package com.smartcity.citizen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.smartcity.citizen.ui.screens.SortItMainScreen
import com.smartcity.citizen.ui.screens.SplashScreen
import com.smartcity.citizen.ui.theme.SortItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SortItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isSplashActive by remember { mutableStateOf(true) }

                    Crossfade(
                        targetState = isSplashActive,
                        animationSpec = tween(durationMillis = 500),
                        label = "SplashToMainCrossfade"
                    ) { showSplash ->
                        if (showSplash) {
                            SplashScreen(
                                onSplashComplete = { isSplashActive = false }
                            )
                        } else {
                            SortItMainScreen()
                        }
                    }
                }
            }
        }
    }
}