package com.usipipo.vpn.data.geo

import com.usipipo.vpn.data.ConfigStore
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.CountryInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Repositorio de geolocalización de país con caché por perfil.
 *
 * Política:
 * - **Validación previa:** una IP que no sea IPv4 o IPv6 completa no se consulta
 *   (ni al principal ni al respaldo) y se registra el motivo.
 * - **Caché por perfil:** código, nombre y fecha junto al perfil. Se renueva si
 *   cambia la IP del endpoint o si pasan 30 días.
 * - **Reintento:** un único reintento unos segundos después del fallo. Si vuelve
 *   a fallar, no se reintenta más hasta la siguiente conexión.
 * - **Enfriamiento por perfil:** tras dos fallos seguidos, el perfil no se
 *   consulta durante un minuto. Un 429 del proveedor enfría todas las consultas.
 * - **Nunca bloquea:** cualquier fallo degrada a [CountryInfo] con `error = true`.
 * - **El registro no lleva la IP ni el token**, solo el tipo de fallo.
 */
class CountryRepository(
    private val store: ConfigStore,
    private val http: CountryHttpClient,
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    /** Caché válida durante 30 días. */
    private val cacheTtlMs: Long = 30L * 24 * 60 * 60 * 1000

    /** Momento (reloj de pared) hasta el que no se vuelve a consultar tras un 429. */
    private var nextQueryAllowedAt: Long = 0L

    /** Fallos consecutivos por perfil: se limpian con un acierto. */
    private val consecutiveFailures = mutableMapOf<String, Int>()

    /** Perfiles en enfriamiento: id del perfil → momento en que se puede consultar. */
    private val cooldownUntil = mutableMapOf<String, Long>()

    /**
     * Devuelve el país para [configId] y [ip].
     *
     * - IP inválida: no se consulta nada y se devuelve el error.
     * - Caché hit: misma IP y edad < 30 días → se devuelve el país guardado.
     * - Caché miss: se consulta al proveedor y, si hay país, se guarda.
     * - Fallo: se devuelve `CountryInfo(ip, error = true)` y el detalle queda en
     *   el registro, sin IP ni token.
     */
    suspend fun countryFor(configId: String, ip: String): CountryInfo {
        if (!IpValidator.isValid(ip)) {
            AppLog.e(
                "Geo",
                "IP de endpoint no válida (perfil $configId): ${IpValidator.invalidReason(ip)}",
            )
            return failed(ip)
        }

        cached(configId, ip)?.let { return it }

        if (now() < nextQueryAllowedAt) return failed(ip)

        val cooling = cooldownUntil[configId]
        if (cooling != null && now() < cooling) return failed(ip)

        return query(configId, ip)
    }

    /** País guardado en el perfil si sigue siendo válido. */
    private suspend fun cached(configId: String, ip: String): CountryInfo? {
        val config = store.configs.value.firstOrNull { it.id == configId } ?: return null
        val fetchedAt = config.countryFetchedAt ?: return null
        if (config.countryIp != ip || config.countryCode == null) return null
        if (now() - fetchedAt >= cacheTtlMs) return null
        return CountryInfo(
            ip = ip,
            countryCode = config.countryCode,
            countryName = config.countryName,
            fetchedAt = fetchedAt,
            error = false,
        )
    }

    /** Consulta al proveedor con un único reintento. */
    private suspend fun query(configId: String, ip: String): CountryInfo {
        var attempt = 0
        while (true) {
            attempt++
            try {
                val info = withContext(Dispatchers.IO) { http.fetch(ip) }
                if (info.error) {
                    registerFailure(configId)
                    AppLog.e("Geo", "Geolocalización sin resultado (perfil $configId)")
                    return failed(ip)
                }
                if (info.countryCode == null) {
                    // IP reservada o sin datos: no es un fallo, pero tampoco se cachea.
                    AppLog.d("Geo", "Geolocalización sin país para el perfil $configId")
                    return info
                }
                store.updateCountry(configId, info)
                consecutiveFailures.remove(configId)
                cooldownUntil.remove(configId)
                return info
            } catch (e: RateLimitedException) {
                nextQueryAllowedAt = now() + e.retryAfterSeconds * 1000
                AppLog.e(
                    "Geo",
                    "Límite de tasa del proveedor de geolocalización; sin reintento hasta ${e.retryAfterSeconds}s",
                )
                return failed(ip)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= MAX_ATTEMPTS) {
                    registerFailure(configId)
                    AppLog.e(
                        "Geo",
                        "Fallo de geolocalización (perfil $configId): ${describeGeoError(e)}",
                    )
                    return failed(ip)
                }
                AppLog.w(
                    "Geo",
                    "Fallo de geolocalización (perfil $configId): ${describeGeoError(e)}; reintento en ${RETRY_DELAY_MS}ms",
                )
                delay(RETRY_DELAY_MS)
            }
        }
    }

    /** Suma un fallo del perfil y activa el enfriamiento si son varios seguidos. */
    private fun registerFailure(configId: String) {
        val failures = (consecutiveFailures[configId] ?: 0) + 1
        consecutiveFailures[configId] = failures
        if (failures >= FAILURES_BEFORE_COOLDOWN) {
            cooldownUntil[configId] = now() + COOLDOWN_MS
        }
    }

    private fun failed(ip: String) = CountryInfo(
        ip = ip,
        countryCode = null,
        countryName = null,
        fetchedAt = now(),
        error = true,
    )

    private companion object {
        /** Un intento y un reintento. */
        const val MAX_ATTEMPTS = 2

        /** Espera antes del reintento. */
        const val RETRY_DELAY_MS = 3000L

        /** Enfriamiento del perfil tras varios fallos seguidos. */
        const val COOLDOWN_MS = 60_000L

        /** Fallos seguidos que activan el enfriamiento. */
        const val FAILURES_BEFORE_COOLDOWN = 2
    }
}
