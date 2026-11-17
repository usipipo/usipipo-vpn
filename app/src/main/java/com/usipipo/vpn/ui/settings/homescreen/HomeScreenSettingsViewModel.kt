package com.usipipo.vpn.ui.settings.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Estado de la subpantalla de pantalla inicial y panel. */
data class HomeScreenSettingsUiState(
    val showCountry: Boolean,
)

/**
 * Ajustes de la pantalla inicial: qué muestra el widget.
 *
 * El estado se mantiene vivo con los cambios del almacén: al ocultar el país, la
 * pantalla lo refleja al momento y el widget lo aplica en su próximo repintado.
 */
class HomeScreenSettingsViewModel(private val appSettings: AppSettings) : ViewModel() {

    /** Estado actual de los ajustes de la pantalla inicial. */
    val uiState: StateFlow<HomeScreenSettingsUiState> = appSettings.changedKeys
        .map { readState() }
        .onStart { emit(readState()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = readState(),
        )

    /** Muestra u oculta el perfil y el país en el widget. */
    fun setShowCountry(enabled: Boolean) {
        appSettings.widgetShowCountry = enabled
    }

    private fun readState(): HomeScreenSettingsUiState = HomeScreenSettingsUiState(
        showCountry = appSettings.widgetShowCountry,
    )
}