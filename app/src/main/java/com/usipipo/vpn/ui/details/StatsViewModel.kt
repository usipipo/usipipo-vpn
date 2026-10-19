package com.usipipo.vpn.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usipipo.vpn.data.ConfSummary
import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.SpeedCalculator
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estadísticas de la conexión activa. Las velocidades son bytes por segundo. */
data class StatsUiState(
    val downBps: Long = 0L,
    val upBps: Long = 0L,
    val downHistory: List<Long> = emptyList(),
    val upHistory: List<Long> = emptyList(),
    val endpoint: String? = null,
    val routesIpv6: Boolean = false,
)

/**
 * Convierte los contadores del túnel en velocidad. Solo lee mientras alguien recoge [ui]
 * (Inicio o Detalles visibles): `WhileSubscribed` apaga el sondeo cinco segundos después.
 */
class StatsViewModel(
    private val store: ConfigStore,
    private val tunnelManager: TunnelManager,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val ui: StateFlow<StatsUiState> = tunnelManager.state
        .map { (it as? TunnelState.Connected)?.configId }
        .distinctUntilChanged()
        .flatMapLatest { configId ->
            if (configId == null) flowOf(StatsUiState()) else statsFor(configId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
        )

    private fun statsFor(configId: String): Flow<StatsUiState> = flow {
        // El .conf se lee solo para extraer endpoint e IPv6; el texto no se conserva.
        val summary = store.readText(configId)?.let { ConfSummary.parse(it) }
        var current = StatsUiState(
            endpoint = summary?.endpoint,
            routesIpv6 = summary?.routesIpv6 ?: false,
        )
        emit(current)

        val calculator = SpeedCalculator()
        tunnelManager.counters().collect { counters ->
            val throughput = calculator.add(counters)
            current = current.copy(
                downBps = throughput.downBps,
                upBps = throughput.upBps,
                downHistory = (current.downHistory + throughput.downBps).takeLast(HISTORY_SIZE),
                upHistory = (current.upHistory + throughput.upBps).takeLast(HISTORY_SIZE),
            )
            emit(current)
        }
    }

    companion object {
        /** Muestras de la gráfica: un minuto, a una por segundo. */
        const val HISTORY_SIZE = 60
    }
}
