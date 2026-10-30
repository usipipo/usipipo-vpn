package com.usipipo.vpn.ui.home

import com.usipipo.vpn.data.DuplicateNameException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelRenameTest {

    private val casa = SavedConfig("a", "Casa")
    private val trabajo = SavedConfig("b", "Trabajo")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `renombrar actualiza el nombre en el estado`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val result = vm.rename("a", "Oficina")

        assertEquals("Oficina", result.getOrThrow().name)
        assertEquals("Oficina", vm.uiState.value.configs.first { it.id == "a" }.name)
    }

    @Test
    fun `renombrar con un nombre repetido devuelve el error de duplicado`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())

        assertTrue(vm.rename("a", "Trabajo").exceptionOrNull() is DuplicateNameException)
    }

    @Test
    fun `renombrar con un nombre vacio devuelve error`() = runTest {
        val vm = HomeViewModel(FakeConfigStore(listOf(casa, trabajo)), FakeTunnelManager(), FakeTunnelConnector())

        assertTrue(vm.rename("a", " ").exceptionOrNull() is IllegalArgumentException)
    }
}
