package com.usipipo.vpn.ui.settings.security

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
 * Subpantalla de Seguridad: desbloqueo biométrico, estado de siempre-on y lista global
 * de apps excluidas del túnel.
 *
 * Recibe el estado y los callbacks, no el ViewModel, para poder previsualizarla sin
 * ciclo de vida.
 */
@Composable
fun SecurityScreen(
    biometricEnabled: Boolean,
    alwaysOn: Boolean?,
    lockdown: Boolean?,
    excludedApps: List<String>,
    mensaje: String?,
    onMensajeConsumido: () -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onRemoveApp: (String) -> Unit,
    onOpenVpnSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            avisos.showSnackbar(mensaje)
            onMensajeConsumido()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(avisos) },
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

            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    FilaBiometrico(biometricEnabled, onBiometricChange)
                    TextoGrupo(stringResource(R.string.security_biometric_body))
                }
            }

            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TextoGrupo(stringResource(R.string.security_kill_switch))
                    FilaEstado(
                        titulo = stringResource(R.string.security_always_on),
                        activo = alwaysOn,
                    )
                    FilaEstado(
                        titulo = stringResource(R.string.security_lockdown),
                        activo = lockdown,
                    )
                    TextoGrupo(stringResource(R.string.security_kill_switch_body))
                    TextButton(onClick = onOpenVpnSettings) {
                        Text(stringResource(R.string.security_open_vpn_settings))
                    }
                }
            }

            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    TextoGrupo(stringResource(R.string.security_excluded_apps))
                    if (excludedApps.isEmpty()) {
                        TextoGrupo(stringResource(R.string.security_excluded_empty))
                    } else {
                        excludedApps.forEach { paquete ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = paquete,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { onRemoveApp(paquete) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(R.string.security_excluded_remove),
                                    )
                                }
                            }
                        }
                    }
                }
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
            text = stringResource(R.string.security_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun FilaBiometrico(activado: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = activado, role = Role.Switch, onValueChange = onChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.security_biometric),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        // El toggle lo gestiona la fila entera (accesibilidad): el Switch es solo visual.
        Switch(checked = activado, onCheckedChange = null)
    }
}

/** Fila informativa de estado del sistema. `null` = no se puede leer en esta versión. */
@Composable
private fun FilaEstado(titulo: String, activo: Boolean?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = when (activo) {
                true -> stringResource(R.string.security_state_on)
                false -> stringResource(R.string.security_state_off)
                null -> stringResource(R.string.security_state_unknown)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TextoGrupo(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Preview(name = "Seguridad claro", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Seguridad oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityScreenPreview() {
    UsipipoTheme {
        SecurityScreen(
            biometricEnabled = true,
            alwaysOn = true,
            lockdown = false,
            excludedApps = listOf("com.app.excluida", "com.otra.app"),
            mensaje = null,
            onMensajeConsumido = {},
            onBiometricChange = {},
            onRemoveApp = {},
            onOpenVpnSettings = {},
            onBack = {},
        )
    }
}


