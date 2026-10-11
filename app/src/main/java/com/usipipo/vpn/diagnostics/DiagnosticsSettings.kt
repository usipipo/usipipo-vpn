package com.usipipo.vpn.diagnostics

import android.content.Context

/** Ajustes de diagnóstico y marca de "cierre inesperado pendiente de avisar". */
class DiagnosticsSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var verbose: Boolean
        get() = prefs.getBoolean(KEY_VERBOSE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_VERBOSE, value).apply()
        }

    val crashPending: Boolean
        get() = prefs.getBoolean(KEY_CRASH_PENDING, false)

    /** commit() síncrono: el proceso puede morir justo después. */
    fun markCrashPending() {
        prefs.edit().putBoolean(KEY_CRASH_PENDING, true).commit()
    }

    fun clearCrashPending() {
        prefs.edit().putBoolean(KEY_CRASH_PENDING, false).apply()
    }

    private companion object {
        const val PREFS_NAME = "diagnostics"
        const val KEY_VERBOSE = "verbose"
        const val KEY_CRASH_PENDING = "crash_pending"
    }
}
