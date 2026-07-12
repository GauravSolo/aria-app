package com.aria.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Aria design tokens — Indigo / Violet, light + dark.
 * Mirrors mobile/src/theme/tokens.ts so both platforms look identical.
 */

data class AriaPalette(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryStrong: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val accent: Color,
    val accentSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val track: Color,
    val overlay: Color,
) {
    fun category(c: String): Color = when (c) {
        "work" -> Color(0xFF6366F1)
        "study" -> Color(0xFF8B5CF6)
        "health" -> Color(0xFF10B981)
        "personal" -> Color(0xFFF59E0B)
        else -> Color(0xFF64748B)
    }

    fun priority(p: String): Color = when (p) {
        "high" -> Color(0xFFEF4444)
        "medium" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }
}

val LightPalette = AriaPalette(
    background = Color(0xFFF6F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF0F1F4),
    surfaceElevated = Color(0xFFFFFFFF),
    border = Color(0xFFE6E8EC),
    borderStrong = Color(0xFFD5D8DF),
    text = Color(0xFF16181D),
    textSecondary = Color(0xFF5B616E),
    textMuted = Color(0xFF8A909C),
    primary = Color(0xFF6366F1),
    primaryStrong = Color(0xFF4F46E5),
    primarySoft = Color(0xFFEEF0FF),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF8B5CF6),
    accentSoft = Color(0xFFF2ECFE),
    success = Color(0xFF10B981),
    successSoft = Color(0xFFE6F7F0),
    warning = Color(0xFFF59E0B),
    warningSoft = Color(0xFFFEF3E2),
    danger = Color(0xFFEF4444),
    dangerSoft = Color(0xFFFDECEC),
    info = Color(0xFF3B82F6),
    infoSoft = Color(0xFFE8F1FE),
    track = Color(0xFFE6E8EC),
    overlay = Color(0x73101218),
)

val DarkPalette = AriaPalette(
    background = Color(0xFF0B0C0F),
    surface = Color(0xFF15171C),
    surfaceAlt = Color(0xFF1C1F26),
    surfaceElevated = Color(0xFF1A1D24),
    border = Color(0xFF262A33),
    borderStrong = Color(0xFF333845),
    text = Color(0xFFF2F3F5),
    textSecondary = Color(0xFFA6ACB8),
    textMuted = Color(0xFF6E7480),
    primary = Color(0xFF818CF8),
    primaryStrong = Color(0xFF6366F1),
    primarySoft = Color(0xFF1E1B3A),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFA78BFA),
    accentSoft = Color(0xFF241E3D),
    success = Color(0xFF34D399),
    successSoft = Color(0xFF0F2A22),
    warning = Color(0xFFFBBF24),
    warningSoft = Color(0xFF2C2410),
    danger = Color(0xFFF87171),
    dangerSoft = Color(0xFF311A1A),
    info = Color(0xFF60A5FA),
    infoSoft = Color(0xFF15233A),
    track = Color(0xFF262A33),
    overlay = Color(0x99000000),
)

val LocalAria = staticCompositionLocalOf { LightPalette }

private fun materialScheme(p: AriaPalette, dark: Boolean) =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.primary,
        onPrimary = p.onPrimary,
        secondary = p.accent,
        onSecondary = p.onPrimary,
        background = p.background,
        onBackground = p.text,
        surface = p.surface,
        onSurface = p.text,
        surfaceVariant = p.surfaceAlt,
        onSurfaceVariant = p.textSecondary,
        outline = p.border,
        outlineVariant = p.border,
        error = p.danger,
        primaryContainer = p.primarySoft,
        onPrimaryContainer = p.primary,
        errorContainer = p.dangerSoft,
        onErrorContainer = p.danger,
    )

/** Kept for source compatibility with existing widget/legacy references. */
object Brand {
    val indigo = Color(0xFF6366F1)
    val blue = Color(0xFF3B82F6)
    val green = Color(0xFF10B981)
    val amber = Color(0xFFF59E0B)
    val red = Color(0xFFEF4444)

    fun category(c: String): Color = LightPalette.category(c)
    fun priority(p: String): Color = LightPalette.priority(p)
}

@Composable
fun AriaTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = when (mode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalAria provides palette) {
        MaterialTheme(colorScheme = materialScheme(palette, dark), content = content)
    }
}
