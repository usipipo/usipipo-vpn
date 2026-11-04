package com.usipipo.vpn.ui.settings.security

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Seguridad: biométrico y lista global de apps excluidas del túnel. */
@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

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
    fun `por defecto no hay biométrico ni apps excluidas`() = runTest {
        val viewModel = SecurityViewModel(settings)
        recolectar(viewModel)

        assertFalse(viewModel.uiState.value.biometricEnabled)
        assertEquals(emptyList<String>(), viewModel.uiState.value.excludedApps)
    }

    @Test
    fun `activar el biométrico lo persiste`() = runTest {
        val viewModel = SecurityViewModel(settings)
        recolectar(viewModel)

        viewModel.setBiometricEnabled(true)

        assertTrue(viewModel.uiState.value.biometricEnabled)
        assertTrue(prefs.getBoolean("biometric_enabled", false))
    }

    @Test
    fun `guardar la lista de apps las persiste y las devuelve`() = runTest {
        val viewModel = SecurityViewModel(settings)
        recolectar(viewModel)

        viewModel.setExcludedApps(listOf("com.app.excluida", "com.otra.app"))

        assertEquals(
            listOf("com.app.excluida", "com.otra.app"),
            viewModel.uiState.value.excludedApps,
        )
        assertEquals("com.app.excluida,com.otra.app", settings.excludedApps)
    }

    @Test
    fun `quitar todas las apps deja la lista vacia`() = runTest {
        settings.excludedApps = "com.app.excluida"
        val viewModel = SecurityViewModel(settings)
        recolectar(viewModel)

        viewModel.setExcludedApps(emptyList())

        assertEquals(emptyList<String>(), viewModel.uiState.value.excludedApps)
        assertEquals("", settings.excludedApps)
    }

    @Test
    fun `una lista con huecos y repetidos se limpia al guardar`() = runTest {
        val viewModel = SecurityViewModel(settings)
        recolectar(viewModel)

        viewModel.setExcludedApps(listOf("com.app.excluida", "  ", "com.app.excluida"))

        assertEquals(listOf("com.app.excluida"), viewModel.uiState.value.excludedApps)
    }

    @Test
    fun `añadir una app conserva las que ya estaban`() = runTest {
        settings.excludedApps = "com.app.excluida"
        val viewModel = SecurityViewModel(settings)
        recolectar(viewModel)

        viewModel.setExcludedApps(viewModel.uiState.value.excludedApps + "com.otra.app")

        assertEquals(
            listOf("com.app.excluida", "com.otra.app"),
            viewModel.uiState.value.excludedApps,
        )
    }

    /** Mantiene vivo el flujo de estado: sin colector no llegan los cambios. */
    private fun TestScope.recolectar(viewModel: SecurityViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }
}
