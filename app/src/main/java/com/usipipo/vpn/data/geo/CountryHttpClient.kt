package com.usipipo.vpn.data.geo

import com.usipipo.vpn.domain.CountryInfo

/** Cliente HTTP para consultar la geolocalización de una IP. */
interface CountryHttpClient {
    /** Consulta ipapi.co para [ip]. Lanza excepción si falla (timeout, red, HTTP != 200, JSON inválido). */
    suspend fun fetch(ip: String): CountryInfo
}
