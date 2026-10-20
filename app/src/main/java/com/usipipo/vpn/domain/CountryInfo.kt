package com.usipipo.vpn.domain

/**
 * Resultado de geolocalización de una IP (ipapi.co).
 *
 * @param ip la IP consultada.
 * @param countryCode código ISO 2 letras (ej. "DE"), null si no se conoció o falló.
 * @param countryName nombre del país (ej. "Germany"), null si no se conoció o falló.
 * @param fetchedAt marca de tiempo de la consulta (milisegundos de reloj del sistema).
 * @param error true si la consulta falló (timeout, red, 429, JSON inválido…).
 */
data class CountryInfo(
    val ip: String,
    val countryCode: String?,
    val countryName: String?,
    val fetchedAt: Long,
    val error: Boolean = false,
)
