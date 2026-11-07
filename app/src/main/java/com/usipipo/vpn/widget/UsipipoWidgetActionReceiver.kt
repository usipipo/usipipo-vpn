package com.usipipo.vpn.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.usipipo.vpn.AppContainer
import com.usipipo.vpn.UsipipoApp
import kotlinx.coroutines.launch

/**
 * Recibe la acción privada del botón del widget.
 *
 * No lleva `intent-filter` y va `exported="false"` en el manifiesto: solo la app
 * puede llegar aquí, porque el `PendingIntent` del botón apunta al componente por
 * su nombre. La acción no puede estar en [UsipipoWidgetProvider], que está exportado
 * para que el sistema le mande las actualizaciones del widget: un receptor exportado
 * que atienda una acción privada deja que cualquier app conecte o desconecte el
 * túnel con una emisión dirigida a esa acción.
 */
class UsipipoWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetIntents.ACTION_TOGGLE) return
        val pendingResult = goAsync()
        val container = (context.applicationContext as UsipipoApp).container
        container.applicationScope.launch {
            try {
                QuickActionRouter(context.applicationContext, container.quickConnectController).toggle()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
