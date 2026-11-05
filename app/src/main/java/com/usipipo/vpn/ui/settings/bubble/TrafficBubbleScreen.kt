package com.usipipo.vpn.ui.settings.bubble

import android.content.res.Configuration
import androidx.annotation.StringRes
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
 * Ajustes de la burbuja de tráfico: interruptor, permiso de superposición y
 * explicación de cuándo se dibuja.
 *
 * Recibe valores planos para poder previsualizarla sin ciclo de vida.
 */
@Composable
fun TrafficBubbleScreen(
    bubbleEnabled: Boolean,
    overlayGranted: Boolean,
    onBubbleChange: (Boolean) -> Unit,
    onGrantOverlay: () -> Unit,
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
            if (bubbleEnabled && !overlayGranted) {
                AvisoPermiso(onGrantOverlay)
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = bubbleEnabled, role = Role.Switch, onValueChange = onBubbleChange)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bubble_switch),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(resumenEstado(bubbleEnabled, overlayGranted)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // El toggle lo gestiona la fila entera (accesibilidad): el Switch es solo visual.
                    Switch(checked = bubbleEnabled, onCheckedChange = null)
                }
            }
            UsipipoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.bubble_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/** Estado que se ve bajo el interruptor. */
@StringRes
private fun resumenEstado(bubbleEnabled: Boolean, overlayGranted: Boolean): Int = when {
    bubbleEnabled && !overlayGranted -> R.string.bubble_state_permission_denied
    bubbleEnabled -> R.string.bubble_state_on
    else -> R.string.bubble_state_off
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
            text = stringResource(R.string.bubble_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Sin permiso de superposición la burbuja no se dibuja: se avisa y se da la ruta. */
@Composable
private fun AvisoPermiso(onGrantOverlay: () -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.bubble_permission_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onGrantOverlay) {
                Text(stringResource(R.string.bubble_permission_action))
            }
        }
    }
}

@Preview(name = "Claro", showBackground = true)
@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrafficBubbleScreenPreview() {
    UsipipoTheme {
        TrafficBubbleScreen(
            bubbleEnabled = true,
            overlayGranted = true,
            onBubbleChange = {},
            onGrantOverlay = {},
            onBack = {},
        )
    }
}
