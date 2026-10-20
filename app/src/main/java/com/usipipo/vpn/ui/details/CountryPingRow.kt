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
import androidx.compose.ui.unit.dp

/**
 * Fila de país y ping para la pantalla Detalles.
 *
 * - País: emoji de bandera (código ISO 2 letras → regional indicator) + nombre,
 *   o globo `⏣` si no hay dato o falló la consulta.
 * - Ping: badge coloreado según latencia usando colores del tema
 *   (primario <100, secundario 100-250, error >250, superficie variante «—»).
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
                    text = "\u26E2", // globo ⏣
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // Ping con badge de color (colores del tema, sin hardcodeo)
        val pingResult = state.pingResult
        val pingText = pingResult?.ms?.let { "$it ms" } ?: "—"
        val pingColor = when {
            pingResult == null || pingResult.error || pingResult.ms == null -> null
            pingResult.ms < 100 -> MaterialTheme.colorScheme.primary
            pingResult.ms <= 250 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        }

        Chip(
            label = { Text(text = pingText, style = MaterialTheme.typography.bodySmall) },
            colors = pingColor?.let { ChipDefaults.colors(containerColor = it) },
            onClick = null,
            modifier = Modifier.heightIn(min = 24.dp),
        )
    }
}

/** Convierte un código ISO 2 letras en emoji de bandera (indicadores regionales). */
internal fun flagEmoji(countryCode: String): String {
    val code = countryCode.uppercase().takeIf { it.length == 2 } ?: return ""
    return code
        .map { it.code - 'A'.code + 0x1F1E6 }
        .joinToString("") { it.toChar() }
}
