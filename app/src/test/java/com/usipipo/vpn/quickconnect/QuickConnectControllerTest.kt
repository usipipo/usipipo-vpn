package com.usipipo.vpn.quickconnect

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.ui.home.FakeConfigStore
import com.usipipo.vpn.ui.home.FakeTunnelManager
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickConnectControllerTest {

    @Test
    fun `conectar usa el conector y devuelve ConnectRequested cuando arranca`() = runTest {
        val store = FakeConfigStore(listOf(casa, trabajo))
        val tunnel = FakeTunnelManager()
        val connector = FakeTunnelConnector(result = true)
        val controller = controller(store, tunnel, permissionChecker = { true }, connector = connector)

        val action = controller.toggle()

        assertEquals(QuickConnectAction.ConnectRequested("a"), action)
        assertEquals(listOf("a"), connector.requestedIds)
        assertTrue(tunnel.connectCalls.isEmpty())
    }

    @Test
    fun `toggle devuelve BackgroundStartDenied si el conector deniega el arranque`() = runTest {
        val store = FakeConfigStore(listOf(casa, trabajo))
        val tunnel = FakeTunnelManager()
        val connector = FakeTunnelConnector(result = false)
        val controller = controller(store, tunnel, permissionChecker = { true }, connector = connector)

        assertEquals(QuickConnectAction.BackgroundStartDenied, controller.toggle())
        assertTrue(tunnel.connectCalls.isEmpty())
    }

    @Test
    fun `conectar se ignora si falta el permiso vpn sin tocar el conector`() = runTest {
        val connector = FakeTunnelConnector()
        val controller = controller(FakeConfigStore(listOf(casa)), FakeTunnelManager(), permissionChecker = { false }, connector = connector)

        assertEquals(QuickConnectAction.VpnPermissionRequired, controller.toggle())
        assertTrue(connector.requestedIds.isEmpty())
    }

    @Test
    fun `conectar usa el perfil seleccionado y su texto`() = runTest {
        val store = FakeConfigStore(listOf(casa, trabajo))
        val tunnel = FakeTunnelManager()
        val connector = FakeTunnelConnector()
        val controller = controller(store, tunnel, permissionChecker = { true }, connector = connector)

        val action = controller.toggle()

        assertEquals(QuickConnectAction.ConnectRequested("a"), action)
        assertEquals(listOf("a"), connector.requestedIds)
        assertTrue(tunnel.connectCalls.isEmpty())
        assertEquals(casa, controller.state.value.selectedConfig)
    }

    @Test
    fun `conectar se ignora si falta el permiso vpn`() = runTest {
        val tunnel = FakeTunnelManager()
        val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { false })

        val action = controller.toggle()

        assertEquals(QuickConnectAction.VpnPermissionRequired, action)
        assertTrue(tunnel.connectCalls.isEmpty())
    }

    @Test
    fun `sin perfiles abre importar aunque falte el permiso`() = runTest {
        val connector = FakeTunnelConnector()
        val controller = controller(FakeConfigStore(), FakeTunnelManager(), permissionChecker = { false }, connector = connector)

        assertEquals(QuickConnectAction.OpenImporter, controller.toggle())
        assertTrue(connector.requestedIds.isEmpty())
    }

    @Test
    fun `conectando un toque desconecta y no vuelve a conectar`() = runTest {
        val tunnel = FakeTunnelManager().apply { emit(TunnelState.Connecting("a")) }
        val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { true })

        assertEquals(QuickConnectAction.DisconnectRequested, controller.toggle())
        assertEquals(1, tunnel.disconnectCalls)
        assertTrue(tunnel.connectCalls.isEmpty())
    }

    @Test
    fun `conectado y sin respuesta desconectan`() = runTest {
        listOf(TunnelState.Connected("a"), TunnelState.NoResponse("a")).forEach { estado ->
            val tunnel = FakeTunnelManager().apply { emit(estado) }
            val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { true })

            assertEquals(QuickConnectAction.DisconnectRequested, controller.toggle())
        }
    }

    @Test
    fun `fallo permite reintentar la conexion`() = runTest {
        val tunnel = FakeTunnelManager().apply { emit(TunnelState.Failed("a")) }
        val connector = FakeTunnelConnector()
        val controller = controller(FakeConfigStore(listOf(casa)), tunnel, permissionChecker = { true }, connector = connector)

        assertEquals(QuickConnectAction.ConnectRequested("a"), controller.toggle())
        assertEquals(listOf("a"), connector.requestedIds)
        assertTrue(tunnel.connectCalls.isEmpty())
    }

    @Test
    fun `un perfil ilegible publica fallo y no pasa el conf al tunel`() = runTest {
        val store = FakeConfigStore(listOf(casa), unreadableIds = setOf("a"))
        val tunnel = FakeTunnelManager()
        var permission = true
        val connector = FakeTunnelConnector()
        val controller = controller(store, tunnel, permissionChecker = { permission }, connector = connector)

        assertEquals(QuickConnectAction.ConfigUnreadable("a"), controller.toggle())
        assertTrue(tunnel.connectCalls.isEmpty())
        assertTrue(connector.requestedIds.isEmpty())
        assertEquals(
            QuickConnectFailure.ConfigUnreadable("a"),
            controller.state.value.failure,
        )

        permission = false
        assertEquals(QuickConnectAction.VpnPermissionRequired, controller.toggle())
        assertNull(controller.state.value.failure)
    }

    @Test
    fun `si se borra la ultima usada se conecta el siguiente perfil`() = runTest {
        val store = FakeConfigStore(listOf(casa, trabajo))
        store.setLastUsed("b")
        store.remove("b")
        val controller = controller(store, FakeTunnelManager(), permissionChecker = { true })

        assertEquals(QuickConnectAction.ConnectRequested("a"), controller.toggle())
        assertEquals("a", controller.state.value.selectedConfig?.id)
    }

    private fun TestScope.controller(
        store: FakeConfigStore,
        tunnel: FakeTunnelManager,
        permissionChecker: () -> Boolean,
        connector: TunnelConnector = FakeTunnelConnector(),
    ): QuickConnectController = QuickConnectController(
        store = store,
        tunnelManager = tunnel,
        permissionChecker = VpnPermissionChecker { permissionChecker() },
        connector = connector,
        scope = backgroundScope,
    )

    private companion object {
        val casa = SavedConfig(id = "a", name = "Casa")
        val trabajo = SavedConfig(id = "b", name = "Trabajo")
    }
}
