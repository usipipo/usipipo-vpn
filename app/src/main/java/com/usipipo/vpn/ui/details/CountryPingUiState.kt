package com.usipipo.vpn.ui.details

import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.PingResult

/**
 * Estado de la pantalla Detalles para país y ping.
 *
 * @param countryInfo información de geolocalización (null si aún no se consultó o falló).
 * @param pingResult resultado del último ping (null si aún no se hizo ping).
 * @param isLoadingCountry true mientras se consulta la API de país.
 */
data class CountryPingUiState(
    val countryInfo: CountryInfo? = null,
    val pingResult: PingResult? = null,
    val isLoadingCountry: Boolean = false,
)
