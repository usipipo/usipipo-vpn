package com.usipipo.vpn.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Contenido descifrado de una copia. */
data class BackupContent(val profiles: List<BackupProfile>, val settings: Map<String, Any>)

/**
 * Lee el zip de copia y lo descifra con la contraseña del usuario.
 *
 * Antes de leer una entrada se valida su nombre ([BackupArchive.isSafeEntryName]):
 * un zip no es de fiar y una entrada `../` escribiría fuera del destino. Las
 * entradas sin nombre válido abortan la lectura: no se importa nada a medias.
 */
class BackupReader(
    private val crypto: BackupCrypto = BackupCrypto(),
    /**
     * Tope por entrada. Un `.conf` cifrado pesa KB, asi que 1 MiB es holgado para una
     * copia legitima y corta el zip bomba antes de que agote la memoria.
     */
    private val maxEntryBytes: Long = MAX_ENTRY_BYTES,
    /** Tope de entradas: cada una cuesta una derivacion de clave antes de validarse. */
    private val maxEntries: Int = MAX_ENTRIES,
) {

    fun read(input: InputStream, password: String): BackupContent {
        val encryptions = try {
            readEntries(input)
        } catch (e: CorruptBackupException) {
            throw e
        } catch (e: Exception) {
            // Un zip ilegible es una copia corrupta, no un fallo del programa: la UI
            // muestra el mensaje y no se importa nada. Se guarda el tipo real para
            // poder diagnosticarlo.
            throw CorruptBackupException("No se pudo leer el zip (${e.javaClass.simpleName})")
        }
        if (encryptions.isEmpty()) throw CorruptBackupException("La copia está vacía")

        val settingsEntry = encryptions[BackupArchive.SETTINGS_ENTRY]
            ?: throw CorruptBackupException("A la copia le falta ${BackupArchive.SETTINGS_ENTRY}")

        val names = encryptions[BackupArchive.MANIFEST_ENTRY]
            ?.let { BackupManifest.parse(crypto.decrypt(password, it).toString(Charsets.UTF_8)) }
            .orEmpty()

        val profiles = encryptions
            .filterKeys { name ->
                name != BackupArchive.SETTINGS_ENTRY && name != BackupArchive.MANIFEST_ENTRY
            }
            .mapNotNull { (name, bytes) ->
                val id = BackupArchive.configIdFromEntry(name) ?: return@mapNotNull null
                val text = crypto.decrypt(password, bytes).toString(Charsets.UTF_8)
                // Sin manifiesto el nombre es el id: la copia sigue importando.
                BackupProfile(id = id, name = names[id] ?: id, configText = text)
            }

        val settings = BackupSettings.parse(
            crypto.decrypt(password, settingsEntry).toString(Charsets.UTF_8),
        )
        return BackupContent(profiles = profiles, settings = settings)
    }

    /** Entradas del zip por nombre; rechaza cualquier nombre peligroso. */
    private fun readEntries(input: InputStream): Map<String, ByteArray> {
        val encryptions = mutableMapOf<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!BackupArchive.isSafeEntryName(entry.name)) {
                    throw CorruptBackupException("Entrada con nombre no válido: ${entry.name}")
                }
                if (encryptions.size >= maxEntries) {
                    throw CorruptBackupException("La copia tiene demasiadas entradas")
                }
                encryptions[entry.name] = readCapped(zip)
            }
        }
        return encryptions
    }

    /**
     * Lee una entrada sin pasarse del tope. La copia la elige el usuario, asi que su
     * contenido no es de fiar: sin este tope, una entrada muy comprimida puede agotar
     * la memoria del proceso.
     */
    private fun readCapped(zip: ZipInputStream): ByteArray {
        val salida = ByteArrayOutputStream()
        val bloque = ByteArray(CHUNK_BYTES)
        var leidos = 0L
        while (true) {
            val cuenta = zip.read(bloque)
            if (cuenta < 0) break
            leidos += cuenta
            if (leidos > maxEntryBytes) {
                throw CorruptBackupException("Entrada demasiado grande")
            }
            salida.write(bloque, 0, cuenta)
        }
        return salida.toByteArray()
    }

    private companion object {
        /** Holgado para una copia legitima; corto para el abuso. */
        const val MAX_ENTRY_BYTES: Long = 1_048_576L

        /** La app escribe una entrada por perfil mas dos fijas: 500 perfiles de margen. */
        const val MAX_ENTRIES: Int = 512

        const val CHUNK_BYTES: Int = 8_192
    }
}
