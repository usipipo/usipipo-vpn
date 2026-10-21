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

private const val TWENTY_FOUR_H_MS = 24 * 60 * 60 * 1000L

class CountryRepositoryTest {

    @Test
    fun `cache hit misma ip y menos de 24 h no llama a la api`() = runTest {
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

        // Se guardó en el almacén
        val saved = store.configs.value.single { it.id == "c1" }
        assertEquals("5.6.7.8", saved.countryIp)
        assertEquals("FR", saved.countryCode)
        assertEquals("France", saved.countryName)
        assertEquals(now, saved.countryFetchedAt)
    }

    @Test
    fun `cache expirado mas de 24 h llama a la api`() = runTest {
        val now = System.currentTimeMillis()
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(
                id = "c1", name = "x",
                countryIp = "1.2.3.4", countryCode = "DE", countryName = "Germany",
                countryFetchedAt = now - (TWENTY_FOUR_H_MS + 60_000), // hace 24 h + 1 min
            ),
        ))
        var httpCalled = false
        val http = FakeHttp { httpCalled = true; null }
        val repo = CountryRepository(store, http)

        val result = repo.countryFor("c1", "1.2.3.4")

        assertTrue(httpCalled)
        assertTrue(result.error)
        assertEquals("1.2.3.4", result.ip)
    }

    @Test
    fun `api falla retorna error true y no guarda nada nuevo`() = runTest {
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(id = "c1", name = "x"),
        ))
        val http = FakeHttp { throw RuntimeException("red caida") }
        val repo = CountryRepository(store, http)

        val result = repo.countryFor("c1", "1.2.3.4")

        assertTrue(result.error)
        assertEquals("1.2.3.4", result.ip)
        assertNull(result.countryCode)
        // No se guardó nada: el perfil sigue sin countryIp
        assertNull(store.configs.value.single { it.id == "c1" }.countryIp)
    }

    @Test
    fun `config sin datos de pais consulta la api`() = runTest {
        val store = FakeCountryStoreMutable(configs = listOf(
            SavedConfig(id = "c1", name = "x"),
        ))
        var httpCalledIp: String? = null
        val http = FakeHttp { ip ->
            httpCalledIp = ip
            CountryInfo(ip = ip, countryCode = "JP", countryName = "Japan", fetchedAt = System.currentTimeMillis(), error = false)
        }
        val repo = CountryRepository(store, http)

        val result = repo.countryFor("c1", "9.9.9.9")

        assertEquals("9.9.9.9", httpCalledIp)
        assertEquals("JP", result.countryCode)
        assertEquals("Japan", result.countryName)
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
