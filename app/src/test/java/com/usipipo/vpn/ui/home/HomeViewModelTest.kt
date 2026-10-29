package com.usipipo.vpn.ui.home

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.quickconnect.FakeTunnelConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val casa = SavedConfig("a", "Casa")
    private val trabajo = SavedConfig("b", "Trabajo")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin ultima usada selecciona la primera configuracion`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals("a", vm.uiState.value.selectedId)
    }

    @Test
    fun `seleccionar otra configuracion la recuerda como ultima usada`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.select("b")

        assertEquals("b", vm.uiState.value.selectedId)
    }

    @Test
    fun `conectar pasa al tunel el texto de la configuracion seleccionada`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), tunnel, FakeTunnelConnector(result = false))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.select("b")
        vm.connectSelected()

        assertEquals(listOf("b" to "conf-de-b"), tunnel.connectCalls)
        assertEquals(TunnelState.Connecting("b"), vm.uiState.value.tunnel)
    }

    @Test
    fun `conectar sin configuraciones no hace nada`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = HomeViewModel(FakeConfigStore(), tunnel, FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.connectSelected()

        assertEquals(emptyList<Pair<String, String>>(), tunnel.connectCalls)
    }

    @Test
    fun `borrar la configuracion conectada desconecta antes de eliminarla`() = runTest {
        val tunnel = FakeTunnelManager()
        val store = FakeConfigStore(listOf(casa, trabajo))
        val vm = HomeViewModel(store, tunnel, FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        tunnel.emit(TunnelState.Connected("a"))

        vm.delete("a")

        assertEquals(1, tunnel.disconnectCalls)
        assertEquals(listOf(trabajo), store.configs.value)
    }

    @Test
    fun `borrar una configuracion distinta a la conectada no desconecta`() = runTest {
        val tunnel = FakeTunnelManager()
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), tunnel, FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        tunnel.emit(TunnelState.Connected("a"))

        vm.delete("b")

        assertEquals(0, tunnel.disconnectCalls)
    }

    @Test
    fun `permiso denegado muestra el mensaje y se puede descartar`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa)), FakeTunnelManager(), FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onPermissionDenied()
        assertEquals(HomeMessage.PermissionDenied, vm.uiState.value.message)

        vm.dismissMessage()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `conectar pide la conexion al conector`() = runTest {
        val tunnel = FakeTunnelManager()
        val connector = FakeTunnelConnector(result = true)
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), tunnel, connector)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.select("b")
        vm.connectSelected()

        assertEquals(listOf("b"), connector.requestedIds)
        assertTrue(tunnel.connectCalls.isEmpty())
    }

    @Test
    fun `conectar con conector denegado conecta directamente`() = runTest {
        val tunnel = FakeTunnelManager()
        val connector = FakeTunnelConnector(result = false)
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), tunnel, connector)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.select("b")
        vm.connectSelected()

        assertEquals(listOf("b" to "conf-de-b"), tunnel.connectCalls)
    }

    @Test
    fun `conectar con perfil ilegible no conecta y muestra ConfigUnreadable`() = runTest {
        val tunnel = FakeTunnelManager()
        val connector = FakeTunnelConnector(result = true)
        val store = FakeConfigStore(listOf(casa), unreadableIds = setOf("a"))
        val vm = HomeViewModel(store, tunnel, connector)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.select("a")
        vm.connectSelected()

        assertEquals(HomeMessage.ConfigUnreadable, vm.uiState.value.message)
        assertTrue(tunnel.connectCalls.isEmpty())
        assertTrue(connector.requestedIds.isEmpty())
    }
}
