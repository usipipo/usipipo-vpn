package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.SavedConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

class CountryRepositoryTest {

    @Test
    fun `cache hit misma ip y menos de 30 dias no llama a la api`() = runTest {
        val now = System.currentTimeMillis()
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(
                id = "c1", name = "x",
                countryIp = "1.2.3.4", countryCode = "DE", countryName = "Germany",
                countryFetchedAt = now - 3_600_000, // hace 1 hora
            ),
        ))
        var httpCalled = false
        val http = FakeHttp { httpCalled = true; throw AssertionError("no debe llamar a la API") }
        val repo = CountryRepository(store, http)

        val result = repo.countryFor("c1", "1.2.3.4")

        assertFalse(httpCalled)
        assertEquals("DE", result.countryCode)
        assertEquals("Germany", result.countryName)
        assertFalse(result.error)
        assertEquals(now - 3_600_000, result.fetchedAt)
    }

    @Test
    fun `cache miss ip diferente llama a la api y guarda`() = runTest {
        val now = System.currentTimeMillis()
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(
                id = "c1", name = "x",
                countryIp = "1.2.3.4", countryCode = "DE", countryName = "Germany",
                countryFetchedAt = now - 3_600_000,
            ),
        ))
        var httpCalledIp: String? = null
        val http = FakeHttp { ip ->
            httpCalledIp = ip
            CountryInfo(ip = ip, countryCode = "FR", countryName = "France", fetchedAt = now, error = false)
        }
        val repo = CountryRepository(store, http)

        val result = repo.countryFor("c1", "5.6.7.8")

        assertEquals("5.6.7.8", httpCalledIp)
        assertEquals("FR", result.countryCode)
        assertEquals("France", result.countryName)
        assertFalse(result.error)

        val saved = store.configs.value.single { it.id == "c1" }
        assertEquals("5.6.7.8", saved.countryIp)
        assertEquals("FR", saved.countryCode)
        assertEquals("France", saved.countryName)
        assertEquals(now, saved.countryFetchedAt)
    }

    @Test
    fun `cache vigente a los 29 dias no vuelve a consultar`() = runTest {
        val now = System.currentTimeMillis()
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(
                id = "c1", name = "x",
                countryIp = "1.2.3.4", countryCode = "DE", countryName = "Germany",
                countryFetchedAt = now - (THIRTY_DAYS_MS - 60_000),
            ),
        ))
        var httpCalled = false
        val http = FakeHttp { httpCalled = true; null }

        val result = CountryRepository(store, http).countryFor("c1", "1.2.3.4")

        assertFalse(httpCalled)
        assertEquals("DE", result.countryCode)
    }

    @Test
    fun `cache expirado a los 31 dias vuelve a consultar`() = runTest {
        val now = System.currentTimeMillis()
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(
                id = "c1", name = "x",
                countryIp = "1.2.3.4", countryCode = "DE", countryName = "Germany",
                countryFetchedAt = now - (THIRTY_DAYS_MS + 60_000),
            ),
        ))
        var httpCalled = false
        val http = FakeHttp { httpCalled = true; null }

        val result = CountryRepository(store, http).countryFor("c1", "1.2.3.4")

        assertTrue(httpCalled)
        assertTrue(result.error)
        assertEquals("1.2.3.4", result.ip)
    }

    @Test
    fun `una ip invalida no hace ninguna peticion`() = runTest {
        val store = FakeCountryStoreMutable(configs = listOf(SavedConfig(id = "c1", name = "x")))
        var httpCalls = 0
        val http = FakeHttp { httpCalls++; null }

        val result = CountryRepository(store, http).countryFor("c1", "165.140")

        assertEquals(0, httpCalls)
        assertTrue(result.error)
        assertNull(result.countryCode)
    }

    @Test
    fun `un fallo reintenta una vez y devuelve error`() = runTest {
        val store = FakeCountryStoreMutable(configs = listOf(SavedConfig(id = "c1", name = "x")))
        var httpCalls = 0
        val http = FakeHttp { httpCalls++; throw java.net.SocketTimeoutException("timeout") }

        val result = CountryRepository(store, http).countryFor("c1", "1.2.3.4")

        assertEquals(2, httpCalls)
        assertTrue(result.error)
        assertNull(store.configs.value.single { it.id == "c1" }.countryIp)
    }

    @Test
    fun `tras dos fallos seguidos el perfil entra en enfriamiento`() = runTest {
        val store = FakeCountryStoreMutable(configs = listOf(SavedConfig(id = "c1", name = "x")))
        var httpCalls = 0
        val http = FakeHttp { httpCalls++; throw java.net.SocketTimeoutException("timeout") }
        val repo = CountryRepository(store, http)

        repo.countryFor("c1", "1.2.3.4")
        assertEquals(2, httpCalls)

        repo.countryFor("c1", "1.2.3.4")
        assertEquals(4, httpCalls)

        // Enfriamiento: ya no se insiste hasta que pase el tiempo.
        val third = repo.countryFor("c1", "1.2.3.4")
        assertEquals(4, httpCalls)
        assertTrue(third.error)
    }

    @Test
    fun `un acierto borra el enfriamiento del perfil`() = runTest {
        val clock = FakeClock()
        val store = FakeCountryStoreMutable(configs = listOf(SavedConfig(id = "c1", name = "x")))
        var fail = true
        val http = FakeHttp { ip ->
            if (fail) throw java.net.SocketTimeoutException("timeout")
            CountryInfo(ip = ip, countryCode = "DE", countryName = "Germany", fetchedAt = clock.now, error = false)
        }
        val repo = CountryRepository(store, http, now = { clock.now })

        repo.countryFor("c1", "1.2.3.4")
        repo.countryFor("c1", "1.2.3.4")
        fail = false
        // Más tarde que el enfriamiento del perfil (1 minuto).
        clock.advance(61_000)

        val result = repo.countryFor("c1", "1.2.3.4")

        assertEquals("DE", result.countryCode)
    }

    @Test
    fun `una respuesta sin pais no se cachea`() = runTest {
        val store = FakeCountryStoreMutable(configs = listOf(SavedConfig(id = "c1", name = "x")))
        val http = FakeHttp { ip ->
            CountryInfo(ip = ip, countryCode = null, countryName = null, fetchedAt = 0L, error = false)
        }

        val result = CountryRepository(store, http).countryFor("c1", "1.2.3.4")

        assertFalse(result.error)
        assertNull(result.countryCode)
        assertNull(store.configs.value.single { it.id == "c1" }.countryIp)
    }

    @Test
    fun `un 429 no se reintenta y no vuelve a consultar`() = runTest {
        val store = FakeCountryStoreMutable(configs = listOf(SavedConfig(id = "c1", name = "x")))
        var httpCalls = 0
        val http = FakeHttp { httpCalls++; throw RateLimitedException(60L) }
        val repo = CountryRepository(store, http)

        val first = repo.countryFor("c1", "1.2.3.4")
        assertTrue(first.error)
        assertEquals(1, httpCalls)

        val second = repo.countryFor("c1", "1.2.3.4")
        assertTrue(second.error)
        assertEquals(1, httpCalls)
    }

    @Test
    fun `el cache se renueva si cambia la ip`() = runTest {
        val now = System.currentTimeMillis()
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(
                id = "c1", name = "x",
                countryIp = "1.2.3.4", countryCode = "DE", countryName = "Germany",
                countryFetchedAt = now,
            ),
        ))
        var httpCalledIp: String? = null
        val http = FakeHttp { ip ->
            httpCalledIp = ip
            CountryInfo(ip = ip, countryCode = "FR", countryName = "France", fetchedAt = now, error = false)
        }

        val result = CountryRepository(store, http).countryFor("c1", "9.9.9.9")

        assertEquals("9.9.9.9", httpCalledIp)
        assertEquals("FR", result.countryCode)
    }

    private class FakeClock(var now: Long = 1_000_000L) {
        fun advance(ms: Long) {
            now += ms
        }
    }

    private class FakeCountryStoreMutable(configs: List<SavedConfig>) : ConfigStore {
        private val _configs = MutableStateFlow(configs)
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
            val cfg = _configs.value.firstOrNull { it.id == id } ?: return
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
    }

    private class FakeHttp(private val call: (String) -> CountryInfo?) : CountryHttpClient {
        override suspend fun fetch(ip: String): CountryInfo {
            return call(ip) ?: CountryInfo(ip = ip, countryCode = null, countryName = null, fetchedAt = 0, error = true)
        }
    }
}
