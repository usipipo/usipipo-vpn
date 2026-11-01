package com.usipipo.vpn.data.geo

import com.usipipo.vpn.diagnostics.AppLog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpinfoLiteClientTest {

    @Test
    fun `consulta la url de lite con el token`() = runTest {
        val transport = FakeTransport { HttpResponse(200, """{"country_code":"DE","country":"Germany"}""") }

        IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8")

        assertEquals("https://api.ipinfo.io/lite/8.8.8.8?token=abc123", transport.lastUrl)
    }

    @Test
    fun `sin token no hace ninguna peticion`() = runTest {
        val transport = FakeTransport { HttpResponse(200, "{}") }

        val error = runCatching { IpinfoLiteClient(token = "  ", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertTrue(error is MissingTokenException)
        assertEquals(0, transport.calls)
    }

    @Test
    fun `una respuesta correcta devuelve codigo y nombre del pais`() = runTest {
        val transport = FakeTransport {
            HttpResponse(200, """{"ip":"8.8.8.8","asn":"AS15169","country_code":"DE","country":"Germany"}""")
        }

        val info = IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8")

        assertEquals("DE", info.countryCode)
        assertEquals("Germany", info.countryName)
        assertFalse(info.error)
    }

    @Test
    fun `una respuesta bogon no trae pais pero no es un fallo`() = runTest {
        val transport = FakeTransport { HttpResponse(200, """{"ip":"10.0.0.1","bogon":true}""") }

        val info = IpinfoLiteClient(token = "abc123", transport = transport).fetch("10.0.0.1")

        assertNull(info.countryCode)
        assertNull(info.countryName)
        assertFalse(info.error)
    }

    @Test
    fun `una respuesta sin country_code no trae pais pero no es un fallo`() = runTest {
        val transport = FakeTransport { HttpResponse(200, """{"ip":"8.8.8.8","asn":"AS15169"}""") }

        val info = IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8")

        assertNull(info.countryCode)
        assertNull(info.countryName)
        assertFalse(info.error)
    }

    @Test
    fun `un token invalido devuelve el error http`() = runTest {
        val transport = FakeTransport { HttpResponse(401, """{"error":"invalid token"}""") }

        val error = runCatching { IpinfoLiteClient(token = "malo", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertTrue(error is GeoHttpException)
        assertEquals(401, (error as GeoHttpException).status)
    }

    @Test
    fun `un 404 devuelve el error http`() = runTest {
        val transport = FakeTransport { HttpResponse(404, """{"error":"not found"}""") }

        val error = runCatching { IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertTrue(error is GeoHttpException)
        assertEquals(404, (error as GeoHttpException).status)
    }

    @Test
    fun `un 429 lanza la excepcion de limite de tasa`() = runTest {
        val transport = FakeTransport { HttpResponse(429, "", mapOf("Retry-After" to "120")) }

        val error = runCatching { IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertTrue(error is RateLimitedException)
        assertEquals(120L, (error as RateLimitedException).retryAfterSeconds)
    }

    @Test
    fun `un 429 sin cabecera usa la espera por defecto`() = runTest {
        val transport = FakeTransport { HttpResponse(429, "") }

        val error = runCatching { IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertEquals(60L, (error as RateLimitedException).retryAfterSeconds)
    }

    @Test
    fun `un error de servidor devuelve el error http`() = runTest {
        val transport = FakeTransport { HttpResponse(503, "") }

        val error = runCatching { IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertTrue(error is GeoHttpException)
        assertEquals(503, (error as GeoHttpException).status)
    }

    @Test
    fun `un timeout se propaga como error de red`() = runTest {
        val transport = FakeTransport { throw java.net.SocketTimeoutException("timeout") }

        val error = runCatching { IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertTrue(error is java.net.SocketTimeoutException)
    }

    @Test
    fun `un json invalido es un error de respuesta`() = runTest {
        val transport = FakeTransport { HttpResponse(200, "no es json") }

        val error = runCatching { IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        assertTrue(error is GeoResponseException)
    }

    @Test
    fun `el error no incluye la ip ni el token`() = runTest {
        val transport = FakeTransport { HttpResponse(500, "fallo con 8.8.8.8 y abc123") }

        val error = runCatching { IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8") }
            .exceptionOrNull()

        val message = error?.message.orEmpty()
        assertFalse(message.contains("8.8.8.8"))
        assertFalse(message.contains("abc123"))
    }

    @Test
    fun `el registro de la peticion usa la plantilla sin ip ni token`() = runTest {
        val mensajes = mutableListOf<String>()
        AppLog.setVerbose(true)
        AppLog.mirror = { _, _, message -> mensajes += message }
        try {
            val transport = FakeTransport { HttpResponse(200, """{"country_code":"DE"}""") }

            IpinfoLiteClient(token = "abc123", transport = transport).fetch("8.8.8.8")

            assertTrue(mensajes.any { it.contains("https://api.ipinfo.io/lite/{ip}") })
            assertFalse(mensajes.any { it.contains("8.8.8.8") })
            assertFalse(mensajes.any { it.contains("abc123") })
        } finally {
            AppLog.mirror = null
            AppLog.setVerbose(false)
        }
    }

    private class FakeTransport(private val response: () -> HttpResponse) : HttpTransport {
        var calls: Int = 0
        var lastUrl: String? = null

        override suspend fun get(url: String): HttpResponse {
            calls++
            lastUrl = url
            return response()
        }
    }
}
