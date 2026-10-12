package com.usipipo.vpn.update

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas de [PrefsUpdateStateStore] sobre unas preferencias falsas: la implementación
 * real es de Android, así que se sustituye el contrato ([SharedPreferences]) por una
 * versión en memoria, sin Robolectric.
 */
class UpdateStateStoreTest {

    private val prefs = FakeSharedPreferences()
    private val store = PrefsUpdateStateStore(prefs)

    @Test
    fun `empieza sin version pospuesta ni comprobacion previa`() {
        assertNull(store.skippedVersion)
        assertEquals(0L, store.lastCheckAt)
    }

    @Test
    fun `guarda la version pospuesta`() {
        store.skippedVersion = "0.2.0"

        assertEquals("0.2.0", store.skippedVersion)
    }

    @Test
    fun `guarda la marca de la ultima comprobacion`() {
        store.lastCheckAt = 1_700_000_000_000L

        assertEquals(1_700_000_000_000L, store.lastCheckAt)
    }

    @Test
    fun `una version pospuesta en blanco se lee como ausente`() {
        store.skippedVersion = "   "

        assertNull(store.skippedVersion)
    }

    @Test
    fun `borrar la version pospuesta devuelve el estado al inicio`() {
        store.skippedVersion = "0.2.0"

        store.skippedVersion = null

        assertNull(store.skippedVersion)
    }

    @Test
    fun `lo guardado sobrevive a otra instancia sobre las mismas preferencias`() {
        store.skippedVersion = "0.3.0"
        store.lastCheckAt = 42L
        val otra = PrefsUpdateStateStore(prefs)

        assertEquals("0.3.0", otra.skippedVersion)
        assertEquals(42L, otra.lastCheckAt)
    }

    @Test
    fun `las claves de las preferencias son estables`() {
        // Fija los nombres de clave: cambiarlos perdería la versión pospuesta guardada.
        prefs.edit().putLong("last_check_at", 99L).putString("skipped_version", "9.9.9").apply()

        assertEquals("9.9.9", store.skippedVersion)
        assertEquals(99L, store.lastCheckAt)
    }

    /** SharedPreferences en memoria: mismo contrato, sin Android. */
    private class FakeSharedPreferences : SharedPreferences {

        private val values = mutableMapOf<String, Any?>()

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

        override fun edit(): SharedPreferences.Editor = FakeEditor(values)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) = Unit
    }

    private class FakeEditor(private val values: MutableMap<String, Any?>) :
        SharedPreferences.Editor {

        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?) = apply {
            pending[key] = value
            removals -= key
        }

        override fun putStringSet(key: String, values: MutableSet<String>?) = apply {
            pending[key] = values
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
            if (clearAll) values.clear()
            removals.forEach { values.remove(it) }
            values.putAll(pending)
            pending.clear()
            removals.clear()
            clearAll = false
        }
    }
}
