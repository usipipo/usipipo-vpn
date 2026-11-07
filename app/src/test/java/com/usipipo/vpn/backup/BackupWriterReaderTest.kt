package com.usipipo.vpn.backup

import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.settings.FakeSharedPreferences
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zip cifrado: estructura, roundtrip, contraseña y entradas peligrosas. */
class BackupWriterReaderTest {

    private val crypto = BackupCrypto(iterations = 1_000)
    private val writer = BackupWriter(crypto)
    private val reader = BackupReader(crypto)

    private val perfiles = listOf(
        BackupProfile(id = "p1", name = "Casa", configText = "[Interface]\nPrivateKey = clave-uno\n"),
        BackupProfile(id = "p2", name = "Trabajo", configText = "[Interface]\nPrivateKey = clave-dos\n"),
    )
    private val ajustes = mapOf<String, Any>("theme_mode" to "dark")

    @Test
    fun `leer lo que se escribe devuelve los perfiles y los ajustes`() {
        val zip = escribirZip(contraseña = "secreta")

        val contenido = reader.read(ByteArrayInputStream(zip), "secreta")

        assertEquals(2, contenido.profiles.size)
        assertEquals("p1", contenido.profiles[0].id)
        assertEquals("Casa", contenido.profiles[0].name)
        assertEquals("[Interface]\nPrivateKey = clave-uno\n", contenido.profiles[0].configText)
        assertEquals("dark", contenido.settings["theme_mode"])
    }

    @Test
    fun `el zip lleva una entrada cifrada por perfil, manifiesto y settings`() {
        val zip = escribirZip(contraseña = "secreta")

        val nombres = nombresDeEntrada(zip)

        assertTrue(nombres.contains(BackupArchive.SETTINGS_ENTRY))
        assertTrue(nombres.contains(BackupArchive.MANIFEST_ENTRY))
        assertTrue(nombres.contains("p1.conf.enc"))
        assertTrue(nombres.contains("p2.conf.enc"))
    }

    @Test
    fun `el texto de los perfiles no viaja en claro dentro del zip`() {
        val zip = escribirZip(contraseña = "secreta")

        // La clave privada aparece cifrada, no legible a ojo en el archivo.
        val texto = String(zip, Charsets.ISO_8859_1)
        assertTrue(!texto.contains("clave-uno"))
        assertTrue(zip.isNotEmpty())
    }

    @Test
    fun `con la contraseña incorrecta no se importa nada`() {
        val zip = escribirZip(contraseña = "secreta")

        try {
            reader.read(ByteArrayInputStream(zip), "otra")
            throw AssertionError("se esperaba WrongPasswordException")
        } catch (e: WrongPasswordException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `un zip que no es de copia es corrupto`() {
        val basura = "esto no es un zip".toByteArray()

        try {
            reader.read(ByteArrayInputStream(basura), "secreta")
            throw AssertionError("se esperaba CorruptBackupException")
        } catch (e: CorruptBackupException) {
            assertTrue(true)
        }
    }

    @Test
    fun `una entrada que se escapa del destino es corrupta`() {
        val zip = zipConEntradaPeligrosa()

        try {
            reader.read(ByteArrayInputStream(zip), "secreta")
            throw AssertionError("se esperaba CorruptBackupException")
        } catch (e: CorruptBackupException) {
            assertTrue(true)
        }
    }

    @Test
    fun `el perfil incluido no puede suplantar settings con un nombre parecido`() {
        val zip = escribirZipConEntrada(BackupArchive.SETTINGS_ENTRY, crypto.encrypt("secreta", "no es json".toByteArray()))

        try {
            reader.read(ByteArrayInputStream(zip), "secreta")
            throw AssertionError("se esperaba CorruptBackupException")
        } catch (e: CorruptBackupException) {
            assertTrue(true)
        }
    }

    @Test
    fun `una copia con la foto real de ajustes se lee entera`() {
        val ajustesReales = BackupSettings.snapshot(AppSettings(FakeSharedPreferences()))
        val salida = ByteArrayOutputStream()
        writer.write(
            profiles = listOf(BackupProfile(id = "p1", name = "Casa", configText = "[Interface]\n")),
            settingsValues = ajustesReales,
            password = "secreta",
            output = salida,
        )

        val contenido = reader.read(ByteArrayInputStream(salida.toByteArray()), "secreta")

        assertEquals(1, contenido.profiles.size)
        // La foto viaja con sus tipos y vuelve a aplicarse.
        assertEquals("azul_noche", contenido.settings["palette"])
        assertEquals(true, contenido.settings["notifications_enabled"])
    }

    @Test
    fun `una entrada mas grande que el tope se rechaza`() {
        val grande = ByteArray(64) { 0x41 }
        val zip = escribirZipConEntrada("p1.conf.enc", grande)
        val apretado = BackupReader(crypto, maxEntryBytes = 16)

        try {
            apretado.read(ByteArrayInputStream(zip), "secreta")
            throw AssertionError("se esperaba CorruptBackupException")
        } catch (e: CorruptBackupException) {
            assertTrue(e.message?.contains("grande") == true)
        }
    }

    @Test
    fun `mas entradas de las permitidas se rechazan`() {
        val zip = ByteArrayOutputStream().also { salida ->
            ZipOutputStream(salida).use { zipOut ->
                listOf("p1.conf.enc", "p2.conf.enc", "p3.conf.enc").forEach { nombre ->
                    zipOut.putNextEntry(ZipEntry(nombre))
                    zipOut.write("x".toByteArray())
                    zipOut.closeEntry()
                }
            }
        }.toByteArray()
        val apretado = BackupReader(crypto, maxEntries = 2)

        try {
            apretado.read(ByteArrayInputStream(zip), "secreta")
            throw AssertionError("se esperaba CorruptBackupException")
        } catch (e: CorruptBackupException) {
            assertTrue(e.message?.contains("entradas") == true)
        }
    }

    @Test
    fun `una copia normal cabe en los topes por defecto`() {
        val contenido = reader.read(ByteArrayInputStream(escribirZip(contraseña = "secreta")), "secreta")

        assertEquals(2, contenido.profiles.size)
    }

    private fun escribirZip(contraseña: String): ByteArray {
        val salida = ByteArrayOutputStream()
        writer.write(perfiles, ajustes, contraseña, salida)
        return salida.toByteArray()
    }

    private fun escribirZipConEntrada(nombre: String, contenido: ByteArray): ByteArray {
        val salida = ByteArrayOutputStream()
        ZipOutputStream(salida).use { zip ->
            zip.putNextEntry(ZipEntry(nombre))
            zip.write(contenido)
            zip.closeEntry()
        }
        return salida.toByteArray()
    }

    /** Un zip con una entrada `../` y otra cifrada válida: el lector debe rechazarlo. */
    private fun zipConEntradaPeligrosa(): ByteArray {
        val salida = ByteArrayOutputStream()
        ZipOutputStream(salida).use { zip ->
            zip.putNextEntry(ZipEntry("../fuera.conf.enc"))
            zip.write("lo que sea".toByteArray())
            zip.closeEntry()
        }
        return salida.toByteArray()
    }

    private fun nombresDeEntrada(zip: ByteArray): List<String> {
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
