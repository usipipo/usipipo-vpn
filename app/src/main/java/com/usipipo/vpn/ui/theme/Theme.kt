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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.usipipo.vpn.R

// Paleta de azules pastel (ver docs/plans/2026-10-09-usipipo-vpn-android-design.md, sección 5).
private val Navy = Color(0xFF0B2545)

private val LightColors = lightColorScheme(
    primary = Color(0xFFA8CCFF),
    onPrimary = Navy,
    primaryContainer = Color(0xFFD6E6FB),
    onPrimaryContainer = Navy,
    secondaryContainer = Color(0xFFD6E6FB),
    onSecondaryContainer = Navy,
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
    primary = Color(AtmospherePalette.DARK_PRIMARY),
    onPrimary = Color(AtmospherePalette.DARK_ON_PRIMARY),
    primaryContainer = Color(0xFF14386B),
    onPrimaryContainer = Color(0xFFE6F0FF),
    secondaryContainer = Color(0xFF14386B),
    onSecondaryContainer = Color(0xFFE6F0FF),
    secondary = Color(AtmospherePalette.DARK_SECONDARY),
    onSecondary = Color(AtmospherePalette.DARK_ON_SECONDARY),
    background = Color(AtmospherePalette.DARK_BACKGROUND),
    onBackground = Color(0xFFE6F0FF),
    surface = Color(AtmospherePalette.DARK_SURFACE),
    onSurface = Color(0xFFE6F0FF),
    surfaceVariant = Color(AtmospherePalette.DARK_SURFACE_VARIANT),
    onSurfaceVariant = Color(0xFFB7C9E2),
    outline = Color(0xFF244A7C),
    error = Color(0xFFF4A3A3),
    onError = Navy,
)

/** Colores del botón según el estado del túnel. Siempre van acompañados de texto. */
@Immutable
data class StatusColors(val idle: Color, val connected: Color, val error: Color)

private val LightStatus = StatusColors(
    idle = Color(AtmospherePalette.BUTTON_LIGHT_IDLE),
    connected = Color(AtmospherePalette.BUTTON_LIGHT_CONNECTED),
    error = Color(AtmospherePalette.BUTTON_LIGHT_ERROR),
)

private val DarkStatus = StatusColors(
    idle = Color(AtmospherePalette.BUTTON_DARK_IDLE),
    connected = Color(AtmospherePalette.BUTTON_DARK_CONNECTED),
    error = Color(AtmospherePalette.BUTTON_DARK_ERROR),
)

/** Colores del badge de ping por latencia (RF-3: verde <100 ms, ambar 100-250 ms, rojo >250 ms). */
@Immutable
data class PingColors(val good: Color, val warn: Color, val bad: Color, val unknown: Color)

private val LightPing = PingColors(
    good = Color(0xFF1B7F4B),
    warn = Color(0xFF8A5A00),
    bad = Color(0xFFB3261E),
    unknown = Color(0xFF4A6283),
)

private val DarkPing = PingColors(
    good = Color(0xFF5BD98A),
    warn = Color(0xFFF5C451),
    bad = Color(0xFFF87171),
    unknown = Color(0xFFB7C9E2),
)

val LocalPingColors = staticCompositionLocalOf { LightPing }

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

// Manrope variable: los cinco pesos usan el mismo archivo con distinto eje `wght`.
private val Manrope = FontFamily(
    Font(R.font.manrope, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.manrope, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val UsipipoTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
        headlineSmall = headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = bodyMedium.copy(fontFamily = Manrope),
        labelLarge = labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    )
}

/** Degradado de fondo (arriba → abajo) de una fase del ambiente. */
@Immutable
data class Atmosphere(val top: Color, val bottom: Color)

@Immutable
data class AtmosphereColors(
    val calm: Atmosphere,
    val drifting: Atmosphere,
    val flowing: Atmosphere,
    val alert: Atmosphere,
) {
    fun forPhase(phase: AtmospherePhase): Atmosphere = when (phase) {
        AtmospherePhase.Calm -> calm
        AtmospherePhase.Drifting -> drifting
        AtmospherePhase.Flowing -> flowing
        AtmospherePhase.Alert -> alert
    }
}

private val LightAtmosphere = AtmosphereColors(
    calm = Atmosphere(Color(AtmospherePalette.LIGHT_CALM_TOP), Color(AtmospherePalette.LIGHT_CALM_BOTTOM)),
    drifting = Atmosphere(Color(AtmospherePalette.LIGHT_DRIFTING_TOP), Color(AtmospherePalette.LIGHT_DRIFTING_BOTTOM)),
    flowing = Atmosphere(Color(AtmospherePalette.LIGHT_FLOWING_TOP), Color(AtmospherePalette.LIGHT_FLOWING_BOTTOM)),
    alert = Atmosphere(Color(AtmospherePalette.LIGHT_ALERT_TOP), Color(AtmospherePalette.LIGHT_ALERT_BOTTOM)),
)

private val DarkAtmosphere = AtmosphereColors(
    calm = Atmosphere(Color(AtmospherePalette.DARK_CALM_TOP), Color(AtmospherePalette.DARK_CALM_BOTTOM)),
    drifting = Atmosphere(Color(AtmospherePalette.DARK_DRIFTING_TOP), Color(AtmospherePalette.DARK_DRIFTING_BOTTOM)),
    flowing = Atmosphere(Color(AtmospherePalette.DARK_FLOWING_TOP), Color(AtmospherePalette.DARK_FLOWING_BOTTOM)),
    alert = Atmosphere(Color(AtmospherePalette.DARK_ALERT_TOP), Color(AtmospherePalette.DARK_ALERT_BOTTOM)),
)

val LocalAtmosphere = staticCompositionLocalOf { LightAtmosphere }

/**
 * Colores de la barra inferior: fondo, color de la pestaña activa y de las inactivas.
 * Vive aparte del esquema de Material porque la barra lleva su propio azul de marca.
 */
@Immutable
data class BottomBarColors(val container: Color, val active: Color, val inactive: Color)

private val LightBottomBar = BottomBarColors(
    container = Color(AtmospherePalette.BOTTOM_BAR_LIGHT),
    active = Color(AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT),
    inactive = Color(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT),
)

private val DarkBottomBar = BottomBarColors(
    container = Color(AtmospherePalette.BOTTOM_BAR_DARK),
    active = Color(AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK),
    inactive = Color(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK),
)

val LocalBottomBarColors = staticCompositionLocalOf { LightBottomBar }

@Composable
fun UsipipoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus,
        LocalAtmosphere provides if (darkTheme) DarkAtmosphere else LightAtmosphere,
        LocalPingColors provides if (darkTheme) DarkPing else LightPing,
        LocalBottomBarColors provides if (darkTheme) DarkBottomBar else LightBottomBar,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = UsipipoTypography,
            content = content,
        )
    }
}
