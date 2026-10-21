package com.usipipo.vpn.data.geo

import com.usipipo.vpn.domain.PingResult
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de ping nativo hacia un destino dentro del túnel WireGuard.
 *
 * - Ejecuta `ping -c 1 -W 1000 <target>` vía `ProcessBuilder` en [Dispatchers.IO].
 * - Parsea `time=XX ms` de la salida.
 * - Si el binario `ping` no existe o falla, fallback a `InetAddress.isReachable(target, 1000)`:
 *   - true → `PingResult(ms = null, error = false)` (responde, sin latitud precisa).
 *   - false → `PingResult(ms = null, error = true)`.
 * - Nunca lanza: cualquier excepción se degrada a `error = true`.
 */
class PingRepository(
    private val pingRunner: ((String) -> String?)? = null,
    private val reachabilityChecker: ((String, Int) -> Boolean)? = null,
) {

    private val defaultPingRunner: (String) -> String? = { target ->
        runNativePing(target)
    }

    private val defaultReachabilityChecker: (String, Int) -> Boolean = { host, timeout ->
        InetAddress.getByName(host).isReachable(timeout)
    }

    private companion object {
        /** Timeout del proceso `ping` nativo, en segundos. */
        const val PING_TIMEOUT_SECONDS = 3L
    }

    suspend fun ping(target: String): PingResult = withContext(Dispatchers.IO) {
        if (target.isBlank()) return@withContext PingResult(ms = null, error = true)

        val runner = pingRunner ?: defaultPingRunner
        val checker = reachabilityChecker ?: defaultReachabilityChecker

        val output = runner(target)
        val ms = parsePingTime(output)
        if (ms != null) return@withContext PingResult(ms = ms, error = false)

        val reachable = try {
            checker(target, 1000)
        } catch (e: Exception) {
            false
        }
        return@withContext PingResult(ms = null, error = !reachable)
    }

    private fun runNativePing(target: String): String? {
        var process: Process? = null
        return try {
            // `-W` esta en SEGUNDOS en Linux/Android (no en ms): 1 s es el timeout efectivo.
            // redirectErrorStream unifica stderr en stdout: un solo pipe, sin interbloqueo.
            process = ProcessBuilder("ping", "-c", "1", "-W", "1", target)
                .redirectErrorStream(true)
                .start()
            // Se espera PRIMERO con timeout: leer antes bloquearia el hilo de IO hasta el EOF.
            if (!process.waitFor(PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                process.waitFor()
                return null
            }
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            null
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: SecurityException) {
            null
        } finally {
            process?.destroy()
        }
    }

    private fun parsePingTime(output: String?): Long? {
        if (output == null) return null
        val regex = Regex("""time=(\d+(?:\.\d+)?)""")
        val match = regex.find(output)
        return match?.groupValues?.get(1)?.toDouble()?.roundToLong()
    }
}
