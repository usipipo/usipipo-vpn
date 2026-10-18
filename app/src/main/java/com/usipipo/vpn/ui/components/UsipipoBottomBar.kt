package com.usipipo.vpn.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.nav.MainTab

/** Barra inferior con las cuatro pestañas principales. */
@Composable
fun UsipipoBottomBar(
    tab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = tab == MainTab.Home,
            onClick = { onTabClick(MainTab.Home) },
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_configs)) },
            selected = tab == MainTab.Configs,
            onClick = { onTabClick(MainTab.Configs) },
        )

        NavigationBarItem(
            icon = { Icon(painterResource(R.drawable.ic_import), contentDescription = null) },
            label = { Text(stringResource(R.string.nav_import)) },
            selected = tab == MainTab.Import,
            onClick = { onTabClick(MainTab.Import) },
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Build, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_diagnostics)) },
            selected = tab == MainTab.Diagnostics,
            onClick = { onTabClick(MainTab.Diagnostics) },
        )
    }
}
