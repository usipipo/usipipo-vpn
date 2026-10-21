package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.optStringOrNull
import com.usipipo.vpn.domain.CountryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente mínimo contra `https://ipapi.co/json/{ip}/`.
 *
 * - HTTPS obligatorio (capa gratuita de ipapi.co lo soporta).
 * - Timeout de 3 s, 1 intento, sin reintentos.
 * - Parsea `country_name` y `country_code` del JSON.
 */
class IpApiClient(private val timeoutMs: Int = 3000) : CountryHttpClient {

    /** Primeros 2 octetos: suficiente para diagnosticar sin filtrar la IP completa. */
    private fun abbreviate(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size >= 2) "${parts[0]}.${parts[1]}" else ip
    }

    override suspend fun fetch(ip: String): CountryInfo = withContext(Dispatchers.IO) {
        val url = URL("https://ipapi.co/json/$ip/")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.instanceFollowRedirects = true
        try {
            val code = connection.responseCode
            if (code != 200) {
                throw IOException("ipapi.co respondió HTTP $code para ${abbreviate(ip)}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            CountryInfo(
                ip = ip,
                countryCode = json.optStringOrNull("country_code"),
                countryName = json.optStringOrNull("country_name"),
                fetchedAt = System.currentTimeMillis(),
                error = false,
            )
        } finally {
            connection.disconnect()
        }
    }
}
