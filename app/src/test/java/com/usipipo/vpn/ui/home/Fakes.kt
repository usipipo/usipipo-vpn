package com.usipipo.vpn.ui.home

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeConfigStore(initial: List<SavedConfig> = emptyList()) : ConfigStore {
    private val _configs = MutableStateFlow(initial)
    private val _lastUsed = MutableStateFlow<String?>(null)

    override val configs: StateFlow<List<SavedConfig>> = _configs
    override val lastUsedId: StateFlow<String?> = _lastUsed

    override suspend fun add(name: String, configText: String): Result<SavedConfig> {
        val saved = SavedConfig(id = "id-${_configs.value.size + 1}", name = name)
        _configs.value = _configs.value + saved
        _lastUsed.value = saved.id
        return Result.success(saved)
    }

    /** Texto predecible para comprobar qué se le pasa al túnel. */
    override suspend fun readText(id: String): String? =
        if (_configs.value.any { it.id == id }) "conf-de-$id" else null

    override suspend fun remove(id: String) {
        _configs.value = _configs.value.filterNot { it.id == id }
        if (_lastUsed.value == id) _lastUsed.value = null
    }

    override suspend fun setLastUsed(id: String) {
        _lastUsed.value = id
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        _configs.value = _configs.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
    }
}

class FakeTunnelManager : TunnelManager {
    private val _state = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
    override val state: StateFlow<TunnelState> = _state

    val connectCalls = mutableListOf<Pair<String, String>>()
    var disconnectCalls = 0

    override fun connect(configId: String, configText: String) {
        connectCalls += configId to configText
        _state.value = TunnelState.Connecting(configId)
    }

    override fun disconnect() {
        disconnectCalls++
        _state.value = TunnelState.Disconnected
    }

    fun emit(state: TunnelState) {
        _state.value = state
    }
}
