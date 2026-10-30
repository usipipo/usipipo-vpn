package com.usipipo.vpn.widget

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.quickconnect.QuickConnectState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El actualizador lanza su colector en el scope que recibe. Con un dispatcher eager el
 * colector arranca y se reanuda de inmediato, así que el test comprueba el efecto sin
 * depender de que el planificador ejecute el trabajo en segundo plano.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetUpdaterTest {

    @Test
    fun `un estado repetido no se dibuja dos veces`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        val updater = updater(states) { renders++ }

        updater.start()
        states.value = desconectado

        assertEquals(1, renders)
    }

    @Test
    fun `un cambio dibuja y actualiza el mosaico`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        var tileRefreshes = 0
        val updater = updater(states, refreshTile = { tileRefreshes++ }) { renders++ }

        updater.start()
        states.value = conectado

        assertEquals(2, renders)
        assertEquals(2, tileRefreshes)
    }

    @Test
    fun `arrancar dos veces no duplica la coleccion`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        val updater = updater(states) { renders++ }

        updater.start()
        updater.start()

        assertEquals(1, renders)
    }

    @Test
    fun `el estado vivo desconectado invalida un estado activo persistido`() = runTest {
        val store = FakeWidgetStateStore(PersistedWidgetState(WidgetStatus.Connected, 1L))
        val states = MutableStateFlow(desconectado)
        val updater = updater(states, store = store)

        updater.start()

        assertEquals(WidgetStatus.Disconnected, store.written?.status)
    }

    @Test
    fun `el estado persistido guarda la hora del reloj`() = runTest {
        val store = FakeWidgetStateStore()
        val states = MutableStateFlow(desconectado)
        val updater = updater(states, store = store)

        updater.start()

        assertEquals(456L, store.written?.updatedAtEpochMs ?: 0L)
    }

    @Test
    fun `un fallo al refrescar el mosaico no impide guardar el estado`() = runTest {
        val store = FakeWidgetStateStore()
        val states = MutableStateFlow(desconectado)
        val updater = updater(states, store = store, refreshTile = { error("el mosaico no responde") })

        updater.start()

        assertEquals(WidgetStatus.Disconnected, store.written?.status)
    }

    @Test
    fun `refreshNow fuerza un render explicito`() = runTest {
        val states = MutableStateFlow(desconectado)
        var renders = 0
        val updater = updater(states) { renders++ }

        updater.start()
        updater.refreshNow()

        assertEquals(2, renders)
    }

    private fun TestScope.updater(
        states: MutableStateFlow<QuickConnectState>,
        store: WidgetStateStore = FakeWidgetStateStore(),
        refreshTile: () -> Unit = {},
        clock: () -> Long = { 456L },
        render: (QuickConnectState) -> Unit = {},
    ): WidgetUpdater = WidgetUpdater(
        scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
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
