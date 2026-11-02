package com.usipipo.vpn.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Modo de tema: como se resuelve el tema oscuro según el modo elegido y el sistema. */
class ThemeModeTest {

    @Test
    fun `el modo sistema sigue al sistema`() {
        assertTrue(ThemeMode.System.darkTheme(isSystemDark = true))
        assertFalse(ThemeMode.System.darkTheme(isSystemDark = false))
    }

    @Test
    fun `el modo claro ignora el sistema`() {
        assertFalse(ThemeMode.Light.darkTheme(isSystemDark = true))
        assertFalse(ThemeMode.Light.darkTheme(isSystemDark = false))
    }

    @Test
    fun `el modo oscuro ignora el sistema`() {
        assertTrue(ThemeMode.Dark.darkTheme(isSystemDark = false))
        assertTrue(ThemeMode.Dark.darkTheme(isSystemDark = true))
    }

    @Test
    fun `cada modo tiene su clave estable`() {
        assertEquals("system", ThemeMode.System.key)
        assertEquals("light", ThemeMode.Light.key)
        assertEquals("dark", ThemeMode.Dark.key)
    }

    @Test
    fun `cada clave recupera su modo`() {
        ThemeMode.entries.forEach { modo ->
            assertEquals(modo, ThemeMode.fromKey(modo.key))
        }
    }

    @Test
    fun `una clave desconocida vuelve al modo sistema`() {
        assertEquals(ThemeMode.System, ThemeMode.fromKey("sepia"))
        assertEquals(ThemeMode.System, ThemeMode.fromKey(null))
    }
}
