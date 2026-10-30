package com.usipipo.vpn.tunnel

import com.usipipo.vpn.R
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.domain.isActive

/** Título, texto y carácter vivo de la notificación del servicio en primer plano. */
data class TunnelNotification(val titleRes: Int, val textRes: Int, val ongoing: Boolean)

/**
 * Mapea el estado del túnel a la notificación del servicio en primer plano.
 *
 * Función pura, sin dependencias de Android: se prueba en la JVM. La notificación vive
 * mientras hay túnel (conectando, conectado o sin respuesta); si el túnel se apaga o
 * falla, el servicio la retira.
 */
object TunnelNotificationMapper {

    fun map(state: TunnelState): TunnelNotification = TunnelNotification(
        titleRes = R.string.app_name,
        textRes = textFor(state),
        // «Vivo» es exactamente el conjunto de estados con túnel en pie.
        ongoing = state.isActive,
    )

    private fun textFor(state: TunnelState): Int = when (state) {
        TunnelState.Disconnected -> R.string.widget_status_disconnected
        is TunnelState.Connecting -> R.string.widget_status_connecting
        is TunnelState.Connected -> R.string.widget_status_active
        is TunnelState.NoResponse -> R.string.widget_status_no_response
        is TunnelState.Failed -> R.string.widget_status_failed
    }
}
