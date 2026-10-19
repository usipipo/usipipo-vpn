package com.usipipo.vpn.tunnel

import android.content.Context
import android.os.SystemClock
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.TunnelCounters
import com.usipipo.vpn.domain.TunnelState
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Implementación sobre la librería oficial (GoBackend).
 *
 * WireGuard no tiene "conexión" real: el túnel puede estar arriba sin que el servidor
 * responda. Por eso el estado Connected solo se emite al recibir el primer handshake;
 * si en [HANDSHAKE_TIMEOUT_MS] no llega, el estado pasa a NoResponse (y sigue vigilando).
 *
 * Registro: solo metadatos. Las excepciones del parser de configuración NO se pasan al
 * registro (su mensaje puede contener texto del .conf); se anota solo el tipo de error.
 */
class WireGuardTunnelManager(
    context: Context,
    private val scope: CoroutineScope,
) : TunnelManager {

    private class ActiveTunnel(val configId: String, val tunnel: Tunnel)

    private val appContext = context.applicationContext
    private val backend by lazy { GoBackend(appContext) }
    private val mutex = Mutex()

    private val _state = MutableStateFlow<TunnelState>(TunnelState.Disconnected)
    override val state: StateFlow<TunnelState> = _state.asStateFlow()

    @Volatile
    private var active: ActiveTunnel? = null
    private var job: Job? = null

    override fun connect(configId: String, configText: String) {
        AppLog.i(TAG, "Conectar solicitado (cfg ${shortId(configId)})")
        job?.cancel()
        job = scope.launch { runConnect(configId, configText) }
    }

    override fun disconnect() {
        AppLog.i(TAG, "Desconectar solicitado")
        job?.cancel()
        job = scope.launch {
            mutex.withLock { shutDownLocked() }
            setState(TunnelState.Disconnected)
        }
    }

    private suspend fun runConnect(configId: String, configText: String) {
        setState(TunnelState.Connecting(configId))

        val config = try {
            withContext(Dispatchers.Default) {
                Config.parse(ByteArrayInputStream(configText.toByteArray(Charsets.UTF_8)))
            }
        } catch (e: BadConfigException) {
            AppLog.e(TAG, "La configuración guardada no es válida (${e.javaClass.simpleName})")
            setState(TunnelState.Failed(configId))
            return
        } catch (e: IOException) {
            AppLog.e(TAG, "No se pudo leer la configuración guardada (${e.javaClass.simpleName})")
            setState(TunnelState.Failed(configId))
            return
        }

        val tunnel = UsipipoTunnel(tunnelNameFor(configId))
        try {
            mutex.withLock {
                shutDownLocked()
                // Se registra antes de subir: si se cancela a mitad, el siguiente
                // shutDownLocked() sabe qué túnel hay que bajar.
                active = ActiveTunnel(configId, tunnel)
                AppLog.d(TAG, "Subiendo túnel ${tunnel.name}")
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.UP, config)
                }
                AppLog.d(TAG, "Túnel ${tunnel.name} arriba; esperando handshake")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(TAG, "Fallo al subir el túnel", e)
            if (active?.tunnel === tunnel) active = null
            setState(TunnelState.Failed(configId))
            return
        }

        monitorHandshake(tunnel, configId)
    }

    private suspend fun shutDownLocked() {
        val current = active ?: return
        // Se anula antes de bajar para que el callback onStateChange ignore este cierre.
        active = null
        withContext(NonCancellable + Dispatchers.IO) {
            runCatching { backend.setState(current.tunnel, Tunnel.State.DOWN, null) }
                .onFailure { AppLog.w(TAG, "Error al bajar el túnel", it) }
        }
    }

    private suspend fun monitorHandshake(tunnel: Tunnel, configId: String) {
        val start = SystemClock.elapsedRealtime()
        while (active?.tunnel === tunnel) {
            val handshake = withContext(Dispatchers.IO) { hasHandshake(tunnel) }
            val elapsed = SystemClock.elapsedRealtime() - start
            AppLog.d(TAG, "Sondeo de handshake: ${if (handshake) "recibido" else "pendiente"} (t=$elapsed ms)")
            if (handshake) {
                AppLog.i(TAG, "Handshake recibido a los $elapsed ms")
                setState(TunnelState.Connected(configId, SystemClock.elapsedRealtime()))
                return
            }
            if (elapsed >= HANDSHAKE_TIMEOUT_MS && _state.value !is TunnelState.NoResponse) {
                AppLog.w(TAG, "Sin handshake tras ${HANDSHAKE_TIMEOUT_MS / 1000} s")
                setState(TunnelState.NoResponse(configId))
            }
            delay(if (_state.value is TunnelState.NoResponse) SLOW_POLL_MS else FAST_POLL_MS)
        }
    }

    private fun hasHandshake(tunnel: Tunnel): Boolean = try {
        val stats = backend.getStatistics(tunnel)
        stats.peers().any { key -> (stats.peer(key)?.latestHandshakeEpochMillis() ?: 0L) > 0L }
    } catch (e: Exception) {
        AppLog.d(TAG, "getStatistics falló (${e.javaClass.simpleName})")
        false
    }

    override fun counters(): Flow<TunnelCounters> = flow {
        while (true) {
            val tunnel = active?.tunnel
            if (tunnel != null) {
                val totals = withContext(Dispatchers.IO) { readTotals(tunnel) }
                if (totals != null) {
                    emit(TunnelCounters(totals.first, totals.second, SystemClock.elapsedRealtime()))
                }
            }
            delay(STATS_POLL_MS)
        }
    }

    /** Suma recibidos y enviados de todos los peers; null si la librería falla esta vez. */
    private fun readTotals(tunnel: Tunnel): Pair<Long, Long>? = try {
        val stats = backend.getStatistics(tunnel)
        var rx = 0L
        var tx = 0L
        stats.peers().forEach { key ->
            stats.peer(key)?.let {
                rx += it.rxBytes()
                tx += it.txBytes()
            }
        }
        rx to tx
    } catch (e: Exception) {
        AppLog.d(TAG, "getStatistics falló (${e.javaClass.simpleName})")
        null
    }

    private fun setState(newState: TunnelState) {
        if (_state.value != newState) AppLog.i(TAG, "Estado: ${describe(newState)}")
        _state.value = newState
    }

    private fun describe(state: TunnelState): String = when (state) {
        TunnelState.Disconnected -> "Desconectado"
        is TunnelState.Connecting -> "Conectando (cfg ${shortId(state.configId)})"
        is TunnelState.Connected -> "Conectado (cfg ${shortId(state.configId)})"
        is TunnelState.NoResponse -> "Sin respuesta (cfg ${shortId(state.configId)})"
        is TunnelState.Failed -> "Fallo (cfg ${state.configId?.let { shortId(it) } ?: "-"})"
    }

    private fun shortId(id: String) = id.take(8)

    /** Nombre de interfaz válido: máx. 15 caracteres de [a-zA-Z0-9_=+.-]. */
    private fun tunnelNameFor(configId: String) = "usp" + configId.replace("-", "").take(10)

    private inner class UsipipoTunnel(private val tunnelName: String) : Tunnel {
        override fun getName(): String = tunnelName

        override fun onStateChange(newState: Tunnel.State) {
            // Cierre externo (otro VPN, permiso revocado desde Ajustes...).
            if (newState == Tunnel.State.DOWN && active?.tunnel === this) {
                AppLog.w(TAG, "El sistema cerró el túnel")
                active = null
                setState(TunnelState.Disconnected)
            }
        }
    }

    private companion object {
        const val TAG = "Tunel"
        const val HANDSHAKE_TIMEOUT_MS = 10_000L
        const val FAST_POLL_MS = 500L
        const val SLOW_POLL_MS = 2_000L
        const val STATS_POLL_MS = 1_000L
    }
}
