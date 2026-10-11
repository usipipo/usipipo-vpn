package com.usipipo.vpn.diagnostics

import java.io.File

/**
 * Registro en archivo con rotación: `usipipo.log` (actual) y `usipipo.log.1` (anterior).
 * Cuando el actual supera [maxBytes], el anterior se descarta y el actual pasa a ser el anterior.
 * Solo usa java.io, así que se prueba en la JVM sin Android.
 */
class FileLogWriter(
    private val dir: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    private val current = File(dir, "usipipo.log")
    private val previous = File(dir, "usipipo.log.1")

    @Synchronized
    fun append(line: String) {
        dir.mkdirs()
        if (current.exists() && current.length() + line.length + 1 > maxBytes) rotate()
        current.appendText(line + "\n")
    }

    /** Todo el contenido, del más antiguo al más reciente. */
    @Synchronized
    fun readAll(): String = buildString {
        if (previous.exists()) append(previous.readText())
        if (current.exists()) append(current.readText())
    }

    @Synchronized
    fun readLastLines(count: Int): List<String> =
        readAll().lines().filter { it.isNotEmpty() }.takeLast(count)

    @Synchronized
    fun sizeBytes(): Long =
        (if (previous.exists()) previous.length() else 0L) +
            (if (current.exists()) current.length() else 0L)

    @Synchronized
    fun clear() {
        previous.delete()
        current.delete()
    }

    private fun rotate() {
        previous.delete()
        current.renameTo(previous)
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 512L * 1024
    }
}
