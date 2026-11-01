package com.usipipo.vpn.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.data.ConfSummary
import com.usipipo.vpn.data.geo.CountryRepository
import com.usipipo.vpn.data.geo.PingRepository
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.TunnelManager
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * ViewModel que orquesta país (al conectar) y ping (cada 5 s) para la app.
 *
 * - País: se consulta al detectar [TunnelState.Connected] (túnel arriba y con
 *   handshake recibido), extrayendo la IP del endpoint del .conf activo.
 *   Re-consulta solo si cambia el configId o la IP.
 * - Ping: solo corre mientras la pantalla Detalles está visible (lo avisa con
 *   [setDetailsVisible]) y el túnel está conectado. Se cancela al desconectar,
 *   al cambiar de perfil o al ocultar Detalles.
 * - El país solo se consulta con la app en primer plano ([setAppForeground]) y la
 *   petición se cancela al desconectar.
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
    /** Job del loop de ping; se cancela al desconectar, cambiar de perfil u ocultar Detalles. */
    private var pingJob: Job? = null
    /** Job de la consulta de país; se cancela al desconectar o al ir a segundo plano. */
    private var countryJob: Job? = null
    /** ConfigId del túnel conectado, para (re)iniciar el ping cuando Detalles vuelve a verse. */
    private var connectedConfigId: String? = null
    /** La pantalla Detalles avisa de su visibilidad con [setDetailsVisible]. */
    private var detailsVisible = false
    /** La app solo consulta el país en primer plano. */
    private var appInForeground = false
    /** Serializa la comprobacion y actualizacion de la cache de pais (no la llamada de red). */
    private val connectMutex = Mutex()

    init {
        viewModelScope.launch {
            tunnelManager.state.collect { state ->
                when (state) {
                    is TunnelState.Connected -> {
                        connectedConfigId = state.configId
                        // La consulta de pais va en su propia corrutina: el colector no
                        // debe esperar a la red para procesar un desconexion.
                        if (appInForeground) startCountryQuery(state.configId)
                        if (detailsVisible) startPingLoop(state.configId)
                    }
                    else -> {
                        connectedConfigId = null
                        cancelPingLoop()
                        cancelCountryQuery()
                        _ui.update { it.copy(pingResult = null, countryInfo = null) }
                    }
                }
            }
        }
    }

    /**
     * La pantalla Detalles notifica su visibilidad. El ping solo se ejecuta mientras está
     * visible; el país se consulta igualmente al conectar (aunque Detalles no esté abierta).
     */
    fun setDetailsVisible(visible: Boolean) {
        detailsVisible = visible
        if (visible) {
            connectedConfigId?.let { startPingLoop(it) }
        } else {
            cancelPingLoop()
            _ui.update { it.copy(pingResult = null) }
        }
    }

    /**
     * La app avisa de si está en primer plano. El país solo se consulta en primer
     * plano: si el túnel ya estaba conectado (por ejemplo, conectó desde el
     * widget), la consulta espera a que la app vuelva a primer plano.
     */
    fun setAppForeground(foreground: Boolean) {
        appInForeground = foreground
        if (foreground) {
            connectedConfigId?.let { startCountryQuery(it) }
        } else {
            cancelCountryQuery()
        }
    }

    /** Lanza (o relanza) la consulta de país del perfil conectado. */
    private fun startCountryQuery(configId: String) {
        cancelCountryQuery()
        countryJob = viewModelScope.launch {
            val ip = extractEndpointIp(configId) ?: return@launch
            val shouldQuery = connectMutex.withLock {
                if (configId == lastConfigId && ip == lastIp) {
                    false
                } else {
                    lastConfigId = configId
                    lastIp = ip
                    true
                }
            }
            if (!shouldQuery) return@launch

            _ui.update { it.copy(isLoadingCountry = true) }
            val info = countryRepository.countryFor(configId, ip)
            _ui.update { it.copy(countryInfo = info, isLoadingCountry = false) }
        }
    }

    /** Cancela la consulta de país en vuelo y permite consultarla de nuevo. */
    private fun cancelCountryQuery() {
        countryJob?.cancel()
        countryJob = null
        lastConfigId = null
        lastIp = null
        _ui.update { it.copy(isLoadingCountry = false) }
    }

    private fun startPingLoop(configId: String) {
        cancelPingLoop()
        pingJob = viewModelScope.launch {
            val target = extractPingTarget(configId)
            if (target == null) return@launch
            while (detailsVisible) {
                val result = pingRepository.ping(target)
                _ui.update { it.copy(pingResult = result) }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun cancelPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    /**
     * IP pública del servidor a partir del `Endpoint` del .conf.
     *
     * Resuelve el nombre de host a IP (best effort): los proveedores de país solo
     * aceptan IPs literales. Si no resuelve, no se consulta el país (se muestra el globo).
     */
    private suspend fun extractEndpointIp(configId: String): String? {
        val confText = store.readText(configId) ?: return null
        val endpoint = ConfSummary.parse(confText).endpoint ?: return null
        val host = endpointHost(endpoint) ?: return null
        return withContext(Dispatchers.IO) {
            try {
                InetAddress.getByName(host).hostAddress
            } catch (e: UnknownHostException) {
                null
            }
        }
    }

    /** Host del endpoint `host:puerto` o `[ipv6]:puerto`. */
    private fun endpointHost(endpoint: String): String? {
        val host = if (endpoint.startsWith("[")) {
            endpoint.substringAfter('[').substringBefore(']')
        } else {
            endpoint.substringBefore(':')
        }
        return host.trim().takeIf { it.isNotEmpty() }
    }

    private suspend fun extractPingTarget(configId: String): String? {
        val confText = store.readText(configId) ?: return null

        // Intentar derivar .1 del Address del cliente (ej. 10.5.0.2/24 -> 10.5.0.1)
        val address = confText.lineSequence().mapNotNull { line ->
            val sep = line.indexOf('=')
            if (sep < 0) null else line.substring(0, sep).trim().lowercase() to line.substring(sep + 1).trim()
        }.firstOrNull { (key, _) -> key == "address" }?.second

        val dotOne = address?.let { deriveDotOne(it) }
        return dotOne ?: ConfSummary.parse(confText).endpoint?.let { endpointHost(it) }
    }

    private fun deriveDotOne(cidr: String): String? {
        val ip = cidr.split('/').firstOrNull() ?: return null
        val octets = ip.split('.')
        if (octets.size != 4) return null
        if (octets.any { it.toIntOrNull() == null }) return null
        return "${octets[0]}.${octets[1]}.${octets[2]}.1"
    }

    override fun onCleared() {
        super.onCleared()
        detailsVisible = false
        cancelPingLoop()
        viewModelScope.cancel()
    }

    private companion object {
        /** Intervalo entre pings: 5 s (spec RNF/plan). */
        const val PING_INTERVAL_MS = 5_000L
    }
}
