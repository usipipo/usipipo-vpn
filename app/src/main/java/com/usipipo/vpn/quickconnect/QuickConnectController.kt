package com.usipipo.vpn.quickconnect

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.isActive
import com.usipipo.vpn.domain.selectedConfigId
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class QuickConnectController(
    private val store: ConfigStore,
    private val tunnelManager: TunnelManager,
    private val permissionChecker: VpnPermissionChecker,
    private val connector: TunnelConnector,
    private val scope: CoroutineScope,
) {
    private val _failure = MutableStateFlow<QuickConnectFailure?>(null)

    private val _state = MutableStateFlow(
        QuickConnectState(
            tunnel = tunnelManager.state.value,
            selectedConfig = configSeleccionado(store.configs.value, store.lastUsedId.value),
            failure = null,
        ),
    )

    val state: StateFlow<QuickConnectState> = _state

    init {
        scope.launch {
            combine(store.configs, store.lastUsedId, tunnelManager.state, _failure) { configs, lastUsed, tunnel, failure ->
                QuickConnectState(
                    tunnel = tunnel,
                    selectedConfig = configSeleccionado(configs, lastUsed),
                    failure = failure,
                )
            }.collect { _state.value = it }
        }
    }

    private fun configSeleccionado(configs: List<SavedConfig>, lastUsedId: String?): SavedConfig? {
        val id = selectedConfigId(configs, lastUsedId)
        return id?.let { configs.firstOrNull { it.id == id } }
    }

    suspend fun toggle(): QuickConnectAction {
        if (tunnelManager.state.value.isActive) {
            tunnelManager.disconnect()
            _state.value = _state.value.copy(tunnel = tunnelManager.state.value)
            return QuickConnectAction.DisconnectRequested
        }

        val configs = store.configs.value
        val id = selectedConfigId(configs, store.lastUsedId.value) ?: run {
            _failure.value = null
            _state.value = _state.value.copy(failure = null, selectedConfig = null)
            return QuickConnectAction.OpenImporter
        }

        _failure.value = null

        if (!permissionChecker.isGranted()) {
            _state.value = _state.value.copy(failure = null)
            return QuickConnectAction.VpnPermissionRequired
        }

        val text = store.readText(id)
        if (text == null) {
            _failure.value = QuickConnectFailure.ConfigUnreadable(id)
            AppLog.w("AccesoRapido", "Configuración no legible (cfg ${id.take(8)})")
            _state.value = _state.value.copy(failure = _failure.value)
            return QuickConnectAction.ConfigUnreadable(id)
        }

        AppLog.i("AccesoRapido", "Conexión rápida (cfg ${id.take(8)})")
        val started = connector.connect(id)
        if (!started) {
            AppLog.w("AccesoRapido", "El sistema denegó el arranque en segundo plano (cfg ${id.take(8)})")
            _failure.value = QuickConnectFailure.BackgroundStartDenied(id)
            _state.value = _state.value.copy(failure = _failure.value)
            return QuickConnectAction.BackgroundStartDenied
        }
        _state.value = _state.value.copy(
            tunnel = tunnelManager.state.value,
            selectedConfig = configs.firstOrNull { it.id == id },
            failure = null,
        )
        return QuickConnectAction.ConnectRequested(id)
    }

    fun onPermissionDenied() {
        AppLog.w("Permiso", "Permiso de VPN denegado desde acceso rápido")
    }
}
