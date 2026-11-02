package com.usipipo.vpn.ui.settings.appearance

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import com.usipipo.vpn.settings.PaletteVariant
import com.usipipo.vpn.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Ajustes de apariencia: tema, animaciones y paleta, con su persistencia. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {

    private val prefs = FakeSharedPreferences()
    private val settings = AppSettings(prefs)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `los valores por defecto son sistema, animaciones activas y azul noche`() = runTest {
        val viewModel = AppearanceViewModel(settings)
    recolectar(viewModel)

        assertEquals(ThemeMode.System, viewModel.uiState.value.themeMode)
        assertTrue(viewModel.uiState.value.animationsEnabled)
        assertEquals(PaletteVariant.AzulNoche, viewModel.uiState.value.palette)
    }

    @Test
    fun `cambiar el tema lo persiste y lo devuelve`() = runTest {
        val viewModel = AppearanceViewModel(settings)
        recolectar(viewModel)

        viewModel.setThemeMode(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, viewModel.uiState.value.themeMode)
        assertEquals("dark", prefs.getString("theme_mode", null))
    }

    @Test
    fun `desactivar las animaciones lo persiste`() = runTest {
        val viewModel = AppearanceViewModel(settings)
        recolectar(viewModel)

        viewModel.setAnimationsEnabled(false)

        assertFalse(viewModel.uiState.value.animationsEnabled)
        assertFalse(prefs.getBoolean("animations_enabled", true))
    }

    @Test
    fun `cambiar la paleta lo persiste`() = runTest {
        val viewModel = AppearanceViewModel(settings)
        recolectar(viewModel)

        viewModel.setPalette(PaletteVariant.VerdeBosque)

        assertEquals(PaletteVariant.VerdeBosque, viewModel.uiState.value.palette)
        assertEquals("verde_bosque", prefs.getString("palette", null))
    }

    @Test
    fun `una clave desconocida cae al valor por defecto`() = runTest {
        prefs.edit()
            .putString("theme_mode", "sepia")
            .putString("palette", "otro")
            .apply()
        val viewModel = AppearanceViewModel(settings)
        recolectar(viewModel)

        assertEquals(ThemeMode.System, viewModel.uiState.value.themeMode)
        assertEquals(PaletteVariant.AzulNoche, viewModel.uiState.value.palette)
    }

    /**
     * Mantiene vivo el flujo de estado: sin un colector, el flujo no se comparte y
     * los cambios no llegan al estado.
     */
    private fun TestScope.recolectar(viewModel: AppearanceViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }
}
