package com.usipipo.vpn.ui.details

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.data.geo.CountryHttpClient
import com.usipipo.vpn.data.geo.CountryRepository
import com.usipipo.vpn.data.geo.PingRepository
import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.PingResult
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.tunnel.TunnelManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.usipipo.vpn.ui.home.FakeConfigStore
import com.usipipo.vpn.ui.home.FakeTunnelManager

class CountryPingViewModelTest {

    @Test
    fun `al conectar consulta pais una vez y emite resultado`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf(
                "c1" to """
                    [Interface]
                    Address = 10.5.0.2/24
                    [Peer]
                    Endpoint = 1.2.3.4:51820
                """.trimIndent()
            ),
        )
        val tunnelManager = FakeTunnelManager()
        var httpCalled = false
        val countryRepo = CountryRepository(store, http = FakeHttp { ip ->
            httpCalled = true
            CountryInfo(ip = ip, countryCode = "DE", countryName = "Germany", fetchedAt = 1000L, error = false)
        })
        val pingRepo = PingRepository(
            pingRunner = { null },
            reachabilityChecker = { _, _ -> false },
        )

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo)

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))

        val first = vm.ui.first()
        assertTrue(httpCalled)
        assertEquals("DE", first.countryInfo?.countryCode)
        assertEquals("Germany", first.countryInfo?.countryName)
        assertFalse(first.countryInfo!!.error)
        assertNull(first.pingResult) // ping aún no se ha emitido (delay 5s)
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
        val pingRepo = PingRepository(
            pingRunner = { null },
            reachabilityChecker = { _, _ -> false },
        )

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo)

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))

        val first = vm.ui.first()
        assertFalse(httpCalled)
        assertNull(first.countryInfo)
        assertFalse(first.isLoadingCountry)
    }

    @Test
    fun `al desconectar se limpia el ping`() = runTest {
        val store = FakeConfigStore(
            configs = listOf(SavedConfig("c1", "Mi VPN")),
            texts = mapOf(
                "c1" to """
                    [Interface]
                    Address = 10.5.0.2/24
                    [Peer]
                    Endpoint = 1.2.3.4:51820
                """.trimIndent()
            ),
        )
        val tunnelManager = FakeTunnelManager()
        val countryRepo = CountryRepository(store, http = FakeHttp {
            CountryInfo(ip = "1.2.3.4", countryCode = "DE", countryName = "Germany", fetchedAt = 1000L, error = false)
        })
        val pingRepo = PingRepository(
            pingRunner = { null },
            reachabilityChecker = { _, _ -> false },
        )

        val vm = CountryPingViewModel(store, tunnelManager, countryRepo, pingRepo)

        tunnelManager.emit(TunnelState.Connected("c1", sinceMs = 100L))
        vm.ui.first() // espera a que se emita el país

        tunnelManager.emit(TunnelState.Disconnected)
        val afterDisconnect = vm.ui.first()
        assertNull(afterDisconnect.pingResult)
    }

    private class FakeHttp(private val call: (String) -> CountryInfo) : CountryHttpClient {
        override suspend fun fetch(ip: String): CountryInfo = call(ip)
    }

    private class FakePingRepository : PingRepository(
        pingRunner = { null },
        reachabilityChecker = { _, _ -> false },
    )
}
