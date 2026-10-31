package com.usipipo.vpn.data.geo

import com.usipipo.vpn.domain.CountryInfo
import kotlinx.coroutines.CancellationException

/**
 * Localizador de país que encadena dos proveedores: el principal y el respaldo.
 *
 * - Si el principal responde, su resultado manda.
 * - Si el principal no trae país o falla por infraestructura (red, timeout,
 *   HTTP de error), se consulta el respaldo.
 * - Si el principal está limitado por tasa (429), no se consulta el respaldo:
 *   el límite es del token, no del proveedor, y saltarse el enfriamiento
 *   multiplicaría las peticiones.
 * - Si la petición se cancela (el usuario desconectó), la cancelación se
 *   propaga y no se consulta nada más.
 */
class GeoLocator(
    private val primary: CountryHttpClient,
    private val fallback: CountryHttpClient,
) : CountryHttpClient {

    override suspend fun fetch(ip: String): CountryInfo {
        val primaryInfo = try {
            primary.fetch(ip)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RateLimitedException) {
            throw e
        } catch (e: Exception) {
            return fallback.fetchOrFail(ip, e)
        }
        return if (primaryInfo.countryCode != null) {
            primaryInfo
        } else {
            fallback.fetchOrFail(ip, null)
        }
    }

    /**
     * Consulta el respaldo. Si también falla, se informa de los dos fallos sin
     * copiar sus mensajes (podrían llevar la URL con el token). Un límite de
     * tasa del respaldo se propaga tal cual: activa el enfriamiento.
     */
    private suspend fun CountryHttpClient.fetchOrFail(ip: String, primaryError: Throwable?): CountryInfo =
        try {
            fetch(ip)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RateLimitedException) {
            throw e
        } catch (e: Exception) {
            throw if (primaryError == null) e else GeoProvidersException(primaryError, e)
        }
}
