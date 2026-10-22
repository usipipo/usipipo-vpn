package com.usipipo.vpn.ui.components

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.nav.MainTab
import com.usipipo.vpn.ui.theme.BottomBarColors
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
    NavigationBar(
        modifier = modifier,
        containerColor = colors.container,
    ) {
        BottomBarItem(
            tab = MainTab.Home,
            current = tab,
            colors = colors,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = stringResource(R.string.nav_home),
            onClick = { onTabClick(MainTab.Home) },
        )
        BottomBarItem(
            tab = MainTab.Configs,
            current = tab,
            colors = colors,
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = stringResource(R.string.nav_configs),
            onClick = { onTabClick(MainTab.Configs) },
        )
        BottomBarItem(
            tab = MainTab.Import,
            current = tab,
            colors = colors,
            icon = { Icon(painterResource(R.drawable.ic_import), contentDescription = null) },
            label = stringResource(R.string.nav_import),
            onClick = { onTabClick(MainTab.Import) },
        )
        BottomBarItem(
            tab = MainTab.Diagnostics,
            current = tab,
            colors = colors,
            icon = { Icon(Icons.Default.Build, contentDescription = null) },
            label = stringResource(R.string.nav_diagnostics),
            onClick = { onTabClick(MainTab.Diagnostics) },
        )
    }
}

/**
 * Un elemento de la barra. El tinte del icono y el color de la etiqueta se fijan a mano
 * ademas de pasarlos a Material, para que el estado activo no dependa de como los envuelva.
 */
@Composable
private fun BottomBarItem(
    tab: MainTab,
    current: MainTab,
    colors: BottomBarColors,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    val selected = tab == current
    val iconColor = if (selected) colors.active else colors.inactive
    val labelColor = if (selected) colors.activeLabel else colors.inactive
    NavigationBarItem(
        icon = {
            CompositionLocalProvider(LocalContentColor provides iconColor) { icon() }
        },
        label = { Text(text = label, color = labelColor) },
        selected = selected,
        onClick = onClick,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.active,
            selectedTextColor = colors.activeLabel,
            // La pila de fondo de Material 3 no aparece en el mockup.
            indicatorColor = Color.Transparent,
            unselectedIconColor = colors.inactive,
            unselectedTextColor = colors.inactive,
        ),
    )
}

@Preview(name = "Barra inferior")
@Preview(name = "Barra inferior oscura", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewUsipipoBottomBar() {
    UsipipoTheme {
        Surface {
            UsipipoBottomBar(tab = MainTab.Home, onTabClick = {})
        }
    }
}
