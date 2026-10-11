package com.usipipo.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta de azules pastel (ver docs/plans/2026-10-09-usipipo-vpn-android-design.md, sección 5).
private val Navy = Color(0xFF0B2545)

private val LightColors = lightColorScheme(
    primary = Color(0xFFA8CCFF),
    onPrimary = Navy,
    primaryContainer = Color(0xFFD6E6FB),
    onPrimaryContainer = Navy,
    secondary = Color(0xFF4A6283),
    onSecondary = Color.White,
    background = Color(0xFFF3F8FF),
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFE8F1FD),
    onSurfaceVariant = Color(0xFF4A6283),
    outline = Color(0xFFD6E6FB),
    error = Color(0xFFF4A3A3),
    onError = Navy,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DBBFF),
    onPrimary = Navy,
    primaryContainer = Color(0xFF24406B),
    onPrimaryContainer = Color(0xFFE6F0FF),
    secondary = Color(0xFF9DB8DD),
    onSecondary = Navy,
    background = Color(0xFF0E1A2B),
    onBackground = Color(0xFFE6F0FF),
    surface = Color(0xFF162843),
    onSurface = Color(0xFFE6F0FF),
    surfaceVariant = Color(0xFF1E3556),
    onSurfaceVariant = Color(0xFFB7C9E2),
    outline = Color(0xFF2B4468),
    error = Color(0xFFF4A3A3),
    onError = Navy,
)

/** Colores del botón según el estado del túnel. Siempre van acompañados de texto. */
@Immutable
data class StatusColors(val idle: Color, val connected: Color, val error: Color)

private val LightStatus = StatusColors(
    idle = Color(0xFFA8CCFF),
    connected = Color(0xFF6FA8FF),
    error = Color(0xFFF4A3A3),
)

private val DarkStatus = StatusColors(
    idle = Color(0xFF8DBBFF),
    connected = Color(0xFF6FA8FF),
    error = Color(0xFFF4A3A3),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

@Composable
fun UsipipoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography(),
            content = content,
        )
    }
}
