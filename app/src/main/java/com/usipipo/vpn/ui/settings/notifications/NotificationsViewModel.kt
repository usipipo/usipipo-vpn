package com.usipipo.vpn.ui.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la subpantalla de notificaciones. */
data class NotificationsUiState(
    val notificationsEnabled: Boolean,
    val trafficEnabled: Boolean,
    val disconnectEnabled: Boolean,
    val updatesEnabled: Boolean,
    val notificationsGranted: Boolean,
)

/**
 * Avisos de la app: interruptor general y un interruptor por tipo.
 *
 * El interruptor general silencia todos los avisos propios, pero no borra la
 * decisión de cada tipo: al volver a encenderlo, los avisos quedan como el
 * usuario los dejó. La pantalla deshabilita los tres interruptores cuando el
 * general está apagado.
 */
class NotificationsViewModel(
    private val appSettings: AppSettings,
    private val notificationsGranted: () -> Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.changedKeys.collect { _uiState.value = readState() }
        }
    }

    /** Activa o desactiva todos los avisos propios. */
    fun setNotificationsEnabled(enabled: Boolean) {
        appSettings.notificationsEnabled = enabled
    }

    /** Aviso de estado del túnel con tráfico. */
    fun setTrafficEnabled(enabled: Boolean) {
        appSettings.notificationTraffic = enabled
    }

    /** Aviso de desconexión o fallo. */
    fun setDisconnectEnabled(enabled: Boolean) {
        appSettings.notificationDisconnect = enabled
    }

    /** Aviso de actualización disponible. */
    fun setUpdatesEnabled(enabled: Boolean) {
        appSettings.notificationUpdates = enabled
    }

    /**
     * Relee el permiso de notificaciones. El permiso se concede en los ajustes del
     * sistema, así que hay que volver a leerlo al regresar a la app.
     */
    fun refreshPermission() {
        _uiState.value = readState()
    }

    private fun readState(): NotificationsUiState = NotificationsUiState(
        notificationsEnabled = appSettings.notificationsEnabled,
        trafficEnabled = appSettings.notificationTraffic,
        disconnectEnabled = appSettings.notificationDisconnect,
        updatesEnabled = appSettings.notificationUpdates,
        notificationsGranted = notificationsGranted(),
    )
}
