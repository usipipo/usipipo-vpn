package com.usipipo.vpn.widget

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.quickconnect.QuickConnectState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetUpdaterTest {

    @Test
    fun `un estado repetido no se dibuja dos veces`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        val updater = updater(states, backgroundScope) { renders++ }

        updater.start()
        advanceUntilIdle()
        states.value = desconectado
        advanceUntilIdle()

        assertEquals(1, renders)
    }

    @Test
    fun `un cambio dibuja y actualiza el mosaico`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        var tileRefreshes = 0
        val updater = updater(states, backgroundScope, refreshTile = { tileRefreshes++ }) { renders++ }

        updater.start()
        advanceUntilIdle()
        states.value = conectado
        advanceUntilIdle()

        assertEquals(2, renders)
        assertEquals(2, tileRefreshes)
    }

    @Test
    fun `arrancar dos veces no duplica la coleccion`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        val updater = updater(states, backgroundScope) { renders++ }

        updater.start()
        updater.start()
        advanceUntilIdle()

        assertEquals(1, renders)
    }

    @Test
    fun `el estado vivo desconectado invalida un estado activo persistido`() = runTest {
        val store = FakeWidgetStateStore(PersistedWidgetState(WidgetStatus.Connected, 1L))
        val states = MutableStateFlow(desconectado)
        val updater = updater(states, backgroundScope, store)

        updater.start()
        advanceUntilIdle()

        assertEquals(WidgetStatus.Disconnected, store.written?.status)
    }

    @Test
    fun `el estado persistido guarda la hora del reloj`() = runTest {
        val store = FakeWidgetStateStore()
        val states = MutableStateFlow(desconectado)
        val updater = updater(states, backgroundScope, store)

        updater.start()
        advanceUntilIdle()

        assertEquals(456L, store.written?.updatedAtEpochMs ?: 0L)
    }

    @Test
    fun `refreshNow fuerza un render explicito`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        val updater = updater(states, backgroundScope) { renders++ }

        updater.start()
        advanceUntilIdle()
        updater.refreshNow()
        advanceUntilIdle()

        assertEquals(2, renders)
    }

    private fun updater(
        states: MutableStateFlow<QuickConnectState>,
        scope: CoroutineScope,
        store: WidgetStateStore = FakeWidgetStateStore(),
        refreshTile: () -> Unit = {},
        clock: () -> Long = { 456L },
        render: (QuickConnectState) -> Unit = {},
    ): WidgetUpdater = WidgetUpdater(
        scope = scope,
        states = states,
        renderWidget = render,
        refreshTile = refreshTile,
        store = store,
        clock = clock,
    )

    private class FakeWidgetStateStore(
        initial: PersistedWidgetState? = null,
    ) : WidgetStateStore {
        var written: PersistedWidgetState? = initial

        override fun read(): PersistedWidgetState? = written

        override fun write(state: PersistedWidgetState) {
            written = state
        }
    }

    private companion object {
        val casa: SavedConfig = SavedConfig("a", "Casa")
        val desconectado: QuickConnectState = QuickConnectState(
            tunnel = TunnelState.Disconnected,
            selectedConfig = casa,
        )
        val conectado: QuickConnectState = QuickConnectState(
            tunnel = TunnelState.Connected("a"),
            selectedConfig = casa,
        )
    }
}
