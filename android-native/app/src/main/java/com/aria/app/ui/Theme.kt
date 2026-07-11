package com.aria.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF16181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFF0F1F4),
    error = Color(0xFFEF4444),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFA78BFA),
    background = Color(0xFF0B0C0F),
    onBackground = Color(0xFFF2F3F5),
    surface = Color(0xFF15171C),
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF1C1F26),
    error = Color(0xFFF87171),
)

object Brand {
    val indigo = Color(0xFF6366F1)
    val blue = Color(0xFF3B82F6)
    val green = Color(0xFF10B981)
    val amber = Color(0xFFF59E0B)
    val red = Color(0xFFEF4444)

    fun category(c: String): Color = when (c) {
        "work" -> Color(0xFF6366F1)
        "study" -> Color(0xFF8B5CF6)
        "health" -> Color(0xFF10B981)
        "personal" -> Color(0xFFF59E0B)
        else -> Color(0xFF64748B)
    }

    fun priority(p: String): Color = when (p) {
        "high" -> red
        "medium" -> amber
        else -> green
    }
}

@Composable
fun AriaTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
