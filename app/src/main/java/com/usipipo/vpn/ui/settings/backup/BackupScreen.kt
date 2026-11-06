package com.usipipo.vpn.ui.settings.backup

import android.content.res.Configuration
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.theme.UsipipoTheme
import java.text.DateFormat
import java.util.Date

/**
 * Ajustes de copia de seguridad: última copia, exportar todo (zip cifrado), exportar
 * un perfil en claro e importar con resolución de duplicados.
 *
 * Recibe valores planos para poder previsualizarla sin ciclo de vida. Los diálogos
 * (contraseña, aviso de claves privadas, importación) viven aquí porque son decisiones
 * del usuario, no estado.
 */
@Composable
fun BackupScreen(
    lastBackupAt: Long,
    profiles: List<SavedConfig>,
    busy: Boolean,
    conflicts: List<Pair<String, String>>,
    message: String?,
    profileWarningId: String?,
    importPasswordUriVisible: Boolean,
    onExportAll: (String) -> Unit,
    onExportProfile: (String) -> Unit,
    onExportProfileConfirm: () -> Unit,
    onExportProfileCancel: () -> Unit,
    onImport: () -> Unit,
    onImportPassword: (String) -> Unit,
    onImportPasswordCancel: () -> Unit,
    onResolveConflict: (DuplicateDecision) -> Unit,
    onCancelImport: () -> Unit,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
) {
    if (importPasswordUriVisible) {
        PasswordDialog(
            title = stringResource(R.string.backup_import_password_title),
            warning = null,
            confirmLabel = stringResource(R.string.backup_import),
            onConfirm = onImportPassword,
            onDismiss = onImportPasswordCancel,
        )
    }
    profileWarningId?.let { id ->
        WarningKeysDialog(onConfirm = onExportProfileConfirm, onDismiss = onExportProfileCancel)
    }

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
            UltimaCopia(lastBackupAt)
            message?.let { texto ->
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.backup_export_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.backup_password_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.padding(vertical = 4.dp))
                    ExportarTodo(busy, onExportAll)
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.backup_profile_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    profiles.forEach { profile ->
                        FilaPerfil(profile, enabled = !busy, onExport = { onExportProfile(profile.id) })
                    }
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(onClick = onImport, enabled = !busy) {
                        Text(stringResource(R.string.backup_import_action))
                    }
                }
            }
            if (conflicts.isNotEmpty()) {
                Conflictos(conflicts, onResolveConflict, onCancelImport)
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
            text = stringResource(R.string.backup_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun UltimaCopia(lastBackupAt: Long) {
    val fecha = if (lastBackupAt > 0L) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastBackupAt))
    } else {
        stringResource(R.string.backup_none)
    }
    Text(
        text = stringResource(R.string.backup_last, fecha),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Botón que abre el diálogo de contraseña y luego el selector de documentos. */
@Composable
private fun ExportarTodo(busy: Boolean, onExportAll: (String) -> Unit) {
    var pedirClave by remember { mutableStateOf(false) }
    if (pedirClave) {
        PasswordDialog(
            title = stringResource(R.string.backup_export_password_title),
            warning = stringResource(R.string.backup_password_warning),
            confirmLabel = stringResource(R.string.backup_export),
            onConfirm = { clave ->
                pedirClave = false
                onExportAll(clave)
            },
            onDismiss = { pedirClave = false },
        )
    }
    OutlinedButton(onClick = { pedirClave = true }, enabled = !busy) {
        Text(stringResource(R.string.backup_export_action))
    }
}

@Composable
private fun FilaPerfil(profile: SavedConfig, enabled: Boolean, onExport: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Es una acción, no un interruptor: toggleable anunciaría «casilla sin
            // marcar» a TalkBack y dispararía también al desmarcar.
            .clickable(enabled = enabled, onClick = onExport)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Conflictos(
    conflicts: List<Pair<String, String>>,
    onResolve: (DuplicateDecision) -> Unit,
    onCancel: () -> Unit,
) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_conflicts_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.backup_conflicts_body, conflicts.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.backup_conflict_item, conflicts.first().first),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onResolve(DuplicateDecision.Overwrite) }) {
                    Text(stringResource(R.string.backup_overwrite))
                }
                OutlinedButton(onClick = { onResolve(DuplicateDecision.KeepBoth) }) {
                    Text(stringResource(R.string.backup_keep_both))
                }
                OutlinedButton(onClick = { onResolve(DuplicateDecision.Skip) }) {
                    Text(stringResource(R.string.backup_skip))
                }
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

/** Contraseña con confirmación; avisa que no hay recuperación. */
@Composable
private fun PasswordDialog(
    title: String,
    warning: String?,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var clave by remember { mutableStateOf("") }
    var repetida by remember { mutableStateOf("") }
    val cuadra = clave.isNotEmpty() && clave == repetida
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (warning != null) {
                    Text(warning, style = MaterialTheme.typography.bodyMedium)
                }
                CampoClave(valor = clave, etiqueta = stringResource(R.string.backup_password), onValor = { clave = it })
                CampoClave(valor = repetida, etiqueta = stringResource(R.string.backup_password_confirm), onValor = { repetida = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(clave) }, enabled = cuadra) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun WarningKeysDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_profile_keys_title)) },
        text = { Text(stringResource(R.string.backup_profile_keys_warning)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.backup_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun CampoClave(valor: String, etiqueta: String, onValor: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.material3.OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        label = { Text(etiqueta) },
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(
                    text = stringResource(
                        if (visible) R.string.backup_hide else R.string.backup_show,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BackupScreenPreview() {
    UsipipoTheme {
        BackupScreen(
            lastBackupAt = 1_700_000_000_000L,
            profiles = listOf(SavedConfig(id = "c1", name = "Casa"), SavedConfig(id = "c2", name = "Trabajo")),
            busy = false,
            conflicts = listOf("Casa" to "Casa copia"),
            message = null,
            profileWarningId = null,
            importPasswordUriVisible = false,
            onExportAll = {},
            onExportProfile = {},
            onExportProfileConfirm = {},
            onExportProfileCancel = {},
            onImport = {},
            onImportPassword = {},
            onImportPasswordCancel = {},
            onResolveConflict = {},
            onCancelImport = {},
            onMessageShown = {},
            onBack = {},
        )
    }
}
