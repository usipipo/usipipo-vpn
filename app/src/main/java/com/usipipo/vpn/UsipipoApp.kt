package com.usipipo.vpn

import android.app.Application
import android.util.Log
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.diagnostics.CrashHandler
import com.usipipo.vpn.diagnostics.DeviceInfo
import com.usipipo.vpn.diagnostics.LogLevel
import java.io.File

class UsipipoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Lo primero: registro y captura de cierres, para no perder nada del arranque.
        val settings = container.diagnosticsSettings
        AppLog.init(File(filesDir, "logs"), settings.verbose)
        AppLog.mirror = { level, tag, message ->
            when (level) {
                LogLevel.DEBUG -> Log.d(tag, message)
                LogLevel.INFO -> Log.i(tag, message)
                LogLevel.WARN -> Log.w(tag, message)
                LogLevel.ERROR, LogLevel.FATAL -> Log.e(tag, message)
            }
        }
        CrashHandler.install(settings)
        AppLog.i("App", "Inicio · ${DeviceInfo.collect(this).summary}")

        // Comprobación silenciosa de actualizaciones: como máximo una vez cada 24 h.
        container.checkForUpdatesOnStartup()
        AppLog.i("Actualizar", "Comprobación automática al arrancar (versión ${container.currentVersion})")
    }
}
