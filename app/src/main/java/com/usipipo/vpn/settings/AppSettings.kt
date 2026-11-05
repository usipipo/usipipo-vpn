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

    /** Si el acceso a la app se protege con biometría. */
    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, DEFAULT_BIOMETRIC_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    /** Paquetes excluidos del túnel, en texto (`com.app`, `com.otra`). Lista global. */
    var excludedApps: String
        get() = prefs.getString(KEY_EXCLUDED_APPS, DEFAULT_EXCLUDED_APPS).orEmpty()
        set(value) = prefs.edit().putString(KEY_EXCLUDED_APPS, value).apply()

    /**
     * Interruptor general de avisos propios. No afecta a la notificación del
     * servicio en primer plano del túnel, que Android exige mientras esté activo.
     */
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    /** Aviso de estado del túnel con tráfico (canal «estado»). */
    var notificationTraffic: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_TRAFFIC, DEFAULT_NOTIFICATION_TRAFFIC)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_TRAFFIC, value).apply()

    /** Aviso de desconexión o fallo del túnel (canal «avisos»). */
    var notificationDisconnect: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_DISCONNECT, DEFAULT_NOTIFICATION_DISCONNECT)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_DISCONNECT, value).apply()

    /** Aviso de actualización disponible (canal «avisos»). */
    var notificationUpdates: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_UPDATES, DEFAULT_NOTIFICATION_UPDATES)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_UPDATES, value).apply()

    /** Si la burbuja de tráfico se dibuja sobre otras apps con el túnel activo. */
    var bubbleEnabled: Boolean
        get() = prefs.getBoolean(KEY_BUBBLE_ENABLED, DEFAULT_BUBBLE_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_BUBBLE_ENABLED, value).apply()

    /**
     * Posición guardada de la burbuja, en píxeles dentro de la ventana overlay.
     * Un valor negativo significa «sin posición guardada»: el overlay la coloca
     * centrada y cerca del borde superior.
     */
    var bubbleX: Float
        get() = prefs.getFloat(KEY_BUBBLE_X, DEFAULT_BUBBLE_POSITION)
        set(value) = prefs.edit().putFloat(KEY_BUBBLE_X, value).apply()

    /** Posición vertical guardada de la burbuja; negativo = sin posición. */
    var bubbleY: Float
        get() = prefs.getFloat(KEY_BUBBLE_Y, DEFAULT_BUBBLE_POSITION)
        set(value) = prefs.edit().putFloat(KEY_BUBBLE_Y, value).apply()

    companion object {
        const val KEY_THEME_MODE: String = "theme_mode"
        const val KEY_ANIMATIONS_ENABLED: String = "animations_enabled"
        const val KEY_PALETTE: String = "palette"
        const val KEY_BIOMETRIC_ENABLED: String = "biometric_enabled"
        const val KEY_EXCLUDED_APPS: String = "excluded_apps"
        const val KEY_NOTIFICATIONS_ENABLED: String = "notifications_enabled"
        const val KEY_NOTIFICATION_TRAFFIC: String = "notification_traffic"
        const val KEY_NOTIFICATION_DISCONNECT: String = "notification_disconnect"
        const val KEY_NOTIFICATION_UPDATES: String = "notification_updates"
        const val KEY_BUBBLE_ENABLED: String = "bubble_enabled"
        const val KEY_BUBBLE_X: String = "bubble_x"
        const val KEY_BUBBLE_Y: String = "bubble_y"

        private const val PREFS_NAME = "usipipo_settings"
        private const val DEFAULT_THEME_MODE = "system"
        private const val DEFAULT_ANIMATIONS_ENABLED = true
        private const val DEFAULT_PALETTE = "azul_noche"
        private const val DEFAULT_BIOMETRIC_ENABLED = false
        private const val DEFAULT_EXCLUDED_APPS = ""
        private const val DEFAULT_NOTIFICATIONS_ENABLED = true
        private const val DEFAULT_NOTIFICATION_TRAFFIC = true
        private const val DEFAULT_NOTIFICATION_DISCONNECT = true
        private const val DEFAULT_NOTIFICATION_UPDATES = true
        private const val DEFAULT_BUBBLE_ENABLED = false

        /** Sin posición guardada: el overlay decide dónde colocar la burbuja. */
        private const val DEFAULT_BUBBLE_POSITION = -1f

        fun create(context: Context): AppSettings =
            AppSettings(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
