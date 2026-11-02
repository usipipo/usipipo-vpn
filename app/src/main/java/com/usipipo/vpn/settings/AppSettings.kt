package com.usipipo.vpn.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fachada tipada sobre SharedPreferences para los ajustes globales de la app
 * (tema, animaciones, paleta, …).
 *
 * Cada getter tiene su valor por defecto explícito, igual que
 * [PrefsUpdateStateStore] para las actualizaciones. Las preferencias viven en
 * el archivo `usipipo_settings` (MODE_PRIVATE, área privada de la app).
 *
 * Los cambios se exponen como [changedKeys]: un flujo con la clave de la
 * preferencia que cambió, para que la UI reaccione en vivo (por ejemplo, al
 * cambiar el tema) sin recargar la app.
 */
class AppSettings(private val prefs: SharedPreferences) {

    /**
     * Emite la clave de cada preferencia que se modifica. El buffer extra
     * permite notificar sin suspenderse: los cambios llegan desde el listener
     * de Android, que no es un contexto de corrutina.
     */
    private val changes = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Flujo con la clave de cada preferencia modificada. */
    val changedKeys: Flow<String> = changes.asSharedFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key != null) {
                changes.tryEmit(key)
            }
        }
    }

    /** Sistema / Claro / Oscuro. Por defecto sigue el sistema. */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE) ?: DEFAULT_THEME_MODE
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    /** Si las animaciones y transiciones de la app están activas. */
    var animationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANIMATIONS_ENABLED, DEFAULT_ANIMATIONS_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_ANIMATIONS_ENABLED, value).apply()

    /** Paleta de color activa. */
    var palette: String
        get() = prefs.getString(KEY_PALETTE, DEFAULT_PALETTE) ?: DEFAULT_PALETTE
        set(value) = prefs.edit().putString(KEY_PALETTE, value).apply()

    companion object {
        private const val PREFS_NAME = "usipipo_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        private const val KEY_PALETTE = "palette"
        private const val DEFAULT_THEME_MODE = "system"
        private const val DEFAULT_ANIMATIONS_ENABLED = true
        private const val DEFAULT_PALETTE = "azul_noche"

        fun create(context: Context): AppSettings =
            AppSettings(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
