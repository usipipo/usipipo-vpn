package com.usipipo.vpn.widget

import android.content.Context
import android.content.Intent
import com.usipipo.vpn.quickconnect.QuickConnectAction
import com.usipipo.vpn.quickconnect.QuickConnectController

/**
 * Routing común del widget y del mosaico: decide qué hacer tras la acción de conexión rápida.
 *
 * [launchActivity] deja que cada superficie lance la UI a su manera (el mosaico recoge el panel
 * con `startActivityAndCollapse`); por defecto se usa `Context.startActivity`.
 */
class QuickActionRouter(
    private val context: Context,
    private val controller: QuickConnectController,
    private val launchActivity: (Context, Intent) -> Unit = { ctx, intent -> ctx.startActivity(intent) },
) {
    suspend fun toggle(): Unit {
        when (controller.toggle()) {
            QuickConnectAction.DisconnectRequested,
            is QuickConnectAction.ConnectRequested,
            is QuickConnectAction.ConfigUnreadable,
            -> Unit

            QuickConnectAction.VpnPermissionRequired -> launchActivity(
                context,
                Intent(context, VpnPermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )

            QuickConnectAction.BackgroundStartDenied -> launchActivity(
                context,
                Intent(context, VpnPermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )

            QuickConnectAction.OpenImporter -> launchActivity(
                context,
                WidgetIntents.importerIntent(context),
            )
        }
    }
}
