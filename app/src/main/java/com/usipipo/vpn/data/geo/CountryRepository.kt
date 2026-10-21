package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.CountryInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de geolocalización de país con caché por perfil (TTL 24 h).
 *
 * Consulta `CountryHttpClient` sólo en caché miss o cambio de IP. Nunca bloquea
 * la conexión: cualquier fallo se degrada a [CountryInfo] con `error = true`.
 */
class CountryRepository(
    private val store: ConfigStore,
    private val http: CountryHttpClient,
) {

    private val cacheTtlMs: Long = 24 * 60 * 60 * 1000L

    /**
     * Devuelve el país para [configId] y [ip].
     *
     * - Caché hit: `countryIp == ip` y edad < 24 h → retorna país guardado sin llamar a la API.
     * - Caché miss: llama al HTTP, guarda el resultado vía `store.updateCountry` y lo retorna.
     * - Fallo de API: retorna `CountryInfo(ip, error = true)`, anota en `AppLog` con IP abreviada,
     *   y no sobrescribe la caché anterior.
     */
    suspend fun countryFor(configId: String, ip: String): CountryInfo = withContext(Dispatchers.IO) {
        val config = store.configs.value.firstOrNull { it.id == configId }

        // Caché hit: misma IP y dentro del TTL.
        val fetchedAt = config?.countryFetchedAt
        if (config?.countryIp == ip && fetchedAt != null) {
            val age = System.currentTimeMillis() - fetchedAt
            if (age < cacheTtlMs) {
                return@withContext CountryInfo(
                    ip = ip,
                    countryCode = config.countryCode,
                    countryName = config.countryName,
                    fetchedAt = fetchedAt,
                    error = false,
                )
            }
        }

        // Caché miss: consultar la API externa.
        try {
            val info = http.fetch(ip)
            if (info.error) {
                // La API respondio con error: no se guarda nada (spec 5.2.5) ni se
                // sustituye la cache anterior.
                AppLog.e("Geo", "Geolocalización sin resultado para IP ${abbreviateIp(ip)}")
                return@withContext failed(ip)
            }
            store.updateCountry(configId, info)
            return@withContext info
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No se adjunta la excepción: su stack trace puede incluir el cuerpo de la
            // respuesta (con la IP completa), que el informe de diagnostico si envia.
            AppLog.e("Geo", "Fallo de geolocalización para IP ${abbreviateIp(ip)} (${e::class.simpleName})")
            return@withContext failed(ip)
        }
    }

    private fun failed(ip: String) = CountryInfo(
        ip = ip,
        countryCode = null,
        countryName = null,
        fetchedAt = System.currentTimeMillis(),
        error = true,
    )

    /** Devuelve los primeros 2 octetos de [ip] para registrar sin exponer la IP completa. */
    private fun abbreviateIp(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size >= 2) "${parts[0]}.${parts[1]}" else ip
    }
}
