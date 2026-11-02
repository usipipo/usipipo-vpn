package com.usipipo.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.usipipo.vpn.R
import com.usipipo.vpn.settings.PaletteTokens
import com.usipipo.vpn.settings.PaletteVariant

// Paleta de azules pastel (ver docs/plans/2026-10-09-usipipo-vpn-android-design.md, sección 5).
private val Navy = Color(0xFF0B2545)

// Los esquemas base fijan los colores "on" de la app. La paleta activa sobrescribe
// los tokens que define; el resto se hereda de aquí, para que no queden huecos.
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

/** Tokens por defecto: Azul noche en modo claro. */
private val DefaultTokens: PaletteTokens = PaletteVariant.AzulNoche.tokens(isDark = false)

/** Colores del botón según el estado del túnel. Siempre van acompañados de texto. */
@Immutable
data class StatusColors(val idle: Color, val connected: Color, val error: Color)

/** Colores del badge de ping por latencia (RF-3: verde <100 ms, ambar 100-250 ms, rojo >250 ms). */
@Immutable
data class PingColors(val good: Color, val warn: Color, val bad: Color, val unknown: Color)

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

/**
 * Colores de la barra inferior: fondo, color de la pestaña activa (icono y etiqueta) y de las
 * inactivas. Vive aparte del esquema de Material porque la barra lleva su propio azul de marca.
 */
@Immutable
data class BottomBarColors(
    val container: Color,
    val active: Color,
    val activeLabel: Color,
    val inactive: Color,
)

val LocalPingColors = staticCompositionLocalOf { pingColors(DefaultTokens) }

val LocalStatusColors = staticCompositionLocalOf { statusColors(DefaultTokens) }

val LocalAtmosphere = staticCompositionLocalOf { atmosphereColors(DefaultTokens) }

val LocalBottomBarColors = staticCompositionLocalOf { bottomBarColors(DefaultTokens) }

/** Si la app se pinta en modo oscuro: sigue al tema elegido, no al del sistema. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

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

/** Colores del botón según el estado del túnel, con los tokens de la paleta. */
private fun statusColors(tokens: PaletteTokens): StatusColors = StatusColors(
    idle = Color(tokens.statusIdle),
    connected = Color(tokens.statusConnected),
    error = Color(tokens.statusError),
)

/** Colores del badge de ping, con los tokens de la paleta. */
private fun pingColors(tokens: PaletteTokens): PingColors = PingColors(
    good = Color(tokens.pingGood),
    warn = Color(tokens.pingWarn),
    bad = Color(tokens.pingBad),
    unknown = Color(tokens.pingUnknown),
)

/** Colores de la barra inferior, con los tokens de la paleta. */
private fun bottomBarColors(tokens: PaletteTokens): BottomBarColors = BottomBarColors(
    container = Color(tokens.bottomBarContainer),
    active = Color(tokens.bottomBarActive),
    activeLabel = Color(tokens.bottomBarActiveLabel),
    inactive = Color(tokens.bottomBarInactive),
)

/** Colores de la atmósfera, con los tokens de la paleta. */
private fun atmosphereColors(tokens: PaletteTokens): AtmosphereColors = AtmosphereColors(
    calm = Atmosphere(Color(tokens.calmTop), Color(tokens.calmBottom)),
    drifting = Atmosphere(Color(tokens.driftingTop), Color(tokens.driftingBottom)),
    flowing = Atmosphere(Color(tokens.flowingTop), Color(tokens.flowingBottom)),
    alert = Atmosphere(Color(tokens.alertTop), Color(tokens.alertBottom)),
)

/**
 * Esquema de color de la paleta activa: los tokens que la paleta define se sobrescriben
 * y el resto se hereda del esquema del modo, para que nunca queden huecos de color.
 */
private fun colorScheme(tokens: PaletteTokens, darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) DarkColors else LightColors
    return base.copy(
        primary = Color(tokens.primary),
        onPrimary = Color(tokens.onPrimary),
        primaryContainer = Color(tokens.primaryContainer),
        background = Color(tokens.background),
        surface = Color(tokens.surface),
        surfaceVariant = Color(tokens.surfaceVariant),
        outline = Color(tokens.outline),
        secondary = Color(tokens.secondary),
        error = Color(tokens.error),
    )
}

@Composable
fun UsipipoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: PaletteVariant = PaletteVariant.AzulNoche,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = palette.tokens(isDark = darkTheme)
    CompositionLocalProvider(
        LocalStatusColors provides statusColors(tokens),
        LocalAtmosphere provides atmosphereColors(tokens),
        LocalPingColors provides pingColors(tokens),
        LocalBottomBarColors provides bottomBarColors(tokens),
        LocalIsDarkTheme provides darkTheme,
        // Con las animaciones apagadas se reduce el movimiento en toda la app, aunque
        // el sistema no esté en modo reducido.
        LocalReduceMotion provides if (animationsEnabled) null else true,
    ) {
        MaterialTheme(
            colorScheme = colorScheme(tokens, darkTheme),
            typography = UsipipoTypography,
            content = content,
        )
    }
}
