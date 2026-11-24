package online.layersync.blackdemon.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BD {
    val bg = Color(0xFF050507)
    val surface = Color(0xFF0E0E13)
    val border = Color(0xFF1D1D27)
    val red = Color(0xFFE5173F)
    val redDark = Color(0xFF5A0A1A)
    val muted = Color(0xFF6E6E7C)
}

@Composable
fun BlackDemonTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary = BD.red, background = BD.bg, surface = BD.surface, onSurface = Color.White),
    content = content
)
