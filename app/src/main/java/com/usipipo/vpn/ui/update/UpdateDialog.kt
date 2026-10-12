package com.usipipo.vpn.ui.update

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.update.ReleaseInfo

/**
 * Ventana de actualización: muestra la versión nueva y deja elegir entre descargarla
 * o posponer el aviso. Hereda los colores del tema (claro y oscuro).
 */
@Composable
fun UpdateDialog(
    info: ReleaseInfo,
    onDownload: () -> Unit,
    onPostpone: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onPostpone,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_update),
                contentDescription = stringResource(R.string.update_cd_icon),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.update_title),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.update_body, info.version.toString()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Text(stringResource(R.string.update_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onPostpone) {
                Text(stringResource(R.string.update_later))
            }
        },
    )
}
