package com.edy.rbaclab.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F1E9),
    onPrimaryContainer = Color(0xFF0A3D34),
    secondary = Color(0xFFB9502E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFF6656A5),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1B1C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1C20),
    surfaceVariant = Color(0xFFE9ECEF),
    outline = Color(0xFF74777A),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED5C4),
    primaryContainer = Color(0xFF085044),
    secondary = Color(0xFFFFB59E),
    tertiary = Color(0xFFCCC1FF),
    background = Color(0xFF111416),
    surface = Color(0xFF191C1E),
)

@Composable
fun RbacLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
