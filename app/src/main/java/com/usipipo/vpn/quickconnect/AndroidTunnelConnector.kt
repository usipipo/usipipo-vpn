package com.usipipo.vpn.quickconnect

import com.usipipo.vpn.diagnostics.AppLog

/**
 * Conector de producción: arranca el servicio en primer plano del túnel.
 *
 * El arranque se inyecta como `(String) -> Unit` para que esta clase no dependa de Android
 * y se pueda probar en la JVM. En producción apunta a
 * `TunnelForegroundService.start(appContext, configId)`.
 */
class AndroidTunnelConnector(private val startService: (String) -> Unit) : TunnelConnector {

    override suspend fun connect(configId: String): Boolean =
        try {
            startService(configId)
            true
        } catch (e: Exception) {
            AppLog.w(TAG, "No se pudo arrancar el servicio del túnel (cfg ${configId.take(8)})", e)
            false
        }

    private companion object {
        const val TAG = "Conector"
    }
}
