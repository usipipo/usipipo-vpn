package com.usipipo.vpn.backup

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Perfil tal y como viaja en la copia. */
data class BackupProfile(val id: String, val name: String, val configText: String)

/**
 * Escribe el zip de copia: `settings.json` cifrado y una entrada `<id>.conf.enc`
 * por perfil, cada una con su propia sal e IV.
 *
 * Escribe en un stream que le dan: el destino (SAF, memoria, prueba) lo decide quien
 * llama, igual que el resto de la app.
 */
class BackupWriter(private val crypto: BackupCrypto = BackupCrypto()) {

    fun write(
        profiles: List<BackupProfile>,
        settingsValues: Map<String, Any>,
        password: String,
        output: OutputStream,
    ) {
        ZipOutputStream(output.buffered()).use { zip ->
            writeEntry(zip, BackupArchive.SETTINGS_ENTRY, crypto.encrypt(password, BackupSettings.serialize(settingsValues).toByteArray()))
            profiles.forEach { profile ->
                val name = BackupArchive.entryName(profile.id)
                writeEntry(zip, name, crypto.encrypt(password, profile.configText.toByteArray()))
            }
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content)
        zip.closeEntry()
    }
}
