package com.usipipo.vpn.ui.nav

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.usipipo.vpn.R

/** Las cuatro pestañas de la barra inferior. */
enum class MainTab {
    Home,
    Configs,
    Import,
    Diagnostics,
    ;

    companion object {
        fun fromKey(key: String?): MainTab = entries.firstOrNull { it.name == key } ?: MainTab.Home
    }
}

/** Destino del botón Atrás: Inicio desde cualquier otra pestaña; null en Inicio (sale de la app). */
fun backTarget(tab: MainTab): MainTab? = if (tab == MainTab.Home) null else MainTab.Home

/** Icono propio de la barra inferior para cada pestaña. */
internal val MainTab.iconRes: Int
    @DrawableRes get() = when (this) {
        MainTab.Home -> R.drawable.ic_nav_home
        MainTab.Configs -> R.drawable.ic_nav_configs
        MainTab.Import -> R.drawable.ic_nav_import
        MainTab.Diagnostics -> R.drawable.ic_nav_diagnostics
    }

/** Etiqueta de la barra inferior para cada pestaña. */
internal val MainTab.labelRes: Int
    @StringRes get() = when (this) {
        MainTab.Home -> R.string.nav_home
        MainTab.Configs -> R.string.nav_configs
        MainTab.Import -> R.string.nav_import
        MainTab.Diagnostics -> R.string.nav_diagnostics
    }
