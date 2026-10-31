package com.usipipo.vpn.data.geo

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Respuesta HTTP mínima: código de estado, cuerpo y cabeceras.
 *
 * @param status código de estado (200, 404…).
 * @param body cuerpo de la respuesta (vacío si no había).
 * @param headers cabeceras de la respuesta.
 */
data class HttpResponse(
    val status: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
) {
    /** Cabecera buscada sin distinguir mayúsculas, o null si no está. */
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

/**
 * Transporte HTTP inyectable. Permite probar los clientes de geolocalización sin
 * red ni Android.
 */
interface HttpTransport {
    /** Hace un GET y devuelve la respuesta. Lanza la excepción de red si falla. */
    suspend fun get(url: String): HttpResponse
}

/** Transporte real sobre [HttpURLConnection], con timeout de conexión y lectura. */
class UrlConnectionTransport(
    private val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val userAgent: String = DEFAULT_USER_AGENT,
) : HttpTransport {

    override suspend fun get(url: String): HttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", userAgent)
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val headers = connection.headerFields
                .filterKeys { it != null }
                .mapValues { (_, values) -> values.firstOrNull().orEmpty() }
            HttpResponse(status, body, headers)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 3000
        const val DEFAULT_USER_AGENT = "uSipipoVPN"
    }
}
