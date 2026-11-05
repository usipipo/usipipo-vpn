package com.usipipo.vpn.ui.settings.notifications

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.theme.UsipipoTheme

/**
 * Ajustes de notificaciones: interruptor general, un interruptor por tipo de aviso,
 * el aviso obligatorio de la notificación del servicio en primer plano y la ruta a
 * los ajustes de notificaciones si el permiso está denegado.
 *
 * Recibe valores planos para poder previsualizarla sin ciclo de vida.
 */
@Composable
fun NotificationsScreen(
    notificationsEnabled: Boolean,
    trafficEnabled: Boolean,
    disconnectEnabled: Boolean,
    updatesEnabled: Boolean,
    notificationsGranted: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onTrafficChange: (Boolean) -> Unit,
    onDisconnectChange: (Boolean) -> Unit,
    onUpdatesChange: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBack: () -> Unit,
) {
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
            Cabecera(onBack)
            if (!notificationsGranted) {
                AvisoPermiso(onOpenNotificationSettings)
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                FilaInterruptor(
                    titulo = stringResource(R.string.notifications_master),
                    resumen = null,
                    activo = notificationsEnabled,
                    onCambio = onNotificationsChange,
                )
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    FilaInterruptor(
                        titulo = stringResource(R.string.notifications_traffic),
                        resumen = stringResource(R.string.notifications_traffic_summary),
                        activo = trafficEnabled,
                        habilitada = notificationsEnabled,
                        onCambio = onTrafficChange,
                    )
                    FilaInterruptor(
                        titulo = stringResource(R.string.notifications_disconnect),
                        resumen = stringResource(R.string.notifications_disconnect_summary),
                        activo = disconnectEnabled,
                        habilitada = notificationsEnabled,
                        onCambio = onDisconnectChange,
                    )
                    FilaInterruptor(
                        titulo = stringResource(R.string.notifications_updates),
                        resumen = stringResource(R.string.notifications_updates_summary),
                        activo = updatesEnabled,
                        habilitada = notificationsEnabled,
                        onCambio = onUpdatesChange,
                    )
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.notifications_fgs_banner),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun Cabecera(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        Text(
            text = stringResource(R.string.notifications_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** El permiso denegado no rompe nada: se avisa y se da la ruta a los ajustes. */
@Composable
private fun AvisoPermiso(onOpenNotificationSettings: () -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.notifications_permission_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onOpenNotificationSettings) {
                Text(stringResource(R.string.notifications_permission_action))
            }
        }
    }
}

/**
 * Fila con interruptor. El interruptor general no lleva resumen; los de cada tipo
 * se deshabilitan mientras el general esté apagado.
 */
@Composable
private fun FilaInterruptor(
    titulo: String,
    resumen: String?,
    activo: Boolean,
    habilitada: Boolean = true,
    onCambio: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = activo,
                enabled = habilitada,
                role = Role.Switch,
                onValueChange = onCambio,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            resumen?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        // El toggle lo gestiona la fila entera (accesibilidad): el Switch es solo visual.
        Switch(
            checked = activo,
            onCheckedChange = null,
            enabled = habilitada,
        )
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificationsScreenPreview() {
    UsipipoTheme {
        NotificationsScreen(
            notificationsEnabled = true,
            trafficEnabled = true,
            disconnectEnabled = false,
            updatesEnabled = true,
            notificationsGranted = true,
            onNotificationsChange = {},
            onTrafficChange = {},
            onDisconnectChange = {},
            onUpdatesChange = {},
            onOpenNotificationSettings = {},
            onBack = {},
        )
    }
}
