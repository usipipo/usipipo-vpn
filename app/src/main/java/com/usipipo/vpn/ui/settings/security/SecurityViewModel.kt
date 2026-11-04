package com.usipipo.vpn.ui.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.data.ExcludedAppsConfig
import com.usipipo.vpn.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Estado de la subpantalla de Seguridad. */
data class SecurityUiState(
    val biometricEnabled: Boolean,
    val excludedApps: List<String>,
)

/** Seguridad: desbloqueo biométrico y lista global de apps excluidas del túnel. */
class SecurityViewModel(private val appSettings: AppSettings) : ViewModel() {

    /** Estado actual; se mantiene vivo con los cambios del almacén. */
    val uiState: StateFlow<SecurityUiState> = appSettings.changedKeys
        .map { readState() }
        .onStart { emit(readState()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = readState(),
        )

    /** Activa o desactiva el desbloqueo biométrico. La autenticación la pide la pantalla. */
    fun setBiometricEnabled(enabled: Boolean) {
        appSettings.biometricEnabled = enabled
    }

    /** Sustituye la lista global de apps excluidas del túnel. */
    fun setExcludedApps(packages: List<String>) {
        appSettings.excludedApps = ExcludedAppsConfig.encode(packages)
    }

    private fun readState(): SecurityUiState = SecurityUiState(
        biometricEnabled = appSettings.biometricEnabled,
        excludedApps = ExcludedAppsConfig.decode(appSettings.excludedApps),
    )
}
