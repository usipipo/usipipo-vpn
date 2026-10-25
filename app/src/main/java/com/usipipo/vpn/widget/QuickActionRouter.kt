package com.usipipo.vpn.widget

import android.content.Context
import android.content.Intent
import com.usipipo.vpn.quickconnect.QuickConnectAction
import com.usipipo.vpn.quickconnect.QuickConnectController

class QuickActionRouter(
    private val context: Context,
    private val controller: QuickConnectController,
) {
    suspend fun toggle(): Unit {
        when (controller.toggle()) {
            QuickConnectAction.DisconnectRequested,
            is QuickConnectAction.ConnectRequested,
            is QuickConnectAction.ConfigUnreadable,
            -> Unit

            QuickConnectAction.VpnPermissionRequired -> context.startActivity(
                Intent(context, VpnPermissionActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )

            QuickConnectAction.OpenImporter -> WidgetIntents.openImporter(context)
        }
    }
}
