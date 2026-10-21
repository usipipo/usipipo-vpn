package com.usipipo.vpn.data.geo

import com.usipipo.vpn.domain.PingResult
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Ejecuta el proceso `ping` y devuelve su salida, o null si no se pudo. */
fun interface PingRunner {
    fun run(target: String): String?
}

/** Comprueba si un host responde dentro del tiempo indicado. */
fun interface ReachabilityChecker {
    fun isReachable(host: String, timeoutMs: Int): Boolean
}

/**
 * Repositorio de ping nativo hacia un destino dentro del túnel WireGuard.
 *
 * - Ejecuta `ping -c 1 -W 1 <target>` vía `ProcessBuilder` en [Dispatchers.IO].
 * - Parsea `time=XX ms` y `time<1 ms` de la salida.
 * - Si el binario `ping` no existe o falla, fallback a `InetAddress.isReachable`:
 *   - true → `PingResult(ms = null, error = false)` (responde, sin latencia precisa).
 *   - false → `PingResult(ms = null, error = true)`.
 * - Nunca lanza: cualquier excepción se degrada a `error = true`.
 */
class PingRepository internal constructor(
    private val runner: PingRunner,
    private val checker: ReachabilityChecker,
) {

    /** Repositorio de producción: proceso `ping` nativo y `InetAddress.isReachable`. */
    constructor() : this(DefaultPingRunner, DefaultReachabilityChecker)

    suspend fun ping(target: String): PingResult = withContext(Dispatchers.IO) {
        if (target.isBlank()) return@withContext PingResult(ms = null, error = true)

        val output = runner.run(target)
        val ms = parsePingTime(output)
        if (ms != null) return@withContext PingResult(ms = ms, error = false)

        val reachable = try {
            checker.isReachable(target, REACHABILITY_TIMEOUT_MS)
        } catch (e: Exception) {
            false
        }
        return@withContext PingResult(ms = null, error = !reachable)
    }

    private fun parsePingTime(output: String?): Long? {
        if (output == null) return null
        // Admite 'time=23.4 ms' y 'time<1 ms' (el ping al .1 del túnel suele ser <1 ms).
        val regex = Regex("""time(?:=|<)(\d+(?:\.\d+)?)""")
        val match = regex.find(output)
        return match?.groupValues?.get(1)?.toDouble()?.roundToLong()
    }

    private companion object {
        /** Timeout del proceso `ping` nativo, en segundos. */
        const val PING_TIMEOUT_SECONDS = 3L

        /** Timeout de la comprobación de alcance, en milisegundos. */
        const val REACHABILITY_TIMEOUT_MS = 1000

        val DefaultPingRunner = PingRunner { target -> runNativePing(target) }

        val DefaultReachabilityChecker = ReachabilityChecker { host, timeout ->
            InetAddress.getByName(host).isReachable(timeout)
        }

        fun runNativePing(target: String): String? {
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
    }
}
