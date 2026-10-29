package com.usipipo.vpn.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.MainActivity
import com.usipipo.vpn.R
import com.usipipo.vpn.UsipipoApp
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.domain.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que hospeda el túnel WireGuard.
 *
 * Es el camino único de conexión en segundo plano (widget y mosaico): el sistema exige
 * entrar en primer plano antes de tocar el túnel, y de paso el usuario ve que la VPN
 * sigue viva. Por eso [startForeground] es lo primero que hace el servicio y de forma
 * síncrona: la plataforma da 5 s.
 *
 * El texto del `.conf` nunca viaja por un Intent: el servicio solo recibe el
 * identificador del perfil y lo lee del almacén cifrado dentro de [serviceScope].
 */
class TunnelForegroundService : Service() {

    private val serviceScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val container: AppContainer
        get() = (applicationContext as UsipipoApp).container

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_CONNECT -> connect(intent)
            ACTION_DISCONNECT -> container.tunnelManager.disconnect()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    /** No es un servicio vinculado: nadie lo enlaza, solo lo arranca. */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Entra en primer plano (síncrono) y luego, en la coroutine del servicio, lee el
     * perfil y llama al túnel: nada del túnel se toca antes de estar en primer plano.
     */
    private fun connect(intent: Intent) {
        val configId = intent.getStringExtra(EXTRA_CONFIG_ID)
        if (configId.isNullOrEmpty()) {
            AppLog.w(TAG, "Petición de conexión sin identificador de perfil")
            stopSelf()
            return
        }

        val profileName = container.configStore.configs.value
            .firstOrNull { it.id == configId }
            ?.name
            .orEmpty()

        startForegroundCompat(buildNotification(TunnelState.Connecting(configId), profileName))

        serviceScope.launch {
            val text = container.configStore.readText(configId)
            if (text == null) {
                AppLog.w(TAG, "Configuración no legible en el servicio (cfg ${configId.take(8)})")
            }
            container.tunnelManager.connect(configId, text.orEmpty())
            followTunnelState(profileName)
        }
    }

    /**
     * Mantiene la notificación al día y apaga el servicio cuando el túnel se cae.
     *
     * El primer «Desconectado» se ignora: el túnel arranca en otra coroutine y puede que
     * todavía no haya emitido su estado cuando el servicio empieza a mirar.
     */
    private suspend fun followTunnelState(profileName: String) {
        container.tunnelManager.state
            .dropWhile { it is TunnelState.Disconnected }
            .collect { state ->
                when (state) {
                    TunnelState.Disconnected, is TunnelState.Failed -> stopHostingTunnel()
                    is TunnelState.Connecting, is TunnelState.Connected, is TunnelState.NoResponse ->
                        updateNotification(state, profileName)
                }
            }
    }

    /** El túnel se apagó o falló: fuera de primer plano y sin servicio. */
    private fun stopHostingTunnel() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(state: TunnelState, profileName: String) {
        notificationManager()?.notify(NOTIFICATION_ID, buildNotification(state, profileName))
    }

    private fun buildNotification(state: TunnelState, profileName: String): Notification {
        val content = TunnelNotificationMapper.map(state)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(getString(content.titleRes))
            .setContentText(getString(content.textRes))
            .setSubText(profileName)
            .setOngoing(content.ongoing)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent())
            .addAction(R.drawable.ic_power, getString(R.string.disconnect), disconnectPendingIntent())
            .build()
    }

    /** Al tocar la notificación se abre la app. */
    private fun contentPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            REQUEST_CONTENT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** La acción «Desconectar» va al propio servicio. */
    private fun disconnectPendingIntent(): PendingIntent {
        val intent = Intent(this, TunnelForegroundService::class.java).setAction(ACTION_DISCONNECT)
        return PendingIntent.getService(
            this,
            REQUEST_DISCONNECT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * El tipo `specialUse` solo existe desde la API 34; en las anteriores se entra en
     * primer plano sin tipo (el manifiesto ya declara `specialUse`).
     */
    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager()?.createNotificationChannel(channel)
    }

    /** El sistema siempre tiene gestor de notificaciones; el null es solo defensivo. */
    private fun notificationManager(): NotificationManager? =
        getSystemService(NotificationManager::class.java)

    companion object {
        const val ACTION_CONNECT: String = "com.usipipo.vpn.action.CONNECT"
        const val ACTION_DISCONNECT: String = "com.usipipo.vpn.action.DISCONNECT"
        const val EXTRA_CONFIG_ID: String = "com.usipipo.vpn.extra.CONFIG_ID"

        /**
         * Arranca el servicio en primer plano para conectar un perfil.
         *
         * Si el sistema lo deniega (app en segundo plano sin permiso), se traduce a
         * [BackgroundStartDeniedException] para que la app pueda reaccionar sin depender
         * de clases de Android. El mensaje no lleva ninguna carga útil del perfil.
         */
        fun start(context: Context, configId: String) {
            val intent = Intent(context, TunnelForegroundService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_CONFIG_ID, configId)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                throw BackgroundStartDeniedException(
                    "El sistema denegó el arranque del servicio en primer plano",
                    e,
                )
            }
        }

        private const val CHANNEL_ID = "tunel"
        private const val NOTIFICATION_ID = 42
        private const val REQUEST_CONTENT = 0
        private const val REQUEST_DISCONNECT = 1
        private const val TAG = "Servicio"
    }
}
