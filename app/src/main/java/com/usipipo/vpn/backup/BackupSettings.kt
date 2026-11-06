package com.usipipo.vpn.backup

import com.usipipo.vpn.settings.AppSettings
import org.json.JSONObject

/**
 * Foto de los ajustes de la app dentro de la copia (`settings.json`).
 *
 * La lista de claves es explícita a propósito: si mañana se añade una clave a
 * [AppSettings] y no se añade aquí, se ve en el diff en lugar de perderse un ajuste
 * en silencio. Las claves que no son de ajustes se ignoran al restaurar.
 */
object BackupSettings {

    /** Valores que viajan en la copia, con sus tipos. */
    fun snapshot(settings: AppSettings): Map<String, Any> = mapOf(
        AppSettings.KEY_THEME_MODE to settings.themeMode,
        AppSettings.KEY_ANIMATIONS_ENABLED to settings.animationsEnabled,
        AppSettings.KEY_PALETTE to settings.palette,
        AppSettings.KEY_BIOMETRIC_ENABLED to settings.biometricEnabled,
        AppSettings.KEY_EXCLUDED_APPS to settings.excludedApps,
        AppSettings.KEY_NOTIFICATIONS_ENABLED to settings.notificationsEnabled,
        AppSettings.KEY_NOTIFICATION_TRAFFIC to settings.notificationTraffic,
        AppSettings.KEY_NOTIFICATION_DISCONNECT to settings.notificationDisconnect,
        AppSettings.KEY_NOTIFICATION_UPDATES to settings.notificationUpdates,
        AppSettings.KEY_BUBBLE_ENABLED to settings.bubbleEnabled,
        AppSettings.KEY_BUBBLE_X to settings.bubbleX,
        AppSettings.KEY_BUBBLE_Y to settings.bubbleY,
    )

    /** Escribe los valores en el almacén; las claves desconocidas se ignoran. */
    fun restore(settings: AppSettings, values: Map<String, Any>) {
        values.forEach { (key, value) ->
            when (key) {
                AppSettings.KEY_THEME_MODE -> asString(value)?.let { settings.themeMode = it }
                AppSettings.KEY_PALETTE -> asString(value)?.let { settings.palette = it }
                AppSettings.KEY_ANIMATIONS_ENABLED -> asBoolean(value)?.let { settings.animationsEnabled = it }
                AppSettings.KEY_BIOMETRIC_ENABLED -> asBoolean(value)?.let { settings.biometricEnabled = it }
                AppSettings.KEY_EXCLUDED_APPS -> asString(value)?.let { settings.excludedApps = it }
                AppSettings.KEY_NOTIFICATIONS_ENABLED -> asBoolean(value)?.let { settings.notificationsEnabled = it }
                AppSettings.KEY_NOTIFICATION_TRAFFIC -> asBoolean(value)?.let { settings.notificationTraffic = it }
                AppSettings.KEY_NOTIFICATION_DISCONNECT -> asBoolean(value)?.let { settings.notificationDisconnect = it }
                AppSettings.KEY_NOTIFICATION_UPDATES -> asBoolean(value)?.let { settings.notificationUpdates = it }
                AppSettings.KEY_BUBBLE_ENABLED -> asBoolean(value)?.let { settings.bubbleEnabled = it }
                AppSettings.KEY_BUBBLE_X -> asFloat(value)?.let { settings.bubbleX = it }
                AppSettings.KEY_BUBBLE_Y -> asFloat(value)?.let { settings.bubbleY = it }
                else -> Unit
            }
        }
    }

    /** Texto JSON de la foto; se cifra como una entrada más del zip. */
    fun serialize(values: Map<String, Any>): String = JSONObject(values).toString()

    /**
     * Lee el JSON conservando los tipos. Un JSON inválido es una copia corrupta:
     * el lector decide qué hacer con el error, aquí solo se avisa.
     */
    fun parse(json: String): Map<String, Any> {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw CorruptBackupException("settings.json no es JSON válido")
        }
        val values = mutableMapOf<String, Any>()
        root.keys().forEach { key ->
            when (val value = root.get(key)) {
                is Boolean, is String, is Int, is Long, is Double, is Float -> values[key] = value
                else -> Unit
            }
        }
        return values
    }

    private fun asString(value: Any?): String? = value as? String

    private fun asBoolean(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull()
        else -> null
    }

    private fun asFloat(value: Any?): Float? = when (value) {
        is Float -> value
        is Double -> value.toFloat()
        is Int -> value.toFloat()
        is Long -> value.toFloat()
        is String -> value.toFloatOrNull()
        else -> null
    }
}
