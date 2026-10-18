package com.usipipo.vpn.ui.home

import android.app.Activity
import android.content.res.Configuration
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usipipo.vpn.R
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.domain.isActive
import com.usipipo.vpn.ui.components.AppHeader
import com.usipipo.vpn.ui.components.ConnectButton
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
        // Android pide el permiso de VPN la primera vez; después prepare() devuelve null.
        val intent = VpnService.prepare(context)
        if (intent == null) vm.connectSelected() else permissionLauncher.launch(intent)
    }

    val reduceMotion = rememberReduceMotion()
    val phase = atmospherePhase(state.tunnel)
    HomeBackground(phase = phase, reduceMotion = reduceMotion) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {             SnackbarHost(snackbar) },
            contentWindowInsets = WindowInsets.statusBars,
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { AppHeader() }
                item {
                    ConnectSection(
                        tunnel = state.tunnel,
                        enabled = state.selectedId != null,
                        onClick = { onToggle() },
                    )
                }
                item {
                    ConfigsHeader(onImport = onImport, showImport = state.configs.isNotEmpty())
                }

                if (state.configs.isEmpty()) {
                    item { EmptyState(onImport = onImport) }
                } else {
                    items(state.configs, key = { it.id }) { config ->
                        ConfigCard(
                            config = config,
                            selected = config.id == state.selectedId,
                            selectEnabled = !state.tunnel.isActive,
                            onSelect = { vm.select(config.id) },
                            onDeleteRequest = { },
                        )
                    }
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
        if (tunnel is TunnelState.NoResponse) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.hint_no_response,
                    stringResource(R.string.support_email),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
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
private fun ConfigsHeader(onImport: () -> Unit, showImport: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.configs_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (showImport) {
            FilledTonalButton(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_action))
            }
        }
    }
}

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
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onImport) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.import_action))
        }
    }
}

@Composable
private fun ConfigCard(
    config: SavedConfig,
    selected: Boolean,
    selectEnabled: Boolean,
    onSelect: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val deleteLabel = stringResource(R.string.cd_delete, config.name)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
            false // la tarjeta vuelve a su sitio; el borrado real pasa por el diálogo
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 24.dp),
                )
            }
        },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .selectable(
                        selected = selected,
                        enabled = selectEnabled,
                        role = Role.RadioButton,
                        onClick = onSelect,
                    )
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(label = deleteLabel) { onDeleteRequest(); true },
                        )
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_server),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
                if (selected) {
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
            }
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
