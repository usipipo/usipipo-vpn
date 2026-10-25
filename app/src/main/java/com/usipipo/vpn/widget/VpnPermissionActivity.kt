package com.usipipo.vpn.widget

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.usipipo.vpn.UsipipoApp
import com.usipipo.vpn.diagnostics.AppLog
import com.usipipo.vpn.quickconnect.QuickConnectAction
import com.usipipo.vpn.quickconnect.QuickConnectController
import kotlinx.coroutines.launch

class VpnPermissionActivity : ComponentActivity() {

    private val controller: QuickConnectController
        get() = (application as UsipipoApp).container.quickConnectController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                handleAction(controller.toggle())
            }
        } else {
            controller.onPermissionDenied()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent == null) {
            lifecycleScope.launch {
                handleAction(controller.toggle())
            }
        } else {
            permissionLauncher.launch(permissionIntent)
        }
    }

    private fun handleAction(action: QuickConnectAction) {
        when (action) {
            QuickConnectAction.VpnPermissionRequired -> {
                AppLog.w("Permiso", "El permiso VPN sigue sin estar concedido")
            }

            QuickConnectAction.OpenImporter -> WidgetIntents.openImporter(this)
            QuickConnectAction.DisconnectRequested,
            is QuickConnectAction.ConnectRequested,
            is QuickConnectAction.ConfigUnreadable,
            -> Unit
        }
        finish()
    }
}
