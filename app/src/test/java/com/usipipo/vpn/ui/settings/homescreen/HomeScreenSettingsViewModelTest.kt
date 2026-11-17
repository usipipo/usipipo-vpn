package com.usipipo.vpn.ui.settings.homescreen

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Ajustes de la pantalla inicial: qué muestra el widget, con su persistencia. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenSettingsViewModelTest {

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
    fun `el widget muestra pais y perfil por defecto`() = runTest {
        val viewModel = HomeScreenSettingsViewModel(settings)
        recolectar(viewModel)

        assertTrue(viewModel.uiState.value.showCountry)
    }

    @Test
    fun `ocultar pais y perfil lo persiste`() = runTest {
        val viewModel = HomeScreenSettingsViewModel(settings)
        recolectar(viewModel)

        viewModel.setShowCountry(false)

        assertFalse(viewModel.uiState.value.showCountry)
        assertFalse(prefs.getBoolean("widget_show_country", true))
    }

    /**
     * Mantiene vivo el flujo de estado: sin un colector, el flujo no se comparte y
     * los cambios no llegan al estado.
     */
    private fun TestScope.recolectar(viewModel: HomeScreenSettingsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }
}