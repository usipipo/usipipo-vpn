package com.usipipo.vpn.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Fila de país y ping para la pantalla Detalles.
 *
 * - País: emoji de bandera (código ISO 2 letras → regional indicator) + nombre,
 *   o globo `⏣` si no hay dato o falló la consulta.
 * - Ping: badge coloreado según latencia (verde <100, ámbar 100-250, rojo >250,
 *   gris `—` si no hay dato o falló).
 */
@Composable
fun CountryPingRow(state: CountryPingUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // País y bandera
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val countryInfo = state.countryInfo
            if (countryInfo != null && !countryInfo.error && countryInfo.countryCode != null) {
                Text(
                    text = flagEmoji(countryInfo.countryCode),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = countryInfo.countryName ?: countryInfo.countryCode ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = "\u2B1B", // globo ⏣
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // Ping con badge de color
        val pingResult = state.pingResult
        val pingText = pingResult?.ms?.let { "$it ms" } ?: "—"
        val pingColor = when {
            pingResult == null || pingResult.error || pingResult.ms == null -> null
            pingResult.ms < 100 -> Color(0xFF00C853) // verde
            pingResult.ms <= 250 -> Color(0xFFFFB300) // ámbar
            else -> Color(0xFFFF3D3D) // rojo
        }

        Chip(
            label = { Text(text = pingText, style = MaterialTheme.typography.bodySmall) },
            colors = pingColor?.let { ChipDefaults.colors(containerColor = it) },
            onClick = null,
            modifier = Modifier.heightIn(min = 24.dp),
        )
    }
}

private fun flagEmoji(countryCode: String): String {
    return countryCode
        .map { it.code - 'A'.code + 0x1F1E6 }
        .joinToString("") { it.toChar() }
}
