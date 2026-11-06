package com.usipipo.vpn.ui.settings

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.PaletteVariant
import com.usipipo.vpn.settings.ThemeMode

/** ViewModel de la pantalla hub de Ajustes: resúmenes del valor actual de cada sección. */
class SettingsViewModel(private val appSettings: AppSettings) {

    /**
     * Resumen del valor actual de cada sección, para mostrar a la derecha de su
     * fila en el hub de Ajustes. Refleja los valores por defecto hasta que cada
     * sección implemente su propia pantalla.
     */
    fun summary(route: SettingsRoute): String = when (route) {
        SettingsRoute.Appearance -> "Tema: ${themeLabel()} · ${animationsLabel()} · ${paletteLabel()}"
        SettingsRoute.Security -> "Desactivado"
        SettingsRoute.HomeScreen -> "Activado"
        SettingsRoute.TrafficBubble -> if (appSettings.bubbleEnabled) "Activada" else "Desactivada"
        SettingsRoute.Notifications -> if (appSettings.notificationsEnabled) "Activadas" else "Desactivadas"
        SettingsRoute.Updates -> "Activada"
        SettingsRoute.Backup -> if (appSettings.backupLastAt > 0L) "Con copias" else "Sin copias"
        SettingsRoute.Storage -> "—"
        SettingsRoute.More -> "—"
    }

    private fun themeLabel(): String = ThemeMode.fromKey(appSettings.themeMode).label()

    private fun animationsLabel(): String = if (appSettings.animationsEnabled) "Activado" else "Desactivado"

    private fun paletteLabel(): String = PaletteVariant.fromKey(appSettings.palette).label()

    /** Etiqueta en español de cada modo; es material de presentación, vive fuera del dominio. */
    private fun ThemeMode.label(): String = when (this) {
        ThemeMode.System -> "Sistema"
        ThemeMode.Light -> "Claro"
        ThemeMode.Dark -> "Oscuro"
    }

    /** Etiqueta en español de cada paleta. */
    private fun PaletteVariant.label(): String = when (this) {
        PaletteVariant.AzulNoche -> "Azul noche"
        PaletteVariant.VerdeBosque -> "Verde bosque"
        PaletteVariant.Ambar -> "Ámbar"
    }
}
