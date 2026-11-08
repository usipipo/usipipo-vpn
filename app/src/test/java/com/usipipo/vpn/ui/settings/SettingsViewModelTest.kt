package com.usipipo.vpn.ui.settings

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Resúmenes del hub de Ajustes. */
class SettingsViewModelTest {

    private val settings = AppSettings(FakeSharedPreferences())

    /** El resumen de tamaño se da formato con la locale del sistema. */
    private val localeAnterior = Locale.getDefault()

    @Before
    fun localeEspanola() {
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    @After
    fun restaurarLocale() {
        Locale.setDefault(localeAnterior)
    }

    @Test
    fun `almacenamiento resume el tamaño que ocupa la app`() {
        val viewModel = SettingsViewModel(settings) { 2_621_440L }

        assertEquals("2,5 MB", viewModel.summary(SettingsRoute.Storage))
    }

    @Test
    fun `almacenamiento recien instalada resume cero`() {
        val viewModel = SettingsViewModel(settings) { 0L }

        assertEquals("0 B", viewModel.summary(SettingsRoute.Storage))
    }

    @Test
    fun `las demas secciones no dependen del tamaño`() {
        val viewModel = SettingsViewModel(settings) { 2_621_440L }

        val resumen = viewModel.summary(SettingsRoute.Appearance)

        // El resumen de Apariencia habla de tema y paleta, nunca de bytes.
        assertFalse(resumen.contains("MB"))
        assertTrue(resumen.startsWith("Tema:"))
    }
}
