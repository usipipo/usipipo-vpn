package com.usipipo.vpn.ui.details

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.FontWeight
import androidx.compose.ui.text.input.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.usipipo.vpn.R
import com.usipipo.vpn.data.DuplicateNameException
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.ui.components.IconTile
import com.usipipo.vpn.ui.components.StatTile
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.theme.UsipipoTheme
import kotlin.Result
import kotlinx.coroutines.launch

/** Pantalla de detalles de la conexión activa (sub-pantalla de Inicio, no pestaña). */
@Composable
fun DetailsScreen(
    config: SavedConfig?,
    tunnel: TunnelState,
    stats: StatsUiState,
    onClose: () -> Unit,
    onDisconnect: () -> Unit,
    onRename: suspend (String) -> Result<SavedConfig>,
) {
    val connected = tunnel as? TunnelState.Connected
    val unavailable = connected == null || config == null
    LaunchedEffect(unavailable) { if (unavailable) onClose() }
    if (connected == null || config == null) return

    var renaming by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Text(
                    text = stringResource(R.string.details_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(R.drawable.ic_server)
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ) {
                                Text(
                                    text = stringResource(R.string.config_active),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Text(
                            text = config.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.protocol_wireguard),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(onClick = { renaming = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.cd_rename, config.name),
                        )
                    }
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.details_stats_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ThroughputChart(
                        downHistory = stats.downHistory,
                        upHistory = stats.upHistory,
                    )
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
                    Text(
                        text = stringResource(R.string.details_stats_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow(
                        label = stringResource(R.string.details_connection_time),
                        value = rememberUptimeText(connected.sinceMs),
                    )
                    InfoRow(
                        label = stringResource(R.string.stat_protocol),
                        value = stringResource(R.string.protocol_wireguard),
                    )
                    InfoRow(
                        label = stringResource(R.string.details_endpoint),
                        value = stats.endpoint ?: stringResource(R.string.endpoint_unknown),
                    )
                    InfoRow(
                        label = stringResource(R.string.details_ipv6),
                        value = if (stats.routesIpv6) stringResource(R.string.ipv6_enabled) else stringResource(R.string.ipv6_disabled),
                    )
                }
            }
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.disconnect))
            }
        }
    }

    if (renaming) {
        RenameDialog(
            currentName = config.name,
            onRename = { onRename(it) },
            onDismiss = { renaming = false },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onRename: suspend (String) -> Result<SavedConfig>,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val currentError = error

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text(stringResource(R.string.name_label)) },
                singleLine = true,
                isError = currentError != null,
                supportingText = if (currentError != null) {
                    { Text(stringResource(currentError)) }
                } else {
                    null
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    onRename(name).fold(
                        onSuccess = { onDismiss() },
                        onFailure = { error = renameErrorRes(it) },
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@StringRes
private fun renameErrorRes(error: Throwable): Int = when (error) {
    is DuplicateNameException -> R.string.err_name_duplicate
    is IllegalArgumentException -> R.string.err_name_empty
    else -> R.string.err_save_failed
}

@Preview(name = "Detalles claro")
@Preview(name = "Detalles oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDetailsScreen() {
    UsipipoTheme {
        Surface {
            DetailsScreen(
                config = SavedConfig("a", "client-mowgli"),
                tunnel = TunnelState.Connected("a", sinceMs = 1L),
                stats = StatsUiState(
                    downHistory = List(60) { (it % 20) * 500_000L },
                    upHistory = List(60) { (it % 20) * 500_000L },
                    endpoint = "frankfurt.example.com:51820",
                    routesIpv6 = false,
                ),
                onClose = {},
                onDisconnect = {},
                onRename = { Result.success(SavedConfig("a", it)) },
            )
        }
    }
}
