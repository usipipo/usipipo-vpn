package com.usipipo.vpn.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.components.AppHeader
import com.usipipo.vpn.ui.settings.components.SettingsRow

/**
 * Pantalla hub de Ajustes: una fila por sección con el resumen de su valor
 * actual. Las subpantallas de cada sección se activan sección a sección en las
 * fases siguientes.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSectionClick: ((SettingsRoute) -> Unit)? = null,
) {
    val sections = listOf(
        Section(SettingsRoute.Appearance, R.drawable.ic_settings_appearance, R.string.settings_appearance),
        Section(SettingsRoute.Security, R.drawable.ic_shield_check, R.string.settings_security),
        Section(SettingsRoute.HomeScreen, R.drawable.ic_settings_widget, R.string.settings_home_screen),
        Section(SettingsRoute.TrafficBubble, R.drawable.ic_settings_bubble, R.string.settings_traffic_bubble),
        Section(SettingsRoute.Notifications, R.drawable.ic_settings_notifications, R.string.settings_notifications),
        Section(SettingsRoute.Updates, R.drawable.ic_update, R.string.settings_updates),
        Section(SettingsRoute.Backup, R.drawable.ic_settings_backup, R.string.settings_backup),
        Section(SettingsRoute.Storage, R.drawable.ic_settings_storage, R.string.settings_storage),
        Section(SettingsRoute.More, R.drawable.ic_settings_more, R.string.settings_more),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        AppHeader()
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sections, key = { it.route }) { section ->
                SettingsRow(
                    iconRes = section.iconRes,
                    title = section.titleRes,
                    summaryText = viewModel.summary(section.route),
                    onClick = onSectionClick?.let { callback -> { callback(section.route) } },
                )
            }
        }
    }
}

private data class Section(
    val route: SettingsRoute,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
)
