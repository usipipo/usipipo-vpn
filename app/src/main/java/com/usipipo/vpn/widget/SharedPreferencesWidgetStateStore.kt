package com.usipipo.vpn.widget

import android.content.Context

/**
 * Estado del widget en preferencias privadas.
 *
 * Solo se guarda el nombre del estado y la fecha del último cambio. Un estado desconocido
 * (por ejemplo, de una versión futura) se trata como si no hubiera estado guardado.
 */
class SharedPreferencesWidgetStateStore(context: Context) : WidgetStateStore {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): PersistedWidgetState? {
        val status = preferences.getString(KEY_STATUS, null)
            ?.let { name -> runCatching { WidgetStatus.valueOf(name) }.getOrNull() }
            ?: return null
        return PersistedWidgetState(
            status = status,
            updatedAtEpochMs = preferences.getLong(KEY_UPDATED_AT, 0L),
        )
    }

    override fun write(state: PersistedWidgetState) {
        preferences.edit()
            .putString(KEY_STATUS, state.status.name)
            .putLong(KEY_UPDATED_AT, state.updatedAtEpochMs)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "usipipo_widget_state"
        const val KEY_STATUS = "status"
        const val KEY_UPDATED_AT = "updatedAtEpochMs"
    }
}
