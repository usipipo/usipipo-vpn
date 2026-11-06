package com.usipipo.vpn.backup

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
class BackupReader(private val crypto: BackupCrypto = BackupCrypto()) {

    fun read(input: InputStream, password: String): BackupContent {
        val encryptions = mutableMapOf<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!BackupArchive.isSafeEntryName(entry.name)) {
                    throw CorruptBackupException("Entrada con nombre no válido: ${entry.name}")
                }
                encryptions[entry.name] = zip.readBytes()
            }
        }
        if (encryptions.isEmpty()) throw CorruptBackupException()

        val settingsEntry = encryptions[BackupArchive.SETTINGS_ENTRY]
            ?: throw CorruptBackupException("A la copia le falta ${BackupArchive.SETTINGS_ENTRY}")

        val profiles = encryptions
            .filterKeys { name -> name != BackupArchive.SETTINGS_ENTRY }
            .mapNotNull { (name, bytes) ->
                val id = BackupArchive.configIdFromEntry(name) ?: return@mapNotNull null
                val text = crypto.decrypt(password, bytes).toString(Charsets.UTF_8)
                BackupProfile(id = id, name = id, configText = text)
            }

        val settings = BackupSettings.parse(
            crypto.decrypt(password, settingsEntry).toString(Charsets.UTF_8),
        )
        return BackupContent(profiles = profiles, settings = settings)
    }
}
