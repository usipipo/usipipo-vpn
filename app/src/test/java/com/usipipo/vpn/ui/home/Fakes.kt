package com.usipipo.vpn.ui.home

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.data.DuplicateNameException
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelCounters
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeConfigStore(
    configs: List<SavedConfig> = emptyList(),
    private val texts: Map<String, String> = emptyMap(),
) : ConfigStore {
    private val _configs = MutableStateFlow(configs)
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
        texts[id] ?: if (_configs.value.any { it.id == id }) "conf-de-$id" else null

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

    override suspend fun rename(id: String, name: String): Result<SavedConfig> {
        val clean = name.trim()
        val current = _configs.value.firstOrNull { it.id == id }
            ?: return Result.failure(NoSuchElementException())
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Nombre vacío"))
        if (_configs.value.any { it.id != id && it.name.equals(clean, ignoreCase = true) }) {
            return Result.failure(DuplicateNameException())
        }
        val renamed = current.copy(name = clean)
        _configs.value = _configs.value.map { if (it.id == id) renamed else it }
        return Result.success(renamed)
    }

    override suspend fun updateCountry(id: String, countryInfo: CountryInfo) {
        _configs.value = _configs.value.map { cfg ->
            if (cfg.id == id) {
                cfg.copy(
                    countryCode = countryInfo.countryCode,
                    countryName = countryInfo.countryName,
                    countryIp = countryInfo.ip,
                    countryFetchedAt = countryInfo.fetchedAt,
                )
            } else cfg
        }
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

    val counterSamples = MutableSharedFlow<TunnelCounters>(extraBufferCapacity = 16)

    override fun counters(): Flow<TunnelCounters> = counterSamples
}
