package com.usipipo.vpn.ui.details

import com.usipipo.vpn.data.geo.CountryHttpClient
import com.usipipo.vpn.data.geo.CountryRepository
import com.usipipo.vpn.data.geo.PingRepository
import com.usipipo.vpn.data.geo.PingRunner
import com.usipipo.vpn.data.geo.ReachabilityChecker
import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.ui.home.FakeConfigStore
import com.usipipo.vpn.ui.home.FakeTunnelManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountryPingViewModelTest {

    private val conf = """
        [Interface]
        Address = 10.5.0.2/24
        [Peer]
        Endpoint = 1.2.3.4:51820
    """.trimIndent()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pingRepo() = PingRepository(
        PingRunner { null },
        ReachabilityChecker { _, _ -> false },
    )

    @Test
    fun `al conectar consulta pais una vez y emite resultado`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf("c1" to conf),
        )
        val tunnelManager = FakeTunnelManager()
        var httpCalled = false
        val countryRepo = CountryRepository(store, http = FakeHttp { ip ->
            httpCalled = true
            CountryInfo(ip = ip, countryCode = "DE", countryName = "Germany", fetchedAt = 1000L, error = false)
        })

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo())
        vm.setAppForeground(true)

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))

        val state = vm.ui.first { it.countryInfo != null }
        assertTrue(httpCalled)
        assertEquals("DE", state.countryInfo?.countryCode)
        assertEquals("Germany", state.countryInfo?.countryName)
        assertFalse(state.countryInfo?.error ?: true)
    }

    @Test
    fun `al conectar sin endpoint no consulta pais ni rompe`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf("c1" to "[Interface]\nAddress = 10.5.0.2/24\n"),
        )
        val tunnelManager = FakeTunnelManager()
        var httpCalled = false
        val countryRepo = CountryRepository(store, http = FakeHttp { httpCalled = true; null })

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo())
        vm.setAppForeground(true)

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))

        val state = vm.ui.first { it.countryInfo == null && !it.isLoadingCountry }
        assertFalse(httpCalled)
        assertNull(state.countryInfo)
    }

    @Test
    fun `con la app en segundo plano no consulta el pais`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf("c1" to conf),
        )
        val tunnelManager = FakeTunnelManager()
        var httpCalled = false
        val countryRepo = CountryRepository(store, http = FakeHttp { httpCalled = true; null })

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo())

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))
        yield()

        assertFalse(httpCalled)
        assertNull(vm.ui.value.countryInfo)
    }

    @Test
    fun `al volver a primer plano consulta el pais del tunel ya conectado`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf("c1" to conf),
        )
        val tunnelManager = FakeTunnelManager()
        var httpCalled = false
        val countryRepo = CountryRepository(store, http = FakeHttp { ip ->
            httpCalled = true
            CountryInfo(ip = ip, countryCode = "DE", countryName = "Germany", fetchedAt = 1000L, error = false)
        })

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo())

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))
        yield()
        assertFalse(httpCalled)

        vm.setAppForeground(true)

        val state = vm.ui.first { it.countryInfo != null }
        assertTrue(httpCalled)
        assertEquals("DE", state.countryInfo?.countryCode)
    }

    @Test
    fun `al desconectar se cancela la peticion de pais en vuelo`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf("c1" to conf),
        )
        val tunnelManager = FakeTunnelManager()
        // Señal explícita de cancelación. La petición vive en Dispatchers.IO, fuera
        // del scheduler virtual del test: el test la espera, no la sondea.
        val cancelada = CompletableDeferred<Unit>()
        val countryRepo = CountryRepository(store, http = FakeHttp {
            try {
                awaitCancellation()
            } finally {
                cancelada.complete(Unit)
            }
        })

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo())
        vm.setAppForeground(true)

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))
        vm.ui.first { it.isLoadingCountry }

        tunnelManager.emit(TunnelState.Disconnected)

        // Espera con tiempo real: con el reloj virtual de runTest el timeout vencería
        // al instante mientras el hilo de IO todavía no ha publicado la señal.
        val llego = withContext(Dispatchers.Default) {
            withTimeoutOrNull(1_000) { cancelada.await() }
        }
        assertNotNull(llego, "la petición de país no se canceló al desconectar")
        assertNull(vm.ui.value.countryInfo)
        assertFalse(vm.ui.value.isLoadingCountry)
    }

    @Test
    fun `el ping solo corre con detalles visible`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf("c1" to conf),
        )
        val tunnelManager = FakeTunnelManager()
        val countryRepo = CountryRepository(store, http = FakeHttp {
            CountryInfo(ip = "1.2.3.4", countryCode = "DE", countryName = "Germany", fetchedAt = 1000L, error = false)
        })
        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo())
        vm.setAppForeground(true)

        // Detalles oculta: hay país (se consulta al conectar) pero no hay ping.
        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))
        val conPais = vm.ui.first { it.countryInfo != null }
        assertNull(conPais.pingResult)

        // Detalles visible: arranca el ping.
        vm.setDetailsVisible(true)
        val conPing = vm.ui.first { it.pingResult != null }
        assertNotNull(conPing.pingResult)

        // Detalles oculta: el ping se detiene.
        vm.setDetailsVisible(false)
        assertNull(vm.ui.value.pingResult)
    }

    @Test
    fun `al desconectar se limpia el ping`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf("c1" to conf),
        )
        val tunnelManager = FakeTunnelManager()
        val countryRepo = CountryRepository(store, http = FakeHttp {
            CountryInfo(ip = "1.2.3.4", countryCode = "DE", countryName = "Germany", fetchedAt = 1000L, error = false)
        })
        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo())
        vm.setAppForeground(true)
        vm.setDetailsVisible(true)

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))
        vm.ui.first { it.pingResult != null }

        tunnelManager.emit(TunnelState.Disconnected)
        assertNull(vm.ui.value.pingResult)
    }

    private class FakeHttp(private val call: suspend (String) -> CountryInfo?) : CountryHttpClient {
        override suspend fun fetch(ip: String): CountryInfo =
            call(ip) ?: CountryInfo(ip = ip, countryCode = null, countryName = null, fetchedAt = 0, error = true)
    }
}
