package com.usipipo.vpn.ui.settings

import com.usipipo.vpn.settings.AppSettings

/** ViewModel de la pantalla hub de Ajustes: resúmenes del valor actual de cada sección. */
class SettingsViewModel(private val appSettings: AppSettings) {

    /** Modo de tema actual ("system", "light" o "dark"). */
    val themeMode: String get() = appSettings.themeMode

    /** Si las animaciones están activas. */
    val animationsEnabled: Boolean get() = appSettings.animationsEnabled

    /** Paleta de color activa. */
    val palette: String get() = appSettings.palette

    /**
     * Resumen del valor actual de cada sección, para mostrar a la derecha de su
     * fila en el hub de Ajustes. Refleja los valores por defecto hasta que cada
     * sección implemente su propia pantalla.
     */
    fun summary(route: SettingsRoute): String = when (route) {
        SettingsRoute.Appearance -> "Tema: ${themeLabel()}"
        SettingsRoute.Security -> "Desactivado"
        SettingsRoute.HomeScreen -> "Activado"
        SettingsRoute.TrafficBubble -> "Desactivada"
        SettingsRoute.Notifications -> "Activadas"
        SettingsRoute.Updates -> "Activada"
        SettingsRoute.Backup -> "Sin copias"
        SettingsRoute.Storage -> "—"
        SettingsRoute.More -> "—"
    }

    private fun themeLabel(): String = when (appSettings.themeMode) {
        "system" -> "Sistema"
        "light" -> "Claro"
        "dark" -> "Oscuro"
        else -> "Sistema"
    }
}
