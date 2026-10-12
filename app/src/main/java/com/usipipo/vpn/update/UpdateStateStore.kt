package com.usipipo.vpn.update

import android.content.Context
import android.content.SharedPreferences

/** Memoria de la comprobación: qué versión se pospuso y cuándo se miró por última vez. */
interface UpdateStateStore {
    var skippedVersion: String?
    var lastCheckAt: Long

    fun markSkipped(version: String)
    fun markChecked(at: Long)
    fun clear()
}

/** Implementación sobre SharedPreferences: sobrevive a reinicios de la app. */
class PrefsUpdateStateStore(private val prefs: SharedPreferences) : UpdateStateStore {

    override var skippedVersion: String?
        get() = prefs.getString(KEY_SKIPPED, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_SKIPPED, value).apply()
        }

    override var lastCheckAt: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_CHECK, value).apply()
        }

    override fun markSkipped(version: String) {
        skippedVersion = version
    }

    override fun markChecked(at: Long) {
        lastCheckAt = at
    }

    override fun clear() {
        prefs.edit().remove(KEY_SKIPPED).remove(KEY_LAST_CHECK).apply()
    }

    companion object {
        private const val PREFS_NAME = "updates"
        private const val KEY_SKIPPED = "skipped_version"
        private const val KEY_LAST_CHECK = "last_check_at"

        fun create(context: Context): PrefsUpdateStateStore = PrefsUpdateStateStore(
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        )
    }
}
