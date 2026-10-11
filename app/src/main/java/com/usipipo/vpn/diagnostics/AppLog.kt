package com.usipipo.vpn.diagnostics

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Registro de la app. Reglas:
 * - Nunca debe romper la app: cualquier fallo al registrar se descarta en silencio.
 * - Nunca recibe contenido de un .conf ni de un QR (solo metadatos).
 * - Todo texto pasa por [LogRedactor] antes de salir.
 * - Los mensajes DEBUG solo se guardan en modo detallado.
 * Sin inicializar ([init]) no hace nada, lo que mantiene limpias las pruebas unitarias.
 */
object AppLog {

    @Volatile
    private var writer: FileLogWriter? = null

    @Volatile
    var isVerbose: Boolean = false
        private set

    /** Copia opcional a logcat (la configura UsipipoApp; en pruebas queda en null). */
    @Volatile
    var mirror: ((LogLevel, String, String) -> Unit)? = null

    private val executor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { task ->
            Thread(task, "applog").apply { isDaemon = true }
        }
    }

    private val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun init(dir: File, verbose: Boolean) {
        writer = FileLogWriter(dir)
        isVerbose = verbose
    }

    fun setVerbose(enabled: Boolean) {
        isVerbose = enabled
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message, null)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message, null)
    fun w(tag: String, message: String, error: Throwable? = null) = log(LogLevel.WARN, tag, message, error)
    fun e(tag: String, message: String, error: Throwable? = null) = log(LogLevel.ERROR, tag, message, error)

    fun log(level: LogLevel, tag: String, message: String, error: Throwable?) {
        if (level == LogLevel.DEBUG && !isVerbose) return
        runCatching {
            mirror?.invoke(level, tag, LogRedactor.redact(message))
            val target = writer ?: return
            val line = format(level, tag, message, error)
            executor.execute { runCatching { target.append(line) } }
        }
    }

    /** Para cierres inesperados: escribe de forma síncrona porque el proceso está a punto de morir. */
    fun fatal(tag: String, message: String, error: Throwable?) {
        runCatching {
            val line = format(LogLevel.FATAL, tag, message, error)
            executor.shutdown()
            executor.awaitTermination(500, TimeUnit.MILLISECONDS)
            writer?.append(line)
        }
    }

    fun readLastLines(count: Int): List<String> =
        runCatching { writer?.readLastLines(count) }.getOrNull() ?: emptyList()

    fun readAll(): String = runCatching { writer?.readAll() }.getOrNull() ?: ""

    fun sizeBytes(): Long = runCatching { writer?.sizeBytes() }.getOrNull() ?: 0L

    fun clear() {
        runCatching { writer?.clear() }
    }

    private fun format(level: LogLevel, tag: String, message: String, error: Throwable?): String {
        val head = "${LocalDateTime.now().format(timestamp)} ${level.letter}/$tag: $message"
        val full = if (error == null) head else head + "\n" + error.stackTraceToString().trimEnd()
        return LogRedactor.redact(full)
    }
}
