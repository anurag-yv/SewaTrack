package com.smartcity.citizen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.smartcity.citizen.data.PreferencesDataStore

private val SortItLightColors = lightColorScheme(
    primary = SortItPrimary,
    onPrimary = Color.White,
    primaryContainer = SortItSurfaceVariant,
    onPrimaryContainer = SortItPrimaryDark,
    secondary = SortItSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = SortItSecondaryDark,
    tertiary = SortItAccent,
    background = SortItBackground,
    onBackground = Color(0xFF1B241D),
    surface = SortItSurface,
    onSurface = Color(0xFF1B241D),
    surfaceVariant = SortItSurfaceVariant,
    onSurfaceVariant = Color(0xFF384B3D),
    error = SortItError,
    onError = Color.White
)

private val SortItDarkColors = darkColorScheme(
    primary = SortItDarkPrimary,
    onPrimary = Color(0xFF00390E),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFD54F),
    background = SortItDarkBackground,
    onBackground = SortItDarkOnBackground,
    surface = SortItDarkSurface,
    onSurface = SortItDarkOnBackground,
    surfaceVariant = SortItDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA5B8A8),
    error = Color(0xFFEF5350),
    onError = Color.Black
)

@Composable
fun SortItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesDataStore(context) }
    val isAppDarkMode by prefs.isDarkMode.collectAsState(initial = darkTheme)

    MaterialTheme(
        colorScheme = if (isAppDarkMode) SortItDarkColors else SortItLightColors,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}