package com.usipipo.vpn.diagnostics

import java.util.concurrent.TimeUnit

/**
 * Últimas líneas de logcat del propio proceso (incluye lo que escribe el motor nativo de WireGuard).
 * Android solo muestra a una app sus propios mensajes, sin permisos especiales (--pid requiere API 24+).
 * Llamar fuera del hilo principal.
 */
object LogcatCollector {

    fun collect(maxLines: Int = 2000): String? {
        var process: Process? = null
        return try {
            val pid = android.os.Process.myPid()
            process = ProcessBuilder(
                "logcat", "-d", "-v", "threadtime", "-t", maxLines.toString(), "--pid=$pid",
            ).redirectErrorStream(true).start()
            val text = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor(2, TimeUnit.SECONDS)
            text
        } catch (e: Exception) {
            AppLog.w("Logcat", "No se pudo leer logcat (${e.javaClass.simpleName})")
            null
        } finally {
            process?.destroy()
        }
    }
}
