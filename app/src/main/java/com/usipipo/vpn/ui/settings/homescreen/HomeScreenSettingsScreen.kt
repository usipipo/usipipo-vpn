package com.usipipo.vpn.ui.settings.homescreen

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.UsipipoCard

/**
 * Pantalla inicial y panel: qué muestra el widget y cómo se añaden sus accesos.
 *
 * Recibe valores planos, sin ciclo de vida, para poder previsualizarla.
 */
@Composable
fun HomeScreenSettingsScreen(
    widgetAdded: Boolean,
    pinSupported: Boolean,
    showCountry: Boolean,
    message: String?,
    onShowCountryChange: (Boolean) -> Unit,
    onPinWidget: () -> Unit,
    onAddTile: () -> Unit,
    onOpenQuickSettings: () -> Unit,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(message) {
        if (message != null) onMessageShown()
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
            TarjetaWidget(
                widgetAdded = widgetAdded,
                pinSupported = pinSupported,
                showCountry = showCountry,
                onShowCountryChange = onShowCountryChange,
                onPinWidget = onPinWidget,
            )
            TarjetaMosaico(onAddTile)
            TarjetaAccesos(onOpenQuickSettings)
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
            text = stringResource(R.string.homescreen_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun TarjetaWidget(
    widgetAdded: Boolean,
    pinSupported: Boolean,
    showCountry: Boolean,
    onShowCountryChange: (Boolean) -> Unit,
    onPinWidget: () -> Unit,
) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.homescreen_widget_card),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (widgetAdded) R.string.homescreen_widget_added
                    else R.string.homescreen_widget_not_added,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!widgetAdded) {
                Text(
                    text = stringResource(R.string.quick_access_widget_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = showCountry,
                        role = Role.Switch,
                        onValueChange = onShowCountryChange,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.homescreen_show_country),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.homescreen_show_country_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                // El toggle lo gestiona la fila entera (accesibilidad): el Switch es solo visual.
                Switch(checked = showCountry, onCheckedChange = null)
            }
            Spacer(Modifier.height(8.dp))
            if (pinSupported) {
                OutlinedButton(onClick = onPinWidget) {
                    Text(stringResource(R.string.homescreen_pin_widget))
                }
            }
        }
    }
}

@Composable
private fun TarjetaMosaico(onAddTile: () -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.homescreen_tile_card),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onAddTile) {
                Text(stringResource(R.string.quick_access_add_tile))
            }
            Text(
                text = stringResource(R.string.quick_access_tile_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaAccesos(onOpenQuickSettings: () -> Unit) {
    UsipipoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.homescreen_shortcuts),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.homescreen_shortcuts_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onOpenQuickSettings) {
                Text(stringResource(R.string.homescreen_open_quick_settings))
            }
        }
    }
}