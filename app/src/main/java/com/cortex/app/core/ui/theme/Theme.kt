package com.cortex.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Cortex runs dark-only in v1. The content is dense and editorial; a light
 * theme dilutes the aesthetic. Revisit in v2 if users ask.
 */
private val CortexDarkColorScheme = darkColorScheme(
    primary = CortexColors.Accent,
    onPrimary = CortexColors.Paper,
    primaryContainer = CortexColors.AccentDim,
    onPrimaryContainer = CortexColors.Paper,

    secondary = CortexColors.Ink,
    onSecondary = CortexColors.Paper,

    background = CortexColors.Paper,
    onBackground = CortexColors.Ink,

    surface = CortexColors.PaperRaised,
    onSurface = CortexColors.Ink,
    surfaceVariant = CortexColors.PaperSunken,
    onSurfaceVariant = CortexColors.Muted,

    outline = CortexColors.Rule,
    outlineVariant = CortexColors.Rule,

    error = CortexColors.Danger,
    onError = CortexColors.Ink,
)

@Composable
fun CortexTheme(
    // kept as a param for future flexibility; currently ignored
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = CortexDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CortexTypography,
        content = content,
    )
}
