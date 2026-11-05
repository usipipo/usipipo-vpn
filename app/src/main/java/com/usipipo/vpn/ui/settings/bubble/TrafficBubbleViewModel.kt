package com.usipipo.vpn.ui.settings.bubble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la subpantalla de la burbuja de tráfico. */
data class TrafficBubbleUiState(
    val bubbleEnabled: Boolean,
    val bubbleX: Float,
    val bubbleY: Float,
    val overlayGranted: Boolean,
)

/**
 * Ajustes de la burbuja de tráfico: interruptor y posición guardada.
 *
 * El permiso de superposición lo concede el sistema, así que se lee con un
 * proveedor y se vuelve a leer al regresar a la app ([refreshPermission]).
 */
class TrafficBubbleViewModel(
    private val appSettings: AppSettings,
    private val overlayGranted: () -> Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<TrafficBubbleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.changedKeys.collect { _uiState.value = readState() }
        }
    }

    /** Muestra u oculta la burbuja mientras el túnel esté activo. */
    fun setBubbleEnabled(enabled: Boolean) {
        appSettings.bubbleEnabled = enabled
    }

    /** Guarda la posición de la burbuja al soltar el arrastre. */
    fun setPosition(x: Float, y: Float) {
        appSettings.bubbleX = x
        appSettings.bubbleY = y
    }

    /** Relee el permiso de superposición: se concede en los ajustes del sistema. */
    fun refreshPermission() {
        _uiState.value = readState()
    }

    private fun readState(): TrafficBubbleUiState = TrafficBubbleUiState(
        bubbleEnabled = appSettings.bubbleEnabled,
        bubbleX = appSettings.bubbleX,
        bubbleY = appSettings.bubbleY,
        overlayGranted = overlayGranted(),
    )
}
