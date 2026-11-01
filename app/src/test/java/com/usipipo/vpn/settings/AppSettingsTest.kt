package com.usipipo.vpn.settings

import android.content.SharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de [AppSettings] sobre preferencias falsas: la implementación real es de
 * Android, así que se sustituye el contrato ([SharedPreferences]) por una versión en
 * memoria, sin Robolectric.
 */
class AppSettingsTest {

    private val prefs = FakeSharedPreferences()
    private val settings = AppSettings(prefs)

    @Test
    fun `valores por defecto`() {
        assertEquals("system", settings.themeMode)
        assertTrue(settings.animationsEnabled)
        assertEquals("azul_noche", settings.palette)
    }

    @Test
    fun `guardar y leer el modo de tema`() {
        settings.themeMode = "dark"

        assertEquals("dark", settings.themeMode)
        assertEquals("dark", prefs.getString("theme_mode", null))
    }

    @Test
    fun `guardar y leer las animaciones`() {
        settings.animationsEnabled = false

        assertFalse(settings.animationsEnabled)
        assertFalse(prefs.getBoolean("animations_enabled", true))
    }

    @Test
    fun `guardar y leer la paleta`() {
        settings.palette = "verde_bosque"

        assertEquals("verde_bosque", settings.palette)
        assertEquals("verde_bosque", prefs.getString("palette", null))
    }

    @Test
    fun `una clave ajena no afecta los valores por defecto`() {
        prefs.edit().putString("clave_ajena", "cualquier cosa").apply()

        assertEquals("system", settings.themeMode)
        assertTrue(settings.animationsEnabled)
    }

    @Test
    fun `changedKeys emite la clave modificada`() = runTest {
        val emitadas = mutableListOf<String>()
        launch { settings.changedKeys.collect { emitadas.add(it) } }

        settings.themeMode = "light"

        advanceUntilIdle()

        assertEquals(listOf("theme_mode"), emitadas)
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
