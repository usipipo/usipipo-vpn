package com.usipipo.vpn.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.update.UpdateStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onDiagnostics: () -> Unit,
    onCheckUpdates: suspend () -> UpdateStatus,
) {
    val context = LocalContext.current
    val version = remember { appVersion(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var checking by remember { mutableStateOf(false) }

    val upToDateMessage = stringResource(R.string.update_status_ok)
    val postponedMessage = stringResource(R.string.update_status_postponed)
    val errorMessage = stringResource(R.string.update_status_error)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                }
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(96.dp),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.version_label, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionHeader(text = stringResource(R.string.support_label))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.support_email),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FilledTonalButton(
                        onClick = { openSupportEmail(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) { Text(stringResource(R.string.send_email)) }
                }
            }

            SectionHeader(text = stringResource(R.string.about_updates_title))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FilledTonalButton(
                        onClick = {
                            checking = true
                            scope.launch {
                                val status = try {
                                    onCheckUpdates()
                                } catch (e: CancellationException) {
                                    // Cancelar no es un fallo: se relanza sin alertar al usuario,
                                    // igual que UpdateChecker.check y AppContainer.checkForUpdates.
                                    checking = false
                                    throw e
                                } catch (e: Exception) {
                                    AppLog.w("Actualizar", "La comprobación manual de actualizaciones falló", e)
                                    UpdateStatus.Failed
                                }
                                checking = false
                                val message = when (status) {
                                    // Disponible: lo avisa la ventana de actualización.
                                    is UpdateStatus.UpdateAvailable -> null
                                    UpdateStatus.UpToDate -> upToDateMessage
                                    UpdateStatus.Postponed -> postponedMessage
                                    UpdateStatus.SkippedThrottled -> upToDateMessage
                                    UpdateStatus.Failed -> errorMessage
                                }
                                if (message != null) snackbar.showSnackbar(message)
                            }
                        },
                        enabled = !checking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        if (checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.update_checking))
                        } else {
                            Text(stringResource(R.string.about_check_updates))
                        }
                    }
                }
            }

            SectionHeader(text = stringResource(R.string.about_advanced_title))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedButton(
                        onClick = onDiagnostics,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) { Text(stringResource(R.string.diag_open)) }
                }
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

private fun openSupportEmail(context: Context) {
    val address = context.getString(R.string.support_email)
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Sin app de correo instalada: el correo sigue visible en pantalla.
    }
}

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: "—"
