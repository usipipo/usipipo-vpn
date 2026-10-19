package com.usipipo.vpn.ui.details

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelCounters
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.ui.home.FakeConfigStore
import com.usipipo.vpn.ui.home.FakeTunnelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val casa = SavedConfig("a", "Casa")
    private val conf = "[Peer]\nEndpoint = vpn.example.com:51820\nAllowedIPs = 0.0.0.0/0, ::/0\n"

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(tunnel: FakeTunnelManager): StatsViewModel {
        val vm = StatsViewModel(FakeConfigStore(listOf(casa), mapOf("a" to conf)), tunnel)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.ui.collect {} }
        return vm
    }

    @Test
    fun `sin conexion no hay datos`() = runTest {
        val vm = viewModel(FakeTunnelManager())

        assertEquals(StatsUiState(), vm.ui.value)
    }

    @Test
    fun `al conectar muestra el endpoint y el IPv6 del conf`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)

        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))

        assertEquals("vpn.example.com:51820", vm.ui.value.endpoint)
        assertTrue(vm.ui.value.routesIpv6)
    }

    @Test
    fun `calcula la velocidad con los contadores`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)
        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))

        tunnel.counterSamples.tryEmit(TunnelCounters(0L, 0L, 0L))
        tunnel.counterSamples.tryEmit(TunnelCounters(1_000_000L, 250_000L, 1_000L))

        assertEquals(1_000_000L, vm.ui.value.downBps)
        assertEquals(250_000L, vm.ui.value.upBps)
        assertEquals(listOf(0L, 1_000_000L), vm.ui.value.downHistory)
    }

    @Test
    fun `el historial no pasa del tamano maximo`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)
        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))

        repeat(StatsViewModel.HISTORY_SIZE + 10) { i ->
            tunnel.counterSamples.tryEmit(TunnelCounters(i * 1_000L, 0L, i * 1_000L))
        }

        assertEquals(StatsViewModel.HISTORY_SIZE, vm.ui.value.downHistory.size)
    }

    @Test
    fun `al desconectar vuelve al estado vacio`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = viewModel(tunnel)
        tunnel.emit(TunnelState.Connected("a", sinceMs = 1L))
        tunnel.counterSamples.tryEmit(TunnelCounters(0L, 0L, 0L))

        tunnel.emit(TunnelState.Disconnected)

        assertEquals(StatsUiState(), vm.ui.value)
    }
}
