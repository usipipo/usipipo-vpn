package com.usipipo.vpn.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.PaletteVariant
import com.usipipo.vpn.settings.ThemeMode

/** Aspecto visual elegido por el usuario: tema, animaciones y paleta. */
@Immutable
data class AppearanceState(
    val themeMode: ThemeMode,
    val animationsEnabled: Boolean,
    val palette: PaletteVariant,
)

/**
 * Lee el aspecto visual del almacén y lo mantiene al día: cada cambio de
 * `theme_mode`, `animations_enabled` o `palette` se aplica de inmediato, sin
 * reiniciar la app.
 */
@Composable
fun rememberAppearanceState(appSettings: AppSettings): AppearanceState {
    var themeMode by remember { mutableStateOf(ThemeMode.fromKey(appSettings.themeMode)) }
    var animationsEnabled by remember { mutableStateOf(appSettings.animationsEnabled) }
    var palette by remember { mutableStateOf(PaletteVariant.fromKey(appSettings.palette)) }

    LaunchedEffect(appSettings) {
        appSettings.changedKeys.collect { key ->
            when (key) {
                AppSettings.KEY_THEME_MODE -> themeMode = ThemeMode.fromKey(appSettings.themeMode)
                AppSettings.KEY_ANIMATIONS_ENABLED -> animationsEnabled = appSettings.animationsEnabled
                AppSettings.KEY_PALETTE -> palette = PaletteVariant.fromKey(appSettings.palette)
            }
        }
    }

    return AppearanceState(
        themeMode = themeMode,
        animationsEnabled = animationsEnabled,
        palette = palette,
    )
}
