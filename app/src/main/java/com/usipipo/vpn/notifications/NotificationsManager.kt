package com.usipipo.vpn.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.usipipo.vpn.MainActivity
import com.usipipo.vpn.R
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.settings.AppSettings
import com.usipipo.vpn.tunnel.Throughput
import com.usipipo.vpn.ui.details.formatRate
import com.usipipo.vpn.ui.details.uptimeText

/**
 * Avisos propios de la app: estado del túnel con tráfico, desconexión o fallo, y
 * actualización disponible.
 *
 * No publica la notificación del servicio en primer plano del túnel: esa la
 * gestiona `TunnelForegroundService` y Android la exige mientras el servicio esté
 * activo. Aquí decide [NoticePolicy], así que con el interruptor general apagado,
 * con el tipo de aviso apagado o sin permiso de notificaciones no se publica nada
 * y la app sigue funcionando.
 */
class NotificationsManager(
    private val context: Context,
    private val settings: AppSettings,
) {

    /**
     * Crea los canales propios una sola vez. `NotificationManager` los actualiza si
     * ya existen, así que llamarlo en cada arranque es seguro e idempotente.
     */
    fun ensureChannels() {
        val estado = NotificationChannel(
            CHANNEL_STATUS,
            context.getString(R.string.notifications_channel_status_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notifications_channel_status_description)
            setSound(null, null)
            enableVibration(false)
        }
        val avisos = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.notifications_channel_alerts_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notifications_channel_alerts_description)
        }
        manager()?.createNotificationChannel(estado)
        manager()?.createNotificationChannel(avisos)
    }

    /**
     * Notificación de estado con tráfico: velocidad de bajada y subida, y tiempo de
     * conexión. Caduca sola para no dejar avisos huérfanos si el túnel se apaga sin
     * avisar (RF-3).
     */
    fun publishTraffic(profileName: String, throughput: Throughput, sinceMs: Long, nowMs: Long) {
        if (!allowedTraffic()) {
            cancelTraffic()
            return
        }
        val title = profileName.ifBlank { context.getString(R.string.app_name) }
        val text = context.getString(
            R.string.notifications_traffic_text,
            formatRate(throughput.downBps),
            formatRate(throughput.upBps),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(uptimeText(sinceMs, nowMs))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setTimeoutAfter(STATUS_TIMEOUT_MS)
            .setContentIntent(openApp())
            .build()
        manager()?.notify(ID_STATUS, notification)
    }

    /** Retira la notificación de estado: el túnel ya no está activo. */
    fun cancelTraffic() {
        manager()?.cancel(ID_STATUS)
    }

    /** Aviso de desconexión o fallo del túnel (RF-4). */
    fun publishDisconnect(state: TunnelState) {
        val textos = when (state) {
            is TunnelState.NoResponse -> R.string.notifications_notice_no_response_title to
                R.string.notifications_notice_no_response_text
            is TunnelState.Failed -> R.string.notifications_notice_failed_title to
                R.string.notifications_notice_failed_text
            else -> return
        }
        if (!NoticePolicy.allowed(
                master = settings.notificationsEnabled,
                type = settings.notificationDisconnect,
                permissionGranted = granted(),
            )
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(context.getString(textos.first))
            .setContentText(context.getString(textos.second))
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()
        manager()?.notify(ID_DISCONNECT, notification)
    }

    /** Aviso de actualización disponible: al tocar la app abre el diálogo. */
    fun publishUpdateAvailable(version: String) {
        if (!NoticePolicy.allowed(
                master = settings.notificationsEnabled,
                type = settings.notificationUpdates,
                permissionGranted = granted(),
            )
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(context.getString(R.string.notifications_notice_update_title))
            .setContentText(context.getString(R.string.notifications_notice_update_text, version))
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()
        manager()?.notify(ID_UPDATE, notification)
    }

    private fun allowedTraffic(): Boolean = NoticePolicy.allowed(
        master = settings.notificationsEnabled,
        type = settings.notificationTraffic,
        permissionGranted = granted(),
    )

    /** Al tocar un aviso se abre la app. */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_APP,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Permiso de notificaciones. Por debajo de la API 33 no existe y la comprobación
     * devuelve concedido: los avisos se publican sin pedir nada.
     */
    private fun granted(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** El sistema siempre tiene gestor de notificaciones; el null es solo defensivo. */
    private fun manager(): NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val CHANNEL_STATUS = "estado"
        const val CHANNEL_ALERTS = "avisos"
        const val ID_STATUS = 43
        const val ID_DISCONNECT = 44
        const val ID_UPDATE = 45
        const val REQUEST_OPEN_APP = 2

        /** Caducidad de la notificación de estado: 3 minutos. */
        const val STATUS_TIMEOUT_MS = 180_000L
    }
}
