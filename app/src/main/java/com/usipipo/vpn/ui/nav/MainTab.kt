package com.usipipo.vpn.ui.nav

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.usipipo.vpn.R

/** Las cuatro pestañas de la barra inferior. */
enum class MainTab {
    Home,
    Profiles,
    Import,
    Settings,
    ;

    companion object {
        /**
         * Convierte una clave de pestaña en la pestaña correspondiente.
         *
         * No distingue mayúsculas: la clave viaja en un intent y el productor y el consumidor
         * no deben poder discrepar por la caja. Las claves de versiones anteriores
         * ("Configs", "Diagnostics") se redirigen a Perfiles y Ajustes para no romper
         * intents o estados guardados. Una clave desconocida abre Inicio.
         */
        fun fromKey(key: String?): MainTab =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                ?: when (key?.lowercase()) {
                    "configs" -> MainTab.Profiles
                    "diagnostics" -> MainTab.Settings
                    else -> MainTab.Home
                }
    }
}

/** Destino del botón Atrás: Inicio desde cualquier otra pestaña; null en Inicio (sale de la app). */
fun backTarget(tab: MainTab): MainTab? = if (tab == MainTab.Home) null else MainTab.Home

/** Icono propio de la barra inferior para cada pestaña. */
internal val MainTab.iconRes: Int
    @DrawableRes get() = when (this) {
        MainTab.Home -> R.drawable.ic_nav_home
        MainTab.Profiles -> R.drawable.ic_nav_profiles
        MainTab.Import -> R.drawable.ic_nav_import
        MainTab.Settings -> R.drawable.ic_nav_settings
    }

/** Etiqueta de accesibilidad y título de pantalla de cada pestaña. */
internal val MainTab.labelRes: Int
    @StringRes get() = when (this) {
        MainTab.Home -> R.string.nav_home
        MainTab.Profiles -> R.string.nav_profiles
        MainTab.Import -> R.string.nav_import
        MainTab.Settings -> R.string.nav_settings
    }
