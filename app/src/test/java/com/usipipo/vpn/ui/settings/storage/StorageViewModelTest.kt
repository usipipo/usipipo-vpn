package com.usipipo.vpn.ui.settings.storage

import com.usipipo.vpn.domain.CountryInfo
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.ui.home.FakeConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** Pantalla de Almacenamiento: resumen, borrado de registro, caché y borrado total. */
@OptIn(ExperimentalCoroutinesApi::class)
class StorageViewModelTest {

    private val casa = SavedConfig(id = "c1", name = "Casa")
    private val store = FakeConfigStore(configs = listOf(casa))

    /** Lo que mide el disco; cada acción de borrado lo reduce, como hace la real. */
    private var desglose = StorageBreakdown(
        profilesBytes = 300L,
        logBytes = 200L,
        tempBytes = 50L,
    )

    private var registroBorrado = 0
    private var cacheLimpiado = 0
    private var borrados = 0

    private fun viewModel(borradoOk: Boolean = true) = StorageViewModel(
        sizes = { desglose },
        onClearLog = {
            registroBorrado++
            desglose = desglose.copy(logBytes = 0L)
        },
        onClearCache = {
            cacheLimpiado++
            store.clearGeoCache()
            desglose = desglose.copy(tempBytes = 0L)
        },
        onWipeAll = {
            borrados++
            if (!borradoOk) return@StorageViewModel false
            desglose = StorageBreakdown()
            true
        },
        workDispatcher = Dispatchers.Unconfined,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `el resumen suma las tres categorias`() = runTest {
        val estado = viewModel().uiState.value

        assertEquals(300L, estado.profilesBytes)
        assertEquals(200L, estado.logBytes)
        assertEquals(50L, estado.tempBytes)
        assertEquals(550L, estado.totalBytes)
    }

    @Test
    fun `solo la palabra clave exacta habilita el borrado`() = runTest {
        val vm = viewModel()

        vm.onWipeWordChange("borrar")
        assertFalse(vm.uiState.value.wipeReady)

        vm.onWipeWordChange("BORRADOR")
        assertFalse(vm.uiState.value.wipeReady)

        vm.onWipeWordChange("BORRAR")
        assertTrue(vm.uiState.value.wipeReady)

        // Los espacios de más los pone el teclado predictivo, no el usuario.
        vm.onWipeWordChange("  BORRAR  ")
        assertTrue(vm.uiState.value.wipeReady)
    }

    @Test
    fun `borrar el registro lo deja a cero y avisa`() = runTest {
        val vm = viewModel()

        vm.clearLog()

        assertEquals(1, registroBorrado)
        assertEquals(0L, vm.uiState.value.logBytes)
        assertEquals(StorageMessage.LogCleared, vm.uiState.value.message)
        assertFalse(vm.uiState.value.busy)
    }

    @Test
    fun `limpiar la cache vacia el cache de pais y los temporales`() = runTest {
        store.updateCountry(
            casa.id,
            CountryInfo(
                countryCode = "ES",
                countryName = "España",
                ip = "1.1.1.1",
                fetchedAt = 0L,
            ),
        )
        assertTrue(store.configs.value.first().countryCode != null)
        val vm = viewModel()

        vm.clearCache()

        assertEquals(1, cacheLimpiado)
        assertTrue(store.clearedGeo.contains(casa.id))
        assertEquals(0L, vm.uiState.value.tempBytes)
        assertEquals(StorageMessage.CacheCleared, vm.uiState.value.message)
    }

    @Test
    fun `borrar todo deja la app como recien instalada`() = runTest {
        val vm = viewModel()
        vm.onWipeWordChange("BORRAR")

        vm.wipeAll()

        assertEquals(1, borrados)
        assertEquals(0L, vm.uiState.value.totalBytes)
        assertEquals(StorageMessage.Wiped, vm.uiState.value.message)
        // La palabra clave se olvida: el diálogo se abre de nuevo desde cero.
        assertEquals("", vm.uiState.value.wipeWord)
        assertFalse(vm.uiState.value.wipeReady)
    }

    @Test
    fun `si el borrado falla no se marca como hecho`() = runTest {
        val vm = viewModel(borradoOk = false)

        vm.wipeAll()

        assertEquals(1, borrados)
        assertEquals(StorageMessage.WipeFailed, vm.uiState.value.message)
        assertFalse(vm.uiState.value.busy)
    }

    @Test
    fun `consumir el mensaje lo deja vacio`() = runTest {
        val vm = viewModel()

        vm.clearLog()
        vm.consumeMessage()

        assertEquals(null, vm.uiState.value.message)
    }
}
