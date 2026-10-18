package com.usipipo.vpn.ui.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import android.content.res.Configuration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import com.usipipo.vpn.ui.theme.UsipipoTheme
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.AppHeader
import com.usipipo.vpn.ui.components.IconTile
import com.usipipo.vpn.ui.components.UsipipoCard

@Composable
fun ImportScreen(vm: ImportViewModel, onDone: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(ui.done) {
        if (ui.done) {
            vm.reset()
            onDone()
        }
    }

    // Atrás en el paso «Nombre» cancela el paso; en «Elegir», lo gestiona la navegación.
    BackHandler(enabled = ui.stage == ImportStage.Name) { vm.reset() }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) vm.onFilePicked(context.contentResolver, uri)
    }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { vm.onQrScanned(it) }
    }
    val scanPrompt = stringResource(R.string.scan_prompt)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader()
            Text(
                text = stringResource(R.string.import_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            when (ui.stage) {
                ImportStage.Choose -> {
                    ImportOptionCard(
                        icon = R.drawable.ic_qr,
                        title = R.string.scan_qr,
                        subtitle = R.string.import_qr_subtitle,
                        onClick = {
                            scanner.launch(
                                ScanOptions()
                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt(scanPrompt)
                                    .setBeepEnabled(false)
                                    .setOrientationLocked(false)
                                    .setCaptureActivity(PortraitCaptureActivity::class.java),
                            )
                        },
                    )
                    ImportOptionCard(
                        icon = R.drawable.ic_import,
                        title = R.string.import_file_title,
                        subtitle = R.string.import_file_subtitle,
                        // Los .conf no tienen un tipo MIME fiable: se permite cualquier archivo.
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                    )

                    ui.error?.let { error ->
                        Text(
                            text = stringResource(errorRes(error)),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                }

                ImportStage.Name -> {
                    OutlinedTextField(
                        value = ui.name,
                        onValueChange = vm::onNameChange,
                        label = { Text(stringResource(R.string.name_label)) },
                        singleLine = true,
                        isError = ui.error != null,
                        supportingText = if (ui.error != null) {
                            { Text(stringResource(errorRes(ui.error!!))) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = vm::save,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) { Text(stringResource(R.string.save)) }
                    TextButton(
                        onClick = vm::reset,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes subtitle: Int,
    onClick: () -> Unit,
) {
    UsipipoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun errorRes(error: ImportError): Int = when (error) {
    ImportError.InvalidConfig -> R.string.err_invalid_config
    ImportError.TooLarge -> R.string.err_too_large
    ImportError.ReadFailed -> R.string.err_read_failed
    ImportError.NameEmpty -> R.string.err_name_empty
    ImportError.Duplicate -> R.string.err_name_duplicate
    ImportError.SaveFailed -> R.string.err_save_failed
}

@Preview(name = "Tarjeta de importar")
@Preview(name = "Tarjeta de importar oscura", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewImportar() {
    UsipipoTheme {
        Surface {
            ImportOptionCard(
                icon = R.drawable.ic_import,
                title = R.string.import_file_title,
                subtitle = R.string.import_file_subtitle,
                onClick = {},
            )
        }
    }
}
