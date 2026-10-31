package com.usipipo.vpn.data.geo

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpwhoIsClientTest {

    @Test
    fun `consulta la url de ipwho_is`() = runTest {
        val transport = FakeTransport { HttpResponse(200, """{"success":true,"country_code":"FR"}""") }

        IpwhoIsClient(transport = transport).fetch("8.8.8.8")

        assertEquals("https://ipwho.is/8.8.8.8", transport.lastUrl)
    }

    @Test
    fun `una respuesta correcta devuelve codigo y nombre del pais`() = runTest {
        val transport = FakeTransport {
            HttpResponse(200, """{"success":true,"country_code":"FR","country":"France"}""")
        }

        val info = IpwhoIsClient(transport = transport).fetch("8.8.8.8")

        assertEquals("FR", info.countryCode)
        assertEquals("France", info.countryName)
        assertFalse(info.error)
    }

    @Test
    fun `una respuesta con success false no trae pais pero no es un fallo`() = runTest {
        val transport = FakeTransport {
            HttpResponse(200, """{"success":false,"message":"Reserved range"}""")
        }

        val info = IpwhoIsClient(transport = transport).fetch("10.0.0.1")

        assertNull(info.countryCode)
        assertNull(info.countryName)
        assertFalse(info.error)
    }

    @Test
    fun `un error de servidor devuelve el error http`() = runTest {
        val transport = FakeTransport { HttpResponse(500, "") }

        val error = runCatching { IpwhoIsClient(transport = transport).fetch("8.8.8.8") }.exceptionOrNull()

        assertTrue(error is GeoHttpException)
        assertEquals(500, (error as GeoHttpException).status)
    }

    @Test
    fun `un 429 lanza la excepcion de limite de tasa`() = runTest {
        val transport = FakeTransport { HttpResponse(429, "", mapOf("Retry-After" to "90")) }

        val error = runCatching { IpwhoIsClient(transport = transport).fetch("8.8.8.8") }.exceptionOrNull()

        assertEquals(90L, (error as RateLimitedException).retryAfterSeconds)
    }

    @Test
    fun `un json invalido es un error de respuesta`() = runTest {
        val transport = FakeTransport { HttpResponse(200, "no es json") }

        val error = runCatching { IpwhoIsClient(transport = transport).fetch("8.8.8.8") }.exceptionOrNull()

        assertTrue(error is GeoResponseException)
    }

    @Test
    fun `el error no incluye la ip`() = runTest {
        val transport = FakeTransport { HttpResponse(500, "fallo con 8.8.8.8") }

        val error = runCatching { IpwhoIsClient(transport = transport).fetch("8.8.8.8") }.exceptionOrNull()

        assertFalse(error?.message.orEmpty().contains("8.8.8.8"))
    }

    private class FakeTransport(private val response: () -> HttpResponse) : HttpTransport {
        var lastUrl: String? = null

        override suspend fun get(url: String): HttpResponse {
            lastUrl = url
            return response()
        }
    }
}
