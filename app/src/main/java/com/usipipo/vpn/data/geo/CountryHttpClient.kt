package com.usipipo.vpn.data.geo

import com.usipipo.vpn.domain.CountryInfo

/**
 * Cliente HTTP para consultar la geolocalización de una IP.
 *
 * Implementaciones: [IpinfoLiteClient] (principal), [IpwhoIsClient] (respaldo) y
 * [GeoLocator] (cadena de ambos).
 */
interface CountryHttpClient {
    /**
     * Consulta el proveedor para [ip]. Lanza excepción si falla (timeout, red,
     * HTTP ≠ 200, JSON inválido, límite de tasa). Si el proveedor responde sin
     * país, devuelve un [CountryInfo] sin código y sin `error`.
     */
    suspend fun fetch(ip: String): CountryInfo
}
