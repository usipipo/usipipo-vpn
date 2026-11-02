package com.usipipo.vpn.ui.settings.appearance

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import com.usipipo.vpn.settings.PaletteVariant
import com.usipipo.vpn.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ajustes de apariencia: tema, animaciones y paleta, con su persistencia. */
class AppearanceViewModelTest {

    private val prefs = FakeSharedPreferences()
    private val settings = AppSettings(prefs)
    private val viewModel = AppearanceViewModel(settings)

    @Test
    fun `los valores por defecto son sistema, animaciones activas y azul noche`() {
        assertEquals(ThemeMode.System, viewModel.themeMode)
        assertTrue(viewModel.animationsEnabled)
        assertEquals(PaletteVariant.AzulNoche, viewModel.palette)
    }

    @Test
    fun `cambiar el tema lo persiste y lo devuelve`() {
        viewModel.setThemeMode(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, viewModel.themeMode)
        assertEquals("dark", prefs.getString("theme_mode", null))
    }

    @Test
    fun `desactivar las animaciones lo persiste`() {
        viewModel.setAnimationsEnabled(false)

        assertFalse(viewModel.animationsEnabled)
        assertFalse(prefs.getBoolean("animations_enabled", true))
    }

    @Test
    fun `cambiar la paleta lo persiste`() {
        viewModel.setPalette(PaletteVariant.VerdeBosque)

        assertEquals(PaletteVariant.VerdeBosque, viewModel.palette)
        assertEquals("verde_bosque", prefs.getString("palette", null))
    }

    @Test
    fun `una clave desconocida cae al valor por defecto`() {
        prefs.edit()
            .putString("theme_mode", "sepia")
            .putString("palette", "otro")
            .apply()

        assertEquals(ThemeMode.System, viewModel.themeMode)
        assertEquals(PaletteVariant.AzulNoche, viewModel.palette)
    }
}
