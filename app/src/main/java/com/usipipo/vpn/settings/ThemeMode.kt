package com.usipipo.vpn.settings

/** Modo de tema: sigue el sistema, claro u oscuro. */
enum class ThemeMode { System, Light, Dark }

/**
 * Resuelve si el tema oscuro debe aplicarse, según el modo elegido y el estado
 * del sistema.
 */
fun ThemeMode.darkTheme(isSystemDark: Boolean): Boolean = when (this) {
    ThemeMode.System -> isSystemDark
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
