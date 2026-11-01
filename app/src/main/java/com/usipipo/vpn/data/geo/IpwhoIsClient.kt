package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.optStringOrNull
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.CountryInfo
import org.json.JSONException
import org.json.JSONObject

/**
 * Cliente de respaldo contra **ipwho.is**: `GET https://ipwho.is/{ip}`.
 *
 * - HTTPS y sin token: sirve de red de seguridad cuando el proveedor principal
 *   falla o no trae token en la compilación.
 * - Su plan gratuito está limitado a 1.000 peticiones/día por IP de cliente, así
 *   que solo se usa cuando el primario falla.
 * - Responde `success: false` con HTTP 200 para rangos reservados: no es un
 *   fallo de la consulta, es «sin país».
 */
class IpwhoIsClient(
    private val transport: HttpTransport = UrlConnectionTransport(),
) : CountryHttpClient {

    override suspend fun fetch(ip: String): CountryInfo {
        // Plantilla, no la URL real: la IP no llega al registro.
        AppLog.d("Geo", "Consultando GET $BASE_URL/{ip}")
        val response = transport.get("$BASE_URL/$ip")
        return when {
            response.status == HTTP_TOO_MANY_REQUESTS -> throw RateLimitedException(
                response.header("Retry-After")?.toLongOrNull() ?: DEFAULT_RATE_LIMIT_SECONDS,
            )
            response.status == HTTP_OK -> parse(response.body, ip)
            else -> throw GeoHttpException(PROVIDER, response.status)
        }
    }

    private fun parse(body: String, ip: String): CountryInfo {
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            throw GeoResponseException("$PROVIDER devolvió una respuesta inválida")
        }
        if (!json.optBoolean("success", false)) {
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
        const val PROVIDER = "ipwho.is"
        const val BASE_URL = "https://ipwho.is"

        /** Espera conservadora si el proveedor no comunica el reinicio del contador. */
        const val DEFAULT_RATE_LIMIT_SECONDS = 60L

        private const val HTTP_OK = 200
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
