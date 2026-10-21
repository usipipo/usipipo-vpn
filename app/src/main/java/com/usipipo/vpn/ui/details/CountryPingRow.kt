package com.usipipo.vpn.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.GLOBE_EMOJI
import com.usipipo.vpn.ui.components.flagEmoji
import com.usipipo.vpn.ui.theme.LocalPingColors

/**
 * Fila de país y ping para la pantalla Detalles.
 *
 * - País: emoji de bandera (código ISO 2 letras → regional indicator) + nombre,
 *   o el globo si no hay dato o falló la consulta.
 * - Ping: badge con color según latencia (verde <100 ms, ámbar 100-250 ms, rojo >250 ms,
 *   gris «—» si no hay dato o falló).
 */
@Composable
fun CountryPingRow(state: CountryPingUiState, modifier: Modifier = Modifier) {
    val pingColors = LocalPingColors.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // País y bandera
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val countryInfo = state.countryInfo
            val countryCode = if (countryInfo != null && !countryInfo.error) countryInfo.countryCode else null
            val flag = countryCode?.let { flagEmoji(it) }?.takeIf { it.isNotEmpty() }
            when {
                state.isLoadingCountry -> Text(
                    text = stringResource(R.string.country_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                flag != null -> {
                    Text(
                        text = flag,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = countryInfo?.countryName ?: countryCode.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> Text(
                    text = GLOBE_EMOJI,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // Ping con badge de color
        val pingResult = state.pingResult
        val pingText = pingResult?.ms?.let { stringResource(R.string.ping_ms, it) }
            ?: stringResource(R.string.ping_unknown)
        val pingColor = when {
            pingResult == null || pingResult.error || pingResult.ms == null -> pingColors.unknown
            pingResult.ms < 100 -> pingColors.good
            pingResult.ms <= 250 -> pingColors.warn
            else -> pingColors.bad
        }

        Text(
            text = pingText,
            style = MaterialTheme.typography.bodySmall,
            color = pingColor,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(
                    color = pingColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                ),
        )
    }
}
