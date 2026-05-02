package com.smartcity.citizen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.smartcity.citizen.data.PreferencesDataStore

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryDark,
    background = Background,
    surface = Surface,
    error = Error,
    onBackground = OnBackground
)

private val DarkColors = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = OnSecondary,
    primaryContainer = Primary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    error = Error,
    onBackground = DarkOnBackground
)

@Composable
fun SmartCityCitizenTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesDataStore(context) }
    val isDark by prefs.isDarkMode.collectAsState(initial = false)

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}