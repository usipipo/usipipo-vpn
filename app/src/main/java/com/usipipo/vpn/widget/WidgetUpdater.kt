package com.usipipo.vpn.widget

import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.quickconnect.QuickConnectState
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Mantiene el widget y el mosaico al día: redibuja solo cuando el estado del túnel cambia.
 *
 * El estado vivo siempre manda sobre el guardado. Si el proceso muere con el túnel todavía
 * marcado como conectado, el primer render contrasta ambos y anota la invalidación.
 */
class WidgetUpdater(
    private val scope: CoroutineScope,
    private val states: StateFlow<QuickConnectState>,
    private val renderWidget: (QuickConnectState) -> Unit,
    private val refreshTile: () -> Unit,
    private val store: WidgetStateStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val started = AtomicBoolean(false)

    /** Arranca la colección del estado. Es idempotente: llamarlo dos veces no la duplica. */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            // StateFlow solo emite valores distintos: no hace falta distinctUntilChanged.
            states.collect { state ->
                apply(state)
            }
        }
    }

    /** Fuerza un redibulado sin esperar a un cambio de estado (arranque o toque). */
    fun refreshNow() {
        scope.launch { apply(states.value) }
    }

    /** Único camino de aplicación: dibujar, refrescar el mosaico y persistir el estado. */
    private fun apply(state: QuickConnectState) {
        val presentation = WidgetStateMapper.map(state)
        invalidateStalePersistedState(presentation.status)
        renderWidget(state)
        runCatching { refreshTile() }
            .onFailure { AppLog.w(TAG, "No se pudo refrescar el mosaico", it) }
        store.write(PersistedWidgetState(presentation.status, clock()))
    }

    /**
     * El túnel vive en el proceso de la app: si el proceso muere, el túnel cae. Se anota la
     * invalidación del estado activo guardado, pero el estado vivo sigue mandando.
     */
    private fun invalidateStalePersistedState(liveStatus: WidgetStatus) {
        val persisted = store.read() ?: return
        if (persisted.status == WidgetStatus.Connected && liveStatus == WidgetStatus.Disconnected) {
            AppLog.i(TAG, "El estado vivo desconectado invalida el estado activo persistido")
        }
    }

    private companion object {
        const val TAG = "Widget"
    }
}
