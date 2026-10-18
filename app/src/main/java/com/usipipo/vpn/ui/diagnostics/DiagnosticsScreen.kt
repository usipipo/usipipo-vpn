package com.usipipo.vpn.ui.diagnostics

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.usipipo.vpn.R
import com.usipipo.vpn.diagnostics.DeviceInfo
import com.usipipo.vpn.diagnostics.ReportSender
import com.usipipo.vpn.update.UpdateStatus

@Composable
fun DiagnosticsScreen(vm: DiagnosticsViewModel, onCheckUpdates: suspend () -> UpdateStatus) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmSend by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val noMail = stringResource(R.string.diag_no_mail)
    val reportFailed = stringResource(R.string.diag_report_failed)

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is DiagnosticsEvent.SendReport -> {
                    val version = DeviceInfo.collect(context).versionName
                    val body = context.getString(R.string.report_intro) + "\n\n" +
                        event.infoRows.joinToString("\n") { (label, value) -> "$label: $value" }
                    val intent = ReportSender.emailIntent(
                        context = context,
                        file = event.file,
                        address = context.getString(R.string.support_email),
                        subject = context.getString(R.string.report_subject, version),
                        body = body,
                    )
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        snackbar.showSnackbar(noMail)
                    }
                }

                DiagnosticsEvent.ReportFailed -> snackbar.showSnackbar(reportFailed)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
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
            Text(
                text = stringResource(R.string.diag_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            SectionHeader(stringResource(R.string.diag_info_title))
            InfoCard(rows = ui.infoRows)

            SectionHeader(stringResource(R.string.diag_verbose_title))
            VerboseRow(checked = ui.verbose, onChange = vm::setVerbose)

            SectionHeader(stringResource(R.string.diag_log_title))
            LogCard(
                lines = ui.logLines,
                sizeBytes = ui.logSizeBytes,
                onRefresh = vm::refresh,
            )

            if (ui.crashPending) {
                CrashBanner(onDismiss = vm::dismissCrashNotice)
            }

            Button(
                onClick = { confirmSend = true },
                enabled = !ui.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                if (ui.busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.diag_preparing))
                } else {
                    Text(stringResource(R.string.diag_send))
                }
            }

            OutlinedButton(
                onClick = { confirmClear = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.diag_clear)) }

            AboutSection(onCheckUpdates = onCheckUpdates, snackbar = snackbar)

            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmSend) {
        AlertDialog(
            onDismissRequest = { confirmSend = false },
            title = { Text(stringResource(R.string.diag_send_title)) },
            text = { Text(stringResource(R.string.diag_send_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSend = false
                    vm.sendReport()
                }) { Text(stringResource(R.string.diag_send_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSend = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.diag_clear_title)) },
            text = { Text(stringResource(R.string.diag_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearLog()
                }) { Text(stringResource(R.string.diag_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CrashBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.diag_crash_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onError,
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.diag_crash_dismiss),
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VerboseRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.diag_verbose_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            // El toggle lo gestiona la fila entera (accesibilidad): el Switch es solo visual.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun LogCard(lines: List<String>, sizeBytes: Long, onRefresh: () -> Unit) {
    val scroll = rememberScrollState()
    // Va al final (lo más reciente); scrollTo se limita al máximo real tras medir.
    LaunchedEffect(lines) { scroll.scrollTo(Int.MAX_VALUE) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.diag_log_meta, lines.size, formatSize(sizeBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = onRefresh) { Text(stringResource(R.string.diag_refresh)) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 320.dp)
                    .verticalScroll(scroll),
            ) {
                SelectionContainer {
                    Text(
                        text = if (lines.isEmpty()) stringResource(R.string.diag_log_empty) else lines.joinToString("\n"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes < 1024) "$bytes B" else "${bytes / 1024} KB"
