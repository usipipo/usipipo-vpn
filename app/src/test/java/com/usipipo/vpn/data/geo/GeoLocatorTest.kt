package com.usipipo.vpn.data.geo

import com.usipipo.vpn.domain.CountryInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoLocatorTest {

    @Test
    fun `si el primario responde no consulta el respaldo`() = runTest {
        val primary = FakeClient { CountryInfo("8.8.8.8", "DE", "Germany", 1L) }
        val fallback = FakeClient { CountryInfo("8.8.8.8", "FR", "France", 1L) }

        val info = GeoLocator(primary, fallback).fetch("8.8.8.8")

        assertEquals("DE", info.countryCode)
        assertEquals(0, fallback.calls)
    }

    @Test
    fun `si el primario no trae pais consulta el respaldo`() = runTest {
        val primary = FakeClient { CountryInfo("8.8.8.8", null, null, 1L) }
        val fallback = FakeClient { CountryInfo("8.8.8.8", "FR", "France", 1L) }

        val info = GeoLocator(primary, fallback).fetch("8.8.8.8")

        assertEquals("FR", info.countryCode)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun `si el primario falla por http consulta el respaldo`() = runTest {
        val primary = FakeClient { throw GeoHttpException("IPinfo Lite", 500) }
        val fallback = FakeClient { CountryInfo("8.8.8.8", "FR", "France", 1L) }

        val info = GeoLocator(primary, fallback).fetch("8.8.8.8")

        assertEquals("FR", info.countryCode)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun `si falta el token del primario consulta el respaldo`() = runTest {
        val primary = FakeClient { throw MissingTokenException() }
        val fallback = FakeClient { CountryInfo("8.8.8.8", "FR", "France", 1L) }

        val info = GeoLocator(primary, fallback).fetch("8.8.8.8")

        assertEquals("FR", info.countryCode)
        assertEquals(1, fallback.calls)
    }

    @Test
    fun `si el primario esta limitado por tasa no consulta el respaldo`() = runTest {
        val primary = FakeClient { throw RateLimitedException(60L) }
        val fallback = FakeClient { CountryInfo("8.8.8.8", "FR", "France", 1L) }

        val error = runCatching { GeoLocator(primary, fallback).fetch("8.8.8.8") }.exceptionOrNull()

        assertTrue(error is RateLimitedException)
        assertEquals(0, fallback.calls)
    }

    @Test
    fun `si los dos proveedores fallan informa de los dos fallos`() = runTest {
        val primary = FakeClient { throw GeoHttpException("IPinfo Lite", 500) }
        val fallback = FakeClient { throw java.net.SocketTimeoutException("timeout con 8.8.8.8") }

        val error = runCatching { GeoLocator(primary, fallback).fetch("8.8.8.8") }.exceptionOrNull()

        assertTrue(error is GeoProvidersException)
        val message = error?.message.orEmpty()
        assertTrue(message.contains("IPinfo Lite"))
        assertFalse(message.contains("8.8.8.8"))
    }

    @Test
    fun `si se cancela la peticion no consulta el respaldo`() = runTest {
        val primary = FakeClient { throw CancellationException("cancelada") }
        val fallback = FakeClient { CountryInfo("8.8.8.8", "FR", "France", 1L) }

        val error = runCatching { GeoLocator(primary, fallback).fetch("8.8.8.8") }.exceptionOrNull()

        assertTrue(error is CancellationException)
        assertEquals(0, fallback.calls)
    }

    private class FakeClient(private val call: () -> CountryInfo) : CountryHttpClient {
        var calls: Int = 0

        override suspend fun fetch(ip: String): CountryInfo {
            calls++
            return call()
        }
    }
}
