package com.usipipo.vpn.ui.settings.notifications

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

/** Notificaciones: interruptor general, avisos por tipo y permiso del sistema. */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

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
    fun `por defecto todos los avisos estan activos y el permiso concedido`() = runTest {
        val viewModel = viewModel(concedido = true)
        recolectar(viewModel)

        val state = viewModel.uiState.value

        assertTrue(state.notificationsEnabled)
        assertTrue(state.trafficEnabled)
        assertTrue(state.disconnectEnabled)
        assertTrue(state.updatesEnabled)
        assertTrue(state.notificationsGranted)
    }

    @Test
    fun `apagar el general lo persiste y conserva los avisos por tipo`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)

        viewModel.setNotificationsEnabled(false)

        assertFalse(viewModel.uiState.value.notificationsEnabled)
        assertFalse(prefs.getBoolean("notifications_enabled", true))
        // Los avisos por tipo se quedan como estaban: la pantalla los deshabilita,
        // no borra la decisión del usuario.
        assertTrue(viewModel.uiState.value.trafficEnabled)
        assertTrue(viewModel.uiState.value.disconnectEnabled)
        assertTrue(viewModel.uiState.value.updatesEnabled)
    }

    @Test
    fun `volver a encender el general devuelve los avisos por tipo guardados`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)
        viewModel.setTrafficEnabled(false)
        viewModel.setNotificationsEnabled(false)

        viewModel.setNotificationsEnabled(true)

        assertTrue(viewModel.uiState.value.notificationsEnabled)
        assertFalse(viewModel.uiState.value.trafficEnabled)
    }

    @Test
    fun `cada aviso por tipo se activa y se desactiva`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)

        viewModel.setTrafficEnabled(false)
        viewModel.setDisconnectEnabled(false)
        viewModel.setUpdatesEnabled(false)

        assertFalse(viewModel.uiState.value.trafficEnabled)
        assertFalse(prefs.getBoolean("notification_traffic", true))
        assertFalse(viewModel.uiState.value.disconnectEnabled)
        assertFalse(prefs.getBoolean("notification_disconnect", true))
        assertFalse(viewModel.uiState.value.updatesEnabled)
        assertFalse(prefs.getBoolean("notification_updates", true))
    }

    @Test
    fun `un cambio de preferencia ajeno no altera el estado`() = runTest {
        val viewModel = viewModel()
        recolectar(viewModel)
        prefs.edit().putString("palette", "ambar").apply()

        viewModel.setTrafficEnabled(true)

        assertFalse(viewModel.uiState.value.trafficEnabled)
    }

    @Test
    fun `sin permiso de notificaciones el estado lo refleja`() = runTest {
        val viewModel = viewModel(concedido = false)
        recolectar(viewModel)

        assertFalse(viewModel.uiState.value.notificationsGranted)
    }

    private fun viewModel(concedido: Boolean = true) =
        NotificationsViewModel(settings) { concedido }

    /**
     * Mantiene vivo el flujo de estado: sin un colector, el flujo no se comparte y
     * los cambios no llegan al estado.
     */
    private fun TestScope.recolectar(viewModel: NotificationsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }
}
