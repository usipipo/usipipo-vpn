package com.usipipo.vpn.quickconnect

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.selectedConfigId
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class QuickConnectController(
    private val store: ConfigStore,
    private val tunnelManager: TunnelManager,
    private val permissionChecker: VpnPermissionChecker,
    private val scope: CoroutineScope,
) {
    private val _failure = MutableStateFlow<QuickConnectFailure?>(null)

    val state: StateFlow<QuickConnectState> = combine(
        store.configs,
        store.lastUsedId,
        tunnelManager.state,
        _failure,
    ) { configs, lastUsed, tunnel, failure ->
        val id = selectedConfigId(configs, lastUsed)
        QuickConnectState(
            tunnel = tunnel,
            selectedConfig = configs.firstOrNull { it.id == id },
            failure = failure,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = QuickConnectState(),
    )

    suspend fun toggle(): QuickConnectAction {
        if (tunnelManager.state.value.isActive) {
            tunnelManager.disconnect()
            return QuickConnectAction.DisconnectRequested
        }

        val configs = store.configs.value
        val id = selectedConfigId(configs, store.lastUsedId.value) ?: run {
            _failure.value = null
            return QuickConnectAction.OpenImporter
        }

        _failure.value = null

        if (!permissionChecker.isGranted()) {
            return QuickConnectAction.VpnPermissionRequired
        }

        val text = store.readText(id)
        if (text == null) {
            _failure.value = QuickConnectFailure.ConfigUnreadable(id)
            AppLog.w("AccesoRapido", "Configuración no legible (cfg ${id.take(8)})")
            return QuickConnectAction.ConfigUnreadable(id)
        }

        AppLog.i("AccesoRapido", "Conexión rápida (cfg ${id.take(8)})")
        tunnelManager.connect(id, text)
        return QuickConnectAction.ConnectRequested(id)
    }

    fun onPermissionDenied() {
        AppLog.w("Permiso", "Permiso de VPN denegado desde acceso rápido")
    }
}
