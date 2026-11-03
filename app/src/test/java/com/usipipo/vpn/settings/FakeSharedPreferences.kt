package com.usipipo.vpn.settings

import android.content.SharedPreferences

/**
 * SharedPreferences en memoria: mismo contrato, sin Android. Notifica los cambios a
 * los listeners registrados, como hace la implementación real, para que los tests
 * puedan observar el flujo de cambios sin Robolectric.
 */
internal class FakeSharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, Any?>()
    private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? =
        (values[key] as? String) ?: defValue

    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (values[key] as? MutableSet<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        (values[key] as? Int) ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        (values[key] as? Long) ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        (values[key] as? Float) ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        (values[key] as? Boolean) ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        // Se usa add() y no +=: con el tipo platform de Android, += resuelve a
        // plus() y no a plusAssign(), y el elemento acaba tipado como nullable.
        if (listener != null) {
            listeners.add(listener)
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        if (listener != null) {
            listeners.remove(listener)
        }
    }

    /** Notifica a los listeners como hace la implementación real de Android. */
    fun notifyChanged(key: String) {
        listeners.toList().forEach { it.onSharedPreferenceChanged(this, key) }
    }

    /** Número de listeners registrados: permite observar el ciclo de vida. */
    fun listenerCount(): Int = listeners.size

    inner class FakeEditor : SharedPreferences.Editor {

        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?) = apply {
            pending[key] = value
            removals -= key
        }

        override fun putStringSet(key: String, value: MutableSet<String>?) = apply {
            pending[key] = value
            removals -= key
        }

        override fun putInt(key: String, value: Int) = apply {
            pending[key] = value
            removals -= key
        }

        override fun putLong(key: String, value: Long) = apply {
            pending[key] = value
            removals -= key
        }

        override fun putFloat(key: String, value: Float) = apply {
            pending[key] = value
            removals -= key
        }

        override fun putBoolean(key: String, value: Boolean) = apply {
            pending[key] = value
            removals -= key
        }

        override fun remove(key: String) = apply {
            removals += key
            pending -= key
        }

        override fun clear() = apply {
            clearAll = true
            pending.clear()
            removals.clear()
        }

        override fun commit(): Boolean {
            flush()
            return true
        }

        override fun apply() {
            flush()
        }

        private fun flush() {
            if (clearAll) {
                val borradas = values.keys.toList()
                values.clear()
                borradas.forEach { notifyChanged(it) }
            }
            removals.forEach { key ->
                values.remove(key)
                notifyChanged(key)
            }
            val modificadas = pending.keys.toList()
            values.putAll(pending)
            pending.clear()
            removals.clear()
            clearAll = false
            modificadas.forEach { notifyChanged(it) }
        }
    }
}
