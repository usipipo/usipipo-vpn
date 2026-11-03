package com.usipipo.vpn.settings

/** Modo de tema: sigue el sistema, claro u oscuro. */
enum class ThemeMode(val key: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        /**
         * Recupera el modo a partir de su clave. Una clave desconocida (o ausente)
         * vuelve al modo por defecto: el sistema.
         */
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: System
    }
}

/**
 * Resuelve si el tema oscuro debe aplicarse, según el modo elegido y el estado
 * del sistema.
 */
fun ThemeMode.darkTheme(isSystemDark: Boolean): Boolean = when (this) {
    ThemeMode.System -> isSystemDark
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
