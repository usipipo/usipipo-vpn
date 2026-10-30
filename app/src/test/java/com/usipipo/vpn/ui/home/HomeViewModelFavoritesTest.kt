package com.usipipo.vpn.ui.home

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.quickconnect.FakeTunnelConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFavoritesTest {

    private val casa = SavedConfig("a", "Casa")
    private val trabajo = SavedConfig("b", "Trabajo", favorite = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `marcar una configuracion como favorita se refleja en el estado`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.toggleFavorite("a")

        assertTrue(vm.uiState.value.configs.first { it.id == "a" }.favorite)
    }

    @Test
    fun `desmarcar una favorita la devuelve a no favorita`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.toggleFavorite("b")

        assertFalse(vm.uiState.value.configs.first { it.id == "b" }.favorite)
    }

    @Test
    fun `un id desconocido no cambia nada`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.toggleFavorite("zzz")

        assertEquals(listOf(casa, trabajo), vm.uiState.value.configs)
    }
}
