package com.usipipo.vpn.data.geo

import com.usipipo.vpn.BuildConfig
import com.usipipo.vpn.data.optStringOrNull
import com.usipipo.vpn.domain.CountryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONException
import org.json.JSONObject

/**
 * Cliente mínimo contra `https://ipapi.co/json/{ip}/`.
 *
 * - HTTPS obligatorio (capa gratuita de ipapi.co lo soporta).
 * - Timeout de 3 s, 1 intento, sin reintentos.
 * - Parsea `country_name` y `country_code` del JSON.
 */
class IpApiClient(private val timeoutMs: Int = 3000) : CountryHttpClient {

    private companion object {
        /** Espera conservadora si la API no comunica el reinicio del contador. */
        const val DEFAULT_RATE_LIMIT_SECONDS = 60L
    }

    /** Primeros 2 octetos: suficiente para diagnosticar sin filtrar la IP completa. */
    private fun abbreviate(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size >= 2) "${parts[0]}.${parts[1]}" else ip
    }

    /**
     * Parsea el cuerpo de la respuesta. Si el JSON es inválido se lanza una excepción
     * propia: la original incluye el cuerpo recibido (con la IP) en su mensaje.
     */
    private fun parseBody(body: String): JSONObject = try {
        JSONObject(body)
    } catch (e: JSONException) {
        throw IOException("Respuesta inválida de ipapi.co")
    }

    override suspend fun fetch(ip: String): CountryInfo = withContext(Dispatchers.IO) {
        val url = URL("https://ipapi.co/json/$ip/")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "uSipipoVPN/${BuildConfig.VERSION_NAME}")
        try {
            val code = connection.responseCode
            if (code == 429) {
                // ipapi.co comunica el reinicio del contador en 'X-Ttl' (segundos).
                val retryAfter = connection.getHeaderField("X-Ttl")?.toLongOrNull()
                    ?: DEFAULT_RATE_LIMIT_SECONDS
                throw RateLimitedException(retryAfter)
            }
            if (code != 200) {
                throw IOException("ipapi.co respondió HTTP $code para ${abbreviate(ip)}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = parseBody(body)
            // ipapi.co puede responder HTTP 200 con { "error": true, "reason": ... }.
            if (json.optBoolean("error", false)) {
                throw IOException("ipapi.co devolvió error para ${abbreviate(ip)}")
            }
            val countryCode = json.optStringOrNull("country_code")
                ?: throw IOException("ipapi.co no devolvió country_code para ${abbreviate(ip)}")
            CountryInfo(
                ip = ip,
                countryCode = countryCode,
                countryName = json.optStringOrNull("country_name"),
                fetchedAt = System.currentTimeMillis(),
                error = false,
            )
        } finally {
            connection.disconnect()
        }
    }
}
