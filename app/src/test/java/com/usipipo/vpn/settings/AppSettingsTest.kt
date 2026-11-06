package com.usipipo.vpn.settings

import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
@OptIn(ExperimentalCoroutinesApi::class)
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
        suscribir(emitadas)

        settings.themeMode = "light"

        assertEquals(listOf("theme_mode"), emitadas)
    }

    @Test
    fun `cada ajuste modificado emite su clave`() = runTest {
        val emitadas = mutableListOf<String>()
        suscribir(emitadas)

        settings.themeMode = "dark"
        settings.animationsEnabled = false
        settings.palette = "verde_bosque"

        assertEquals(listOf("theme_mode", "animations_enabled", "palette"), emitadas)
    }

    @Test
    fun `el almacen registra el listener de cambios al construirse`() {
        assertEquals(1, prefs.listenerCount())
    }

    @Test
    fun `el desbloqueo biometrico se apaga por defecto y persiste`() {
        assertFalse(settings.biometricEnabled)

        settings.biometricEnabled = true

        assertTrue(settings.biometricEnabled)
        assertTrue(prefs.getBoolean("biometric_enabled", false))
    }

    @Test
    fun `la lista de apps excluidas esta vacia por defecto y persiste`() {
        assertEquals("", settings.excludedApps)

        settings.excludedApps = "com.app.excluida,com.otra.app"

        assertEquals("com.app.excluida,com.otra.app", settings.excludedApps)
        assertEquals("com.app.excluida,com.otra.app", prefs.getString("excluded_apps", null))
    }

    @Test
    fun `cambiar las claves de seguridad emite su clave`() = runTest {
        val emitidas = mutableListOf<String>()
        suscribir(emitidas)

        settings.biometricEnabled = true
        settings.excludedApps = "com.app.excluida"

        assertEquals(listOf("biometric_enabled", "excluded_apps"), emitidas)
    }

    @Test
    fun `los avisos de notificaciones estan activos por defecto`() {
        assertTrue(settings.notificationsEnabled)
        assertTrue(settings.notificationTraffic)
        assertTrue(settings.notificationDisconnect)
        assertTrue(settings.notificationUpdates)
    }

    @Test
    fun `cada aviso de notificacion persiste su clave`() {
        settings.notificationsEnabled = false
        settings.notificationTraffic = false
        settings.notificationDisconnect = false
        settings.notificationUpdates = false

        assertFalse(settings.notificationsEnabled)
        assertFalse(prefs.getBoolean("notifications_enabled", true))
        assertFalse(prefs.getBoolean("notification_traffic", true))
        assertFalse(prefs.getBoolean("notification_disconnect", true))
        assertFalse(prefs.getBoolean("notification_updates", true))
    }

    @Test
    fun `cambiar las claves de notificaciones emite su clave`() = runTest {
        val emitidas = mutableListOf<String>()
        suscribir(emitidas)

        settings.notificationsEnabled = false
        settings.notificationTraffic = false
        settings.notificationDisconnect = false
        settings.notificationUpdates = false

        assertEquals(
            listOf(
                "notifications_enabled",
                "notification_traffic",
                "notification_disconnect",
                "notification_updates",
            ),
            emitidas,
        )
    }

    @Test
    fun `la burbuja esta apagada por defecto y sin posicion guardada`() {
        assertFalse(settings.bubbleEnabled)
        assertEquals(SIN_POSICION, settings.bubbleX, 0.001f)
        assertEquals(SIN_POSICION, settings.bubbleY, 0.001f)
    }

    @Test
    fun `la burbuja y su posicion persisten`() {
        settings.bubbleEnabled = true
        settings.bubbleX = 120f
        settings.bubbleY = 480f

        assertTrue(settings.bubbleEnabled)
        assertTrue(prefs.getBoolean("bubble_enabled", false))
        assertEquals(120f, prefs.getFloat("bubble_x", 0f), 0.001f)
        assertEquals(480f, prefs.getFloat("bubble_y", 0f), 0.001f)
    }

    @Test
    fun `cambiar las claves de la burbuja emite su clave`() = runTest {
        val emitidas = mutableListOf<String>()
        suscribir(emitidas)

        settings.bubbleEnabled = true
        settings.bubbleX = 10f

        assertEquals(listOf("bubble_enabled", "bubble_x"), emitidas)
    }

    @Test
    fun `no hay fecha de ultima copia por defecto y persiste`() {
        assertEquals(0L, settings.backupLastAt)

        settings.backupLastAt = 1_700_000_000_000L

        assertEquals(1_700_000_000_000L, settings.backupLastAt)
        assertEquals(1_700_000_000_000L, prefs.getLong("backup_last_at", 0L))
    }

    @Test
    fun `cambiar la fecha de la copia emite su clave`() = runTest {
        val emitidas = mutableListOf<String>()
        suscribir(emitidas)

        settings.backupLastAt = 1_700_000_000_000L

        assertEquals(listOf("backup_last_at"), emitidas)
    }

    private companion object {
        /** Sin posición guardada: el overlay decide dónde ponerla. */
        const val SIN_POSICION = -1f
    }

    /**
     * Suscribe un colector al flujo de cambios. Con un dispatcher eager el colector
     * arranca y se reanuda de inmediato, así que el test comprueba el efecto sin
     * depender de que el planificador ejecute el trabajo en segundo plano.
     */
    private fun TestScope.suscribir(recibidas: MutableList<String>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            settings.changedKeys.collect { recibidas.add(it) }
        }
    }
}
