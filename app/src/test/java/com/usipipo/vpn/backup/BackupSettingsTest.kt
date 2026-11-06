package com.usipipo.vpn.backup

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** settings.json: foto de los ajustes y su restauración. */
class BackupSettingsTest {

    private val prefs = FakeSharedPreferences()
    private val settings = AppSettings(prefs)

    @Test
    fun `la foto lleva los valores actuales de los ajustes`() {
        settings.themeMode = "dark"
        settings.palette = "ambar"
        settings.notificationsEnabled = false
        settings.excludedApps = "com.app.excluida"

        val foto = BackupSettings.snapshot(settings)

        assertEquals("dark", foto["theme_mode"])
        assertEquals("ambar", foto["palette"])
        assertEquals(false, foto["notifications_enabled"])
        assertEquals("com.app.excluida", foto["excluded_apps"])
    }

    @Test
    fun `restaurar escribe los valores en el almacen`() {
        settings.themeMode = "dark"
        settings.notificationTraffic = false

        BackupSettings.restore(settings, fotoRemota)

        assertEquals("light", settings.themeMode)
        assertTrue(settings.notificationTraffic)
        assertFalse(settings.notificationsEnabled)
        assertEquals(120f, settings.bubbleX, 0.001f)
    }

    @Test
    fun `restaurar ignora claves que no son de ajustes`() {
        settings.themeMode = "dark"

        BackupSettings.restore(settings, mapOf("clave_ajena" to "lo que sea"))

        assertEquals("dark", settings.themeMode)
    }

    @Test
    fun `serializar y leer conserva los valores`() {
        val json = BackupSettings.serialize(fotoRemota)
        val leido = BackupSettings.parse(json)

        assertEquals("light", leido["theme_mode"])
        assertEquals(false, leido["animations_enabled"])
        // JSON no distingue Float de Double: el número vuelve como Double.
        assertEquals(120f, (leido["bubble_x"] as Number).toFloat())
        assertEquals(1_700_000_000_000.0, (leido["backup_last_at"] as Number).toDouble())
    }

    private val fotoRemota = mapOf<String, Any>(
        "theme_mode" to "light",
        "animations_enabled" to false,
        "palette" to "verde_bosque",
        "notifications_enabled" to false,
        "notification_updates" to true,
        "bubble_enabled" to true,
        "bubble_x" to 120f,
        "backup_last_at" to 1_700_000_000_000L,
    )
}
