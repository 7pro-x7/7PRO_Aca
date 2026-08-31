package com.sevenpro.management.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 7PRO Brand Colors - Professional Blue/Slate
val SevenProPrimary = Color(0xFF1E3A5F)      // Deep navy
val SevenProPrimaryLight = Color(0xFF2D5F8A)  // Medium blue
val SevenProSecondary = Color(0xFF0EA5E9)     // Sky blue accent
val SevenProTertiary = Color(0xFF10B981)       // Emerald (success/profit)
val SevenProError = Color(0xFFEF4444)          // Red (overdue/error)
val SevenProWarning = Color(0xFFF59E0B)        // Amber (pending)
val SevenProSurface = Color(0xFFF8FAFC)        // Near-white surface

val SevenProPrimaryDark = Color(0xFF93C5FD)    // Light blue for dark mode
val SevenProSecondaryDark = Color(0xFF38BDF8)   // Bright sky for dark mode
val SevenProSurfaceDark = Color(0xFF0F172A)     // Dark slate
val SevenProBackgroundDark = Color(0xFF020617)  // Almost black

private val LightColorScheme = lightColorScheme(
    primary = SevenProPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E4F7),
    onPrimaryContainer = SevenProPrimary,
    secondary = SevenProSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6F0FF),
    onSecondaryContainer = SevenProPrimary,
    tertiary = SevenProTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    error = SevenProError,
    onError = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = SevenProPrimaryDark,
    onPrimary = SevenProPrimary,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = SevenProPrimaryDark,
    secondary = SevenProSecondaryDark,
    onSecondary = SevenProPrimary,
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = SevenProSecondaryDark,
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF064E3B),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    background = SevenProBackgroundDark,
    onBackground = Color(0xFFE2E8F0),
    surface = SevenProSurfaceDark,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569)
)

@Composable
fun SevenProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SevenProTypography,
        content = content
    )
}
