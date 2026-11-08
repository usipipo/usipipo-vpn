package com.usipipo.vpn.ui.settings.storage

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.SettingsRow
import com.usipipo.vpn.ui.components.UsipipoCard
import com.usipipo.vpn.ui.theme.UsipipoTheme

/**
 * Almacenamiento: cuánto ocupa la app y cómo liberarlo.
 *
 * Recibe valores planos para poder previsualizarla sin ciclo de vida. Los tres
 * diálogos (registro, caché y borrado total) viven aquí porque son decisiones del
 * usuario, no estado: cada uno confirma antes de tocar nada.
 */
@Composable
fun StorageScreen(
    profilesBytes: Long,
    logBytes: Long,
    tempBytes: Long,
    busy: Boolean,
    wipeWord: String,
    wipeReady: Boolean,
    message: String?,
    onClearLog: () -> Unit,
    onClearCache: () -> Unit,
    onWipeWordChange: (String) -> Unit,
    onWipeAll: () -> Unit,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
) {
    var borrarRegistro by remember { mutableStateOf(false) }
    var limpiarCache by remember { mutableStateOf(false) }
    var borrarTodo by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    if (borrarRegistro) {
        ConfirmDialog(
            title = stringResource(R.string.storage_clear_log_title),
            body = stringResource(R.string.storage_clear_log_body),
            confirmLabel = stringResource(R.string.storage_clear_log),
            enabled = !busy,
            onConfirm = {
                borrarRegistro = false
                onClearLog()
            },
            onDismiss = {
                borrarRegistro = false
                onMessageShown()
            },
        )
    }
    if (limpiarCache) {
        ConfirmDialog(
            title = stringResource(R.string.storage_clear_cache_title),
            body = stringResource(R.string.storage_clear_cache_body),
            confirmLabel = stringResource(R.string.storage_clear_cache),
            enabled = !busy,
            onConfirm = {
                limpiarCache = false
                onClearCache()
            },
            onDismiss = {
                limpiarCache = false
                onMessageShown()
            },
        )
    }
    if (borrarTodo) {
        WipeDialog(
            word = wipeWord,
            enabled = wipeReady && !busy,
            onWordChange = onWipeWordChange,
            onConfirm = {
                borrarTodo = false
                confirmarBorrado = true
            },
            onDismiss = {
                borrarTodo = false
                onMessageShown()
            },
        )
    }
    if (confirmarBorrado) {
        ConfirmDialog(
            title = stringResource(R.string.storage_wipe_title),
            body = stringResource(R.string.storage_wipe_body),
            confirmLabel = stringResource(R.string.storage_wipe_confirm),
            enabled = !busy,
            onConfirm = {
                confirmarBorrado = false
                onWipeAll()
            },
            onDismiss = {
                confirmarBorrado = false
                onMessageShown()
            },
        )
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
                        text = stringResource(R.string.storage_used),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    FilaTamano(
                        etiqueta = stringResource(R.string.storage_profiles),
                        valor = StorageFormat.readable(profilesBytes),
                    )
                    FilaTamano(
                        etiqueta = stringResource(R.string.storage_log),
                        valor = StorageFormat.readable(logBytes),
                    )
                    FilaTamano(
                        etiqueta = stringResource(R.string.storage_temp),
                        valor = StorageFormat.readable(tempBytes),
                    )
                    FilaTamano(
                        etiqueta = stringResource(R.string.storage_total),
                        valor = StorageFormat.readable(
                            StorageFormat.total(profilesBytes, logBytes, tempBytes),
                        ),
                        destacada = true,
                    )
                    if (profilesBytes + logBytes + tempBytes == 0L) {
                        Text(
                            text = stringResource(R.string.storage_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsRow(
                        iconRes = R.drawable.ic_delete,
                        title = R.string.storage_clear_log,
                        onClick = { borrarRegistro = true },
                    )
                    SettingsRow(
                        iconRes = R.drawable.ic_update,
                        title = R.string.storage_clear_cache,
                        onClick = { limpiarCache = true },
                    )
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(R.string.storage_wipe),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(R.string.storage_wipe_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    OutlinedButton(onClick = { borrarTodo = true }, enabled = !busy) {
                        Text(stringResource(R.string.storage_wipe))
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.storage_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Categoría del resumen con su tamaño a la derecha. */
@Composable
private fun FilaTamano(etiqueta: String, valor: String, destacada: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacada) FontWeight.Bold else null,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Confirmación simple: avisa de qué se va a borrar y espera el sí. */
@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * Segunda confirmación del borrado total: hay que escribir la palabra clave para
 * poder darle al botón. La palabra la guarda el ViewModel, no la pantalla, para que
 * sobreviva a un cambio de configuración.
 */
@Composable
private fun WipeDialog(
    word: String,
    enabled: Boolean,
    onWordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.storage_wipe_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.storage_wipe_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = word,
                    onValueChange = onWordChange,
                    label = { Text(stringResource(R.string.storage_wipe_word_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) {
                Text(stringResource(R.string.storage_wipe_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StorageScreenPreview() {
    UsipipoTheme {
        StorageScreen(
            profilesBytes = 1_536,
            logBytes = 204_800,
            tempBytes = 0,
            busy = false,
            wipeWord = "",
            wipeReady = false,
            message = null,
            onClearLog = {},
            onClearCache = {},
            onWipeWordChange = {},
            onWipeAll = {},
            onMessageShown = {},
            onBack = {},
        )
    }
}
