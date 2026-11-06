package com.usipipo.vpn.ui.settings.backup

import com.usipipo.vpn.backup.BackupArchive
import com.usipipo.vpn.backup.BackupCrypto
import com.usipipo.vpn.backup.BackupProfile
import com.usipipo.vpn.backup.BackupReader
import com.usipipo.vpn.backup.BackupWriter
import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import com.usipipo.vpn.ui.home.FakeConfigStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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

/** Copia de seguridad: exportar, importar y resolver duplicados. */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val prefs = FakeSharedPreferences()
    private val settings = AppSettings(prefs)

    /** Un perfil ya en el dispositivo. */
    private val casa = SavedConfig(id = "c1", name = "Casa")
    private val confCasa = "[Interface]\nPrivateKey = clave-uno\n[Peer]\nPublicKey = CLAVE-UNO\nEndpoint = 1.1.1.1:51820\n"
    private val store = FakeConfigStore(configs = listOf(casa), texts = mapOf("c1" to confCasa))

    /** Cripto barato: el test no mide el coste de PBKDF2. */
    private val crypto = BackupCrypto(iterations = 1_000)
    private val writer = BackupWriter(crypto)
    private val reader = BackupReader(crypto)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exportar todo marca la fecha de la ultima copia`() = runTest {
        val viewModel = viewModel()
        val salida = ByteArrayOutputStream()

        viewModel.exportAll("secreta", salida)

        assertTrue(salida.size() > 0)
        assertTrue(settings.backupLastAt > 0L)
        assertEquals(BackupMessage.Exported, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.busy)
    }

    @Test
    fun `sin contraseña no se exporta nada`() = runTest {
        val viewModel = viewModel()
        val salida = ByteArrayOutputStream()

        viewModel.exportAll("", salida)

        assertEquals(0, salida.size())
        assertEquals(0L, settings.backupLastAt)
        assertEquals(BackupMessage.EmptyPassword, viewModel.uiState.value.message)
    }

    @Test
    fun `exportar un perfil escribe su conf en claro`() = runTest {
        val viewModel = viewModel()
        val salida = ByteArrayOutputStream()

        viewModel.exportProfile("c1", salida)

        assertEquals(confCasa, salida.toString(Charsets.UTF_8.name()))
        assertTrue(settings.backupLastAt > 0L)
        assertEquals(BackupMessage.ProfileExported, viewModel.uiState.value.message)
    }

    @Test
    fun `importar una copia sin duplicados trae los perfiles y los ajustes`() = runTest {
        settings.themeMode = "dark"
        val zip = zipCon(listOf(perfil("p9", "Oficina", "CLAVE-B", "2.2.2.2:51820")))
        val viewModel = viewModel()

        viewModel.import("secreta", ByteArrayInputStream(zip))

        assertEquals(2, store.configs.value.size)
        assertEquals("light", settings.themeMode)
        assertEquals(BackupMessage.Imported, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.conflicts.isEmpty())
    }

    @Test
    fun `importar con un duplicado no lo trae hasta que se decide`() = runTest {
        val zip = zipCon(listOf(perfil("p9", "Casa", "CLAVE-B", "2.2.2.2:51820")))
        val viewModel = viewModel()

        viewModel.import("secreta", ByteArrayInputStream(zip))

        // El duplicado se detecta por el nombre, y no se importa a medias.
        assertEquals(1, viewModel.uiState.value.conflicts.size)
        assertEquals(1, store.configs.value.size)
        assertEquals(BackupMessage.Conflicts(1), viewModel.uiState.value.message)
    }

    @Test
    fun `sobrescribir reemplaza el perfil existente`() = runTest {
        val viewModel = viewModel()
        viewModel.import("secreta", ByteArrayInputStream(zipCon(listOf(perfil("p9", "Casa", "CLAVE-B", "2.2.2.2:51820")))))

        viewModel.resolveConflict(DuplicateDecision.Overwrite)

        assertEquals(1, store.configs.value.size)
        assertTrue(viewModel.uiState.value.conflicts.isEmpty())
        assertEquals(BackupMessage.Imported, viewModel.uiState.value.message)
        assertTrue(settings.backupLastAt > 0L)
    }

    @Test
    fun `omitir deja el perfil que ya estaba`() = runTest {
        val viewModel = viewModel()
        viewModel.import("secreta", ByteArrayInputStream(zipCon(listOf(perfil("p9", "Casa", "CLAVE-B", "2.2.2.2:51820")))))

        viewModel.resolveConflict(DuplicateDecision.Skip)

        assertEquals(1, store.configs.value.size)
        assertTrue(viewModel.uiState.value.conflicts.isEmpty())
    }

    @Test
    fun `mantener ambos deja los dos perfiles`() = runTest {
        val viewModel = viewModel()
        viewModel.import("secreta", ByteArrayInputStream(zipCon(listOf(perfil("p9", "Casa", "CLAVE-B", "2.2.2.2:51820")))))

        viewModel.resolveConflict(DuplicateDecision.KeepBoth)

        assertEquals(2, store.configs.value.size)
    }

    @Test
    fun `con la contraseña incorrecta no se importa nada`() = runTest {
        val zip = zipCon(listOf(perfil("p9", "Oficina", "CLAVE-B", "2.2.2.2:51820")))
        val viewModel = viewModel()

        viewModel.import("otra", ByteArrayInputStream(zip))

        assertEquals(1, store.configs.value.size)
        assertEquals(BackupMessage.WrongPassword, viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.busy)
    }

    @Test
    fun `un archivo que no es copia no importa nada`() = runTest {
        val viewModel = viewModel()

        viewModel.import("secreta", ByteArrayInputStream("no es un zip".toByteArray()))

        assertEquals(1, store.configs.value.size)
        assertEquals(BackupMessage.CorruptBackup(), viewModel.uiState.value.message)
    }

    @Test
    fun `cancelar el import deja los duplicados pendientes sin resolver`() = runTest {
        val viewModel = viewModel()
        viewModel.import("secreta", ByteArrayInputStream(zipCon(listOf(perfil("p9", "Casa", "CLAVE-B", "2.2.2.2:51820")))))

        viewModel.cancelImport()

        assertTrue(viewModel.uiState.value.conflicts.isEmpty())
        assertEquals(1, store.configs.value.size)
    }

    @Test
    fun `importar un conf suelto lo guarda con el nombre del fichero`() = runTest {
        val viewModel = viewModel()
        val confNuevo = "[Interface]\nPrivateKey = clave-nueva\n[Peer]\nPublicKey = CLAVE-N\nEndpoint = 3.3.3.3:51820\n"

        viewModel.importProfile(confNuevo, "Oficina")

        assertEquals(2, store.configs.value.size)
        assertEquals("Oficina", store.configs.value.last().name)
        assertEquals(confNuevo, store.storedTexts[store.configs.value.last().id])
        assertTrue(settings.backupLastAt > 0L)
        assertEquals(BackupMessage.Imported, viewModel.uiState.value.message)
    }

    @Test
    fun `importar un conf suelto con nombre repetido no pisa el que ya esta`() = runTest {
        val viewModel = viewModel()

        viewModel.importProfile("[Interface]\nPrivateKey = otra\n", "Casa")

        assertEquals(1, store.configs.value.size)
        assertEquals(BackupMessage.ProfileUnreadable, viewModel.uiState.value.message)
    }

    private fun viewModel() = BackupViewModel(
        store = store,
        settings = settings,
        writer = writer,
        reader = reader,
        // Dispatcher eager: el trabajo se completa antes de afirmar, sin hilos reales
        // que pudieran escapar a otra clase de tests.
        workDispatcher = UnconfinedTestDispatcher(),
    )

    private fun perfil(id: String, nombre: String, clave: String, endpoint: String) = BackupProfile(
        id = id,
        name = nombre,
        configText = "[Interface]\nPrivateKey = clave-$id\n[Peer]\nPublicKey = $clave\nEndpoint = $endpoint\n",
    )

    private fun zipCon(perfiles: List<BackupProfile>): ByteArray {
        settings.themeMode = "light"
        val salida = ByteArrayOutputStream()
        writer.write(perfiles, com.usipipo.vpn.backup.BackupSettings.snapshot(settings), "secreta", salida)
        val nombres = nombresZip(salida.toByteArray())
        assertTrue(nombres.contains(BackupArchive.SETTINGS_ENTRY))
        return salida.toByteArray()
    }

    private fun nombresZip(zip: ByteArray): List<String> {
        val nombres = mutableListOf<String>()
        java.util.zip.ZipInputStream(ByteArrayInputStream(zip)).use { flujo ->
            var entrada = flujo.nextEntry
            while (entrada != null) {
                nombres += entrada.name
                entrada = flujo.nextEntry
            }
        }
        return nombres
    }
}
