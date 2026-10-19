package com.usipipo.vpn.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.StatTile
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.details.StatsUiState
import com.usipipo.vpn.ui.details.formatRate
import com.usipipo.vpn.ui.details.rememberUptimeText
import com.usipipo.vpn.ui.theme.UsipipoTheme

/** Tarjetas de Inicio con el túnel conectado: protocolo, tiempo, velocidades y acceso a Detalles. */
@Composable
internal fun ConnectionStats(
    sinceMs: Long,
    stats: StatsUiState,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = R.drawable.ic_shield_check,
                label = stringResource(R.string.stat_protocol),
                value = stringResource(R.string.protocol_wireguard),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = R.drawable.ic_clock,
                label = stringResource(R.string.stat_uptime),
                value = rememberUptimeText(sinceMs),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                icon = R.drawable.ic_arrow_down,
                label = stringResource(R.string.stat_download),
                value = formatRate(stats.downBps),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = R.drawable.ic_arrow_up,
                label = stringResource(R.string.stat_upload),
                value = formatRate(stats.upBps),
                modifier = Modifier.weight(1f),
            )
        }
        UsipipoCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenDetails) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.view_details),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Estadísticas de conexión")
@Preview(name = "Estadísticas de conexión oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewConnectionStats() {
    UsipipoTheme {
        Surface {
            ConnectionStats(
                sinceMs = 0L,
                stats = StatsUiState(downBps = 7_500_000L, upBps = 5_000_000L),
                onOpenDetails = {},
            )
        }
    }
}
