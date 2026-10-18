package com.usipipo.vpn.ui.home

import android.app.Activity
import android.content.res.Configuration
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.StringRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usipipo.vpn.R
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.domain.isActive
import com.usipipo.vpn.ui.components.AppHeader
import com.usipipo.vpn.ui.components.ConnectButton
import com.usipipo.vpn.ui.components.IconTile
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.theme.AtmospherePhase
import com.usipipo.vpn.ui.theme.LocalAtmosphere
import com.usipipo.vpn.ui.theme.UsipipoTheme
import com.usipipo.vpn.ui.theme.atmospherePhase
import com.usipipo.vpn.ui.theme.rememberReduceMotion

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onImport: () -> Unit,
    onOpenConfigs: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.connectSelected() else vm.onPermissionDenied()
    }

    val messageText = state.message?.let { stringResource(messageRes(it)) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            vm.dismissMessage()
        }
    }

    fun onToggle() {
        if (state.tunnel.isActive) {
            vm.disconnect()
            return
        }
        val intent = VpnService.prepare(context)
        if (intent == null) vm.connectSelected() else permissionLauncher.launch(intent)
    }

    val reduceMotion = rememberReduceMotion()
    val phase = atmospherePhase(state.tunnel)
    val selected = state.configs.firstOrNull { it.id == state.selectedId }

    HomeBackground(phase = phase, reduceMotion = reduceMotion) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppHeader()
                ConnectSection(
                    tunnel = state.tunnel,
                    enabled = selected != null,
                    onClick = { onToggle() },
                )
                if (selected == null) {
                    EmptyState(onImport = onImport)
                } else {
                    ProfileCard(config = selected, onClick = onOpenConfigs)
                }
            }
        }
    }
}

@Composable
private fun HomeBackground(
    phase: AtmospherePhase,
    reduceMotion: Boolean = rememberReduceMotion(),
    content: @Composable () -> Unit,
) {
    val atmosphere = LocalAtmosphere.current.forPhase(phase)
    Box(modifier = Modifier.fillMaxSize()) {
        GradientLayer(top = atmosphere.top, bottom = atmosphere.bottom, reduceMotion = reduceMotion)
        content()
    }
}

@Composable
private fun GradientLayer(top: Color, bottom: Color, reduceMotion: Boolean) {
    val spec = tween<Color>(durationMillis = 600)
    val animatedTop = if (reduceMotion) top else animateColorAsState(top, spec, label = "atmosphereTop").value
    val animatedBottom = if (reduceMotion) bottom else animateColorAsState(bottom, spec, label = "atmosphereBottom").value
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedTop, animatedBottom))),
    )
}

@StringRes
private fun messageRes(message: HomeMessage): Int = when (message) {
    HomeMessage.PermissionDenied -> R.string.msg_permission_denied
    HomeMessage.ConfigUnreadable -> R.string.msg_config_unreadable
}

@Composable
private fun ConnectSection(tunnel: TunnelState, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConnectButton(state = tunnel, enabled = enabled, onClick = onClick)
        Spacer(Modifier.height(12.dp))
        Text(
            text = statusLabel(tunnel),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (tunnel is TunnelState.Connected) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.tagline_connected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (tunnel is TunnelState.NoResponse) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.hint_no_response,
                    stringResource(R.string.support_email),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun statusLabel(tunnel: TunnelState): String = stringResource(
    when (tunnel) {
        TunnelState.Disconnected -> R.string.status_disconnected
        is TunnelState.Connecting -> R.string.status_connecting
        is TunnelState.Connected -> R.string.status_connected
        is TunnelState.NoResponse -> R.string.status_no_response
        is TunnelState.Failed -> R.string.status_failed
    },
)

@Composable
private fun EmptyState(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_empty_shield),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onImport) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.import_action))
        }
    }
}

@Composable
private fun ProfileCard(config: SavedConfig, onClick: () -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(R.drawable.ic_server)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.protocol_wireguard),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
                Icon(
                KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Fondo tranquilo")
@Preview(name = "Fondo tranquilo oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFondoTranquilo() {
    UsipipoTheme {
        Surface {
            HomeBackground(phase = AtmospherePhase.Calm, reduceMotion = true) {
                PreviewBackgroundContent()
            }
        }
    }
}

@Preview(name = "Fondo derivando")
@Preview(name = "Fondo derivando oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFondoDerivando() {
    UsipipoTheme {
        Surface {
            HomeBackground(phase = AtmospherePhase.Drifting, reduceMotion = true) {
                PreviewBackgroundContent()
            }
        }
    }
}

@Preview(name = "Fondo fluyendo")
@Preview(name = "Fondo fluyendo oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFondoFluyendo() {
    UsipipoTheme {
        Surface {
            HomeBackground(phase = AtmospherePhase.Flowing, reduceMotion = true) {
                PreviewBackgroundContent()
            }
        }
    }
}

@Preview(name = "Fondo alerta")
@Preview(name = "Fondo alerta oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFondoAlerta() {
    UsipipoTheme {
        Surface {
            HomeBackground(phase = AtmospherePhase.Alert, reduceMotion = true) {
                PreviewBackgroundContent()
            }
        }
    }
}

@Composable
private fun PreviewBackgroundContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.status_connected),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview(name = "Estado vacío")
@Preview(name = "Estado vacío oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewVacio() {
    UsipipoTheme {
        HomeBackground(phase = AtmospherePhase.Calm, reduceMotion = true) {
            EmptyState(onImport = {})
        }
    }
}

@Preview(name = "Inicio conectado")
@Preview(name = "Inicio conectado oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewInicioConectado() {
    UsipipoTheme {
        Surface {
            HomeBackground(phase = AtmospherePhase.Flowing, reduceMotion = true) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ConnectSection(tunnel = TunnelState.Connected("a"), enabled = true, onClick = {})
                    ProfileCard(config = SavedConfig("a", "client-mowgli"), onClick = {})
                }
            }
        }
    }
}
