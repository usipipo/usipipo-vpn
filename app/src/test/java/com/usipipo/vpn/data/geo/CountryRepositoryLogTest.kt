package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.diagnostics.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CountryRepositoryLogTest {

    private val mensajes = mutableListOf<String>()

    @Before
    fun setUp() {
        AppLog.mirror = { _, _, message -> mensajes += message }
    }

    @After
    fun tearDown() {
        AppLog.mirror = null
    }

    @Test
    fun `el registro de un fallo no incluye la ip`() = runTest {
        val repo = CountryRepository(FakeStore(), FakeHttp { throw java.net.SocketTimeoutException("fallo con 1.2.3.4") })

        repo.countryFor("c1", "1.2.3.4")

        assertTrue(mensajes.isNotEmpty())
        assertFalse(mensajes.any { it.contains("1.2.3.4") })
    }

    @Test
    fun `el registro de un fallo no incluye el token`() = runTest {
        val repo = CountryRepository(
            FakeStore(),
            FakeHttp { throw GeoHttpException("IPinfo Lite", 500) },
        )

        repo.countryFor("c1", "1.2.3.4")

        assertTrue(mensajes.isNotEmpty())
        assertFalse(mensajes.any { it.contains("token", ignoreCase = true) })
    }

    @Test
    fun `el registro de una ip invalida no incluye la ip`() = runTest {
        val repo = CountryRepository(FakeStore(), FakeHttp { throw AssertionError("no debe consultar") })

        repo.countryFor("c1", "165.140")

        assertTrue(mensajes.isNotEmpty())
        assertFalse(mensajes.any { it.contains("165.140") })
        assertTrue(mensajes.any { it.contains("no es una IPv4 válida") })
    }

    @Test
    fun `el registro del fallo indica el tipo de fallo`() = runTest {
        val repo = CountryRepository(FakeStore(), FakeHttp { throw java.net.SocketTimeoutException("timeout") })

        repo.countryFor("c1", "1.2.3.4")

        assertTrue(mensajes.any { it.contains("SocketTimeoutException") })
    }

    @Test
    fun `el registro del fallo usa el nivel de error`() = runTest {
        val niveles = mutableListOf<LogLevel>()
        AppLog.mirror = { level, _, _ -> niveles += level }
        val repo = CountryRepository(FakeStore(), FakeHttp { throw java.net.SocketTimeoutException("timeout") })

        repo.countryFor("c1", "1.2.3.4")

        assertTrue(niveles.contains(LogLevel.ERROR))
    }

    private class FakeStore : ConfigStore {
        private val _configs = MutableStateFlow(listOf(SavedConfig(id = "c1", name = "x")))
        private val _lastUsedId = MutableStateFlow<String?>(null)

        override val configs: StateFlow<List<SavedConfig>> = _configs
        override val lastUsedId: StateFlow<String?> = _lastUsedId

        override suspend fun add(name: String, configText: String): Result<SavedConfig>
            = throw UnsupportedOperationException()
        override suspend fun readText(id: String): String? = null
        override suspend fun remove(id: String) {}
        override suspend fun setLastUsed(id: String) {}
        override suspend fun setFavorite(id: String, favorite: Boolean) {}
        override suspend fun rename(id: String, name: String): Result<SavedConfig>
            = throw UnsupportedOperationException()

        override suspend fun updateCountry(id: String, countryInfo: CountryInfo) {
            _configs.value = _configs.value.map { c ->
                if (c.id == id) {
                    c.copy(
                        countryCode = countryInfo.countryCode,
                        countryName = countryInfo.countryName,
                        countryIp = countryInfo.ip,
                        countryFetchedAt = countryInfo.fetchedAt,
                    )
                } else c
            }
        }

        override suspend fun clearGeoCache() {
            _configs.value = _configs.value.map { c ->
                c.copy(
                    countryCode = null,
                    countryName = null,
                    countryIp = null,
                    countryFetchedAt = null,
                )
            }
        }
    }

    private class FakeHttp(private val call: (String) -> CountryInfo?) : CountryHttpClient {
        override suspend fun fetch(ip: String): CountryInfo {
            return call(ip) ?: CountryInfo(ip = ip, countryCode = null, countryName = null, fetchedAt = 0, error = true)
        }
    }
}
