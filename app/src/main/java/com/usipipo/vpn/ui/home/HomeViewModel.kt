package com.usipipo.vpn.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.domain.activeConfigId
import com.usipipo.vpn.domain.selectedConfigId
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeMessage { PermissionDenied, ConfigUnreadable }

data class HomeUiState(
    val configs: List<SavedConfig> = emptyList(),
    val selectedId: String? = null,
    val tunnel: TunnelState = TunnelState.Disconnected,
    val message: HomeMessage? = null,
)

class HomeViewModel(
    private val store: ConfigStore,
    private val tunnelManager: TunnelManager,
) : ViewModel() {

    private val message = MutableStateFlow<HomeMessage?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        store.configs,
        store.lastUsedId,
        tunnelManager.state,
        message,
    ) { configs, lastUsed, tunnel, msg ->
        buildState(configs, lastUsed, tunnel, msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildState(
            store.configs.value,
            store.lastUsedId.value,
            tunnelManager.state.value,
            null,
        ),
    )

    fun select(id: String) {
        viewModelScope.launch { store.setLastUsed(id) }
    }

    fun toggleFavorite(id: String) {
        val current = store.configs.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch { store.setFavorite(id, !current.favorite) }
    }

    /** Llamar solo cuando el permiso de VPN ya está concedido. */
    fun connectSelected() {
        val id = selectedConfigId(store.configs.value, store.lastUsedId.value) ?: return
        viewModelScope.launch {
            val text = store.readText(id)
            if (text == null) {
                AppLog.e("Inicio", "No se pudo leer la configuración seleccionada (cfg ${id.take(8)})")
                message.value = HomeMessage.ConfigUnreadable
            } else {
                tunnelManager.connect(id, text)
            }
        }
    }

    fun disconnect() = tunnelManager.disconnect()

    fun delete(id: String) {
        AppLog.i("Inicio", "Configuración eliminada (cfg ${id.take(8)})")
        viewModelScope.launch {
            if (tunnelManager.state.value.activeConfigId == id) tunnelManager.disconnect()
            store.remove(id)
        }
    }

    suspend fun rename(id: String, name: String): Result<SavedConfig> = store.rename(id, name)

    fun onPermissionDenied() {
        AppLog.w("Permiso", "Permiso de VPN denegado por el usuario")
        message.value = HomeMessage.PermissionDenied
    }

    fun dismissMessage() {
        message.value = null
    }

    private fun buildState(
        configs: List<SavedConfig>,
        lastUsed: String?,
        tunnel: TunnelState,
        msg: HomeMessage?,
    ) = HomeUiState(
        configs = configs,
        selectedId = selectedConfigId(configs, lastUsed),
        tunnel = tunnel,
        message = msg,
    )
}
