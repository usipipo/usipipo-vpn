package com.usipipo.vpn.ui.settings.bubble

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

/** Burbuja de tráfico: interruptor, permiso de superposición y posición. */
@OptIn(ExperimentalCoroutinesApi::class)
class TrafficBubbleViewModelTest {

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
    fun `por defecto la burbuja esta apagada y sin posicion`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)

        assertFalse(viewModel.uiState.value.bubbleEnabled)
        assertEquals(-1f, viewModel.uiState.value.bubbleX, 0.001f)
        assertEquals(-1f, viewModel.uiState.value.bubbleY, 0.001f)
    }

    @Test
    fun `encender la burbuja lo persiste`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)

        viewModel.setBubbleEnabled(true)

        assertTrue(viewModel.uiState.value.bubbleEnabled)
        assertTrue(prefs.getBoolean("bubble_enabled", false))
    }

    @Test
    fun `guardar la posicion la persiste`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)

        viewModel.setPosition(120f, 480f)

        assertEquals(120f, viewModel.uiState.value.bubbleX, 0.001f)
        assertEquals(480f, prefs.getFloat("bubble_y", 0f), 0.001f)
    }

    @Test
    fun `el permiso de superposicion se lee del proveedor`() = runTest {
        val viewModel = viewModel(concedido = false)
        recolectar(viewModel)

        assertFalse(viewModel.uiState.value.overlayGranted)
    }

    @Test
    fun `una clave ajena no altera el estado`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)
        viewModel.setPosition(10f, 20f)

        prefs.edit().putString("palette", "ambar").apply()
        viewModel.refreshPermission()

        assertEquals(10f, viewModel.uiState.value.bubbleX, 0.001f)
    }

    private fun viewModel(concedido: Boolean = true) =
        TrafficBubbleViewModel(settings) { concedido }

    /**
     * Mantiene vivo el flujo de estado: sin un colector, el flujo no se comparte y
     * los cambios no llegan al estado.
     */
    private fun TestScope.recolectar(viewModel: TrafficBubbleViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }
}
