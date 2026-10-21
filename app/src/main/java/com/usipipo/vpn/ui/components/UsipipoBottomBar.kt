package com.usipipo.vpn.ui.components

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.nav.MainTab
import com.usipipo.vpn.ui.theme.LocalBottomBarColors
import com.usipipo.vpn.ui.theme.UsipipoTheme

/** Barra inferior con las cuatro pestañas principales. */
@Composable
fun UsipipoBottomBar(
    tab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBottomBarColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.active,
        selectedTextColor = colors.active,
        // La pila de fondo de Material 3 no aparece en el mockup.
        indicatorColor = Color.Transparent,
        unselectedIconColor = colors.inactive,
        unselectedTextColor = colors.inactive,
    )
    NavigationBar(
        modifier = modifier,
        containerColor = colors.container,
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = if (tab == MainTab.Home) colors.active else colors.inactive,
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_home),
                    color = if (tab == MainTab.Home) colors.active else colors.inactive,
                )
            },
            selected = tab == MainTab.Home,
            onClick = { onTabClick(MainTab.Home) },
            colors = itemColors,
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (tab == MainTab.Configs) colors.active else colors.inactive,
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_configs),
                    color = if (tab == MainTab.Configs) colors.active else colors.inactive,
                )
            },
            selected = tab == MainTab.Configs,
            onClick = { onTabClick(MainTab.Configs) },
            colors = itemColors,
        )

        NavigationBarItem(
            icon = {
                Icon(
                    painterResource(R.drawable.ic_import),
                    contentDescription = null,
                    tint = if (tab == MainTab.Import) colors.active else colors.inactive,
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_import),
                    color = if (tab == MainTab.Import) colors.active else colors.inactive,
                )
            },
            selected = tab == MainTab.Import,
            onClick = { onTabClick(MainTab.Import) },
            colors = itemColors,
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = if (tab == MainTab.Diagnostics) colors.active else colors.inactive,
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.nav_diagnostics),
                    color = if (tab == MainTab.Diagnostics) colors.active else colors.inactive,
                )
            },
            selected = tab == MainTab.Diagnostics,
            onClick = { onTabClick(MainTab.Diagnostics) },
            colors = itemColors,
        )
    }
}

@Preview(name = "Barra inferior")
@Preview(name = "Barra inferior oscura", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUsipipoBottomBar() {
    UsipipoTheme {
        UsipipoBottomBar(tab = MainTab.Home, onTabClick = {})
    }
}
