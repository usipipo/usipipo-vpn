package com.usipipo.vpn.ui.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.PaletteVariant
import com.usipipo.vpn.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Estado de la subpantalla de apariencia. */
data class AppearanceUiState(
    val themeMode: ThemeMode,
    val animationsEnabled: Boolean,
    val palette: PaletteVariant,
)

/**
 * Ajustes de apariencia: modo de tema, animaciones y paleta de color.
 *
 * El estado se mantiene vivo con los cambios del almacen: al cambiar una
 * preferencia, la pantalla y el tema de la app reaccionan de inmediato.
 */
class AppearanceViewModel(private val appSettings: AppSettings) : ViewModel() {

    /** Estado actual de los ajustes de apariencia. */
    val uiState: StateFlow<AppearanceUiState> = appSettings.changedKeys
        .map { readState() }
        .onStart { emit(readState()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = readState(),
        )

    /** Elige el modo de tema. */
    fun setThemeMode(mode: ThemeMode) {
        appSettings.themeMode = mode.key
    }

    /** Activa o desactiva las animaciones. */
    fun setAnimationsEnabled(enabled: Boolean) {
        appSettings.animationsEnabled = enabled
    }

    /** Elige la paleta de color. */
    fun setPalette(palette: PaletteVariant) {
        appSettings.palette = palette.key
    }

    private fun readState(): AppearanceUiState = AppearanceUiState(
        themeMode = ThemeMode.fromKey(appSettings.themeMode),
        animationsEnabled = appSettings.animationsEnabled,
        palette = PaletteVariant.fromKey(appSettings.palette),
    )
}
