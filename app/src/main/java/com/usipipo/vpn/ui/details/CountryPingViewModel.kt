package com.usipipo.vpn.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.data.ConfSummary
import com.usipipo.vpn.data.geo.CountryRepository
import com.usipipo.vpn.data.geo.PingRepository
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ViewModel que orquesta país (una vez al conectar) y ping (loop cada 5 s)
 * para la pantalla Detalles.
 *
 * - País: se consulta al detectar [TunnelState.Connected], extrayendo la IP del endpoint
 *   del .conf activo. Re-consulta solo si cambia el configId o la IP.
 * - Ping: se ejecuta cada 5 s mientras el túnel esté conectado. Se cancela al desconectar,
 *   al cambiar de perfil y al salir de la pantalla Detalles (ViewModel destruido).
 * - Nunca bloquea: fallos de país/ping se degradan a valores nulos o error.
 */
class CountryPingViewModel(
    private val store: ConfigStore,
    private val tunnelManager: TunnelManager,
    private val countryRepository: CountryRepository,
    private val pingRepository: PingRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(CountryPingUiState())
    val ui: StateFlow<CountryPingUiState> = _ui.asStateFlow()

    private var lastConfigId: String? = null
    private var lastIp: String? = null
    /** Job del loop de ping; se cancela al desconectar o al cambiar de perfil. */
    private var pingJob: kotlinx.coroutines.Job? = null
    /** Serializa el acceso a handleConnected para evitar bucles de ping concurrentes. */
    private val connectMutex = Mutex()

    init {
        viewModelScope.launch {
            tunnelManager.state.collect { state ->
                when (state) {
                    is TunnelState.Connected -> {
                        connectMutex.withLock {
                            handleConnected(state.configId)
                        }
                    }
                    else -> {
                        cancelPingLoop()
                        lastConfigId = null
                        lastIp = null
                        _ui.update { it.copy(pingResult = null) }
                    }
                }
            }
        }
    }

    private fun handleConnected(configId: String) = viewModelScope.launch {
        val ip = extractEndpointIp(configId) ?: return@launch
        if (configId == lastConfigId && ip == lastIp) return@launch
        cancelPingLoop()
        lastConfigId = configId
        lastIp = ip

        _ui.update { it.copy(isLoadingCountry = true) }
        val info = countryRepository.countryFor(configId, ip)
        _ui.update { it.copy(countryInfo = info, isLoadingCountry = false) }

        launchPingLoop(configId)
    }

    private fun launchPingLoop(configId: String) {
        pingJob = viewModelScope.launch {
            val target = extractPingTarget(configId)
            if (target == null) return@launch
            while (isActive) {
                val result = pingRepository.ping(target)
                _ui.update { it.copy(pingResult = result) }
                delay(5000)
            }
        }
    }

    private fun cancelPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun extractEndpointIp(configId: String): String? {
        val confText = store.readText(configId) ?: return null
        val endpoint = ConfSummary.parse(confText).endpoint ?: return null
        return endpoint.substringBefore(':').takeIf { it.isNotEmpty() }
    }

    private fun extractPingTarget(configId: String): String? {
        val confText = store.readText(configId) ?: return null

        // Intentar derivar .1 del Address del cliente (ej. 10.5.0.2/24 -> 10.5.0.1)
        val address = confText.lineSequence().mapNotNull { line ->
            val sep = line.indexOf('=')
            if (sep < 0) null else line.substring(0, sep).trim().lowercase() to line.substring(sep + 1).trim()
        }.firstOrNull { (key, _) -> key == "address" }?.second

        val dotOne = address?.let { deriveDotOne(it) }
        return dotOne ?: ConfSummary.parse(confText).endpoint?.substringBefore(':')
    }

    private fun deriveDotOne(cidr: String): String? {
        val ip = cidr.split('/').firstOrNull() ?: return null
        val octets = ip.split('.')
        if (octets.size != 4) return null
        val last = octets[3].toIntOrNull() ?: return null
        return "${octets[0]}.${octets[1]}.${octets[2]}.1"
    }

    override fun onCleared() {
        super.onCleared()
        cancelPingLoop()
        viewModelScope.cancel()
    }
}
