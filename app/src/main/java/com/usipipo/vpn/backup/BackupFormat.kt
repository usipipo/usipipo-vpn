package com.usipipo.vpn.backup

/**
 * Reconoce el formato de un documento elegido en el selector.
 *
 * Un `.conf` suelto se importa tal cual; un zip es una copia cifrada que pide
 * contraseña. Se mira la firma del fichero, no la extensión: el usuario puede
 * renombrar lo que descarga.
 */
object BackupFormat {

    /** Firma de la cabecera local de un zip (`PK` + versión). */
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    /** ¿Estos bytes son un zip? */
    fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.size >= ZIP_MAGIC.size && ZIP_MAGIC.indices.all { bytes[it] == ZIP_MAGIC[it] }
}
