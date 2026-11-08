package com.usipipo.vpn.ui.settings.storage

import com.usipipo.vpn.diagnostics.AppLog
import java.io.File

/**
 * Lo que ocupa la app, repartido en lo que el usuario puede liberar.
 *
 * Solo son tres categorías porque son las tres que se pueden medir: el `.conf` de cada
 * perfil vive cifrado dentro del directorio de configs, así que el caché de país no se
 * puede separar de los perfiles, y los resultados de ping no se guardan en ningún sitio.
 */
data class StorageBreakdown(
    val profilesBytes: Long = 0L,
    val logBytes: Long = 0L,
    val tempBytes: Long = 0L,
) {
    val totalBytes: Long get() = StorageFormat.total(profilesBytes, logBytes, tempBytes)
}

/** Tamaños leídos de disco para el resumen de Almacenamiento. */
object StorageSizes {

    fun read(configsDir: File, cacheDir: File): StorageBreakdown = StorageBreakdown(
        profilesBytes = configsDir.sizeOfFiles(),
        logBytes = AppLog.sizeBytes(),
        tempBytes = cacheDir.sizeOfFiles(),
    )

    /**
     * Suma recursiva: los perfiles pueden vivir en subdirectorios. Un directorio que
     * no existe mide cero, que es lo que se ve recién instalada la app.
     */
    private fun File.sizeOfFiles(): Long = walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
