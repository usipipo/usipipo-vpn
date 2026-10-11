package com.usipipo.vpn.diagnostics

/** Registra el error no controlado, deja la marca de aviso y cede al manejador anterior del sistema. */
class CrashHandler(
    private val settings: DiagnosticsSettings,
    private val previous: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, error: Throwable) {
        runCatching {
            AppLog.fatal("Crash", "Cierre inesperado en el hilo ${thread.name}", error)
            settings.markCrashPending()
        }
        previous?.uncaughtException(thread, error)
    }

    companion object {
        fun install(settings: DiagnosticsSettings) {
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(settings, Thread.getDefaultUncaughtExceptionHandler()),
            )
        }
    }
}
