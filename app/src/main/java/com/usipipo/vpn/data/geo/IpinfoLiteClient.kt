package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.optStringOrNull
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.CountryInfo
import java.net.URLEncoder
import org.json.JSONException
import org.json.JSONObject

/**
 * Cliente contra la API **IPinfo Lite**: `GET https://api.ipinfo.io/lite/{ip}`.
 *
 * - Es el proveedor principal: sin límite de cuota de peticiones.
 * - Exige token; si la compilación no lo trae, no se consulta nada
 *   ([MissingTokenException]) y el geo localizador recurre al respaldo.
 * - Solo se usan `country_code` y `country`; la bandera se calcula aparte.
 * - Una IP reservada responde `{"bogon": true}`: no es un fallo, es «sin país».
 * - El token viaja en la URL, así que ni la IP ni el token aparecen en los
 *   mensajes de error.
 */
class IpinfoLiteClient(
    private val token: String,
    private val transport: HttpTransport = UrlConnectionTransport(),
) : CountryHttpClient {

    override suspend fun fetch(ip: String): CountryInfo {
        if (token.isBlank()) throw MissingTokenException()
        // Plantilla, no la URL real: ni la IP ni el token llegan al registro.
        AppLog.d("Geo", "Consultando GET $BASE_URL/{ip}?token=***")
        val response = transport.get(url(ip))
        return when {
            response.status == HTTP_TOO_MANY_REQUESTS -> throw RateLimitedException(
                response.header("Retry-After")?.toLongOrNull() ?: DEFAULT_RATE_LIMIT_SECONDS,
            )
            response.status == HTTP_OK -> parse(response.body, ip)
            else -> throw GeoHttpException(PROVIDER, response.status)
        }
    }

    /** Plantilla de la URL: el token se codifica para no romper la consulta. */
    private fun url(ip: String): String =
        "$BASE_URL/$ip?token=${URLEncoder.encode(token, "UTF-8")}"

    private fun parse(body: String, ip: String): CountryInfo {
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            throw GeoResponseException("$PROVIDER devolvió una respuesta inválida")
        }
        if (json.optBoolean("bogon", false)) {
            return CountryInfo(
                ip = ip,
                countryCode = null,
                countryName = null,
                fetchedAt = System.currentTimeMillis(),
                error = false,
            )
        }
        return CountryInfo(
            ip = ip,
            countryCode = json.optStringOrNull("country_code"),
            countryName = json.optStringOrNull("country"),
            fetchedAt = System.currentTimeMillis(),
            error = false,
        )
    }

    companion object {
        const val PROVIDER = "IPinfo Lite"
        const val BASE_URL = "https://api.ipinfo.io/lite"

        /** Espera conservadora si el proveedor no comunica el reinicio del contador. */
        const val DEFAULT_RATE_LIMIT_SECONDS = 60L

        private const val HTTP_OK = 200
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
