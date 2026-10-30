package com.usipipo.vpn.widget

import com.usipipo.vpn.quickconnect.QuickConnectAction
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickActionRouterTest {

    @Test
    fun `denegacion del sistema abre la activity de permiso`() {
        assertEquals(QuickActionTarget.PermissionActivity, targetFor(QuickConnectAction.BackgroundStartDenied))
        assertEquals(QuickActionTarget.PermissionActivity, targetFor(QuickConnectAction.VpnPermissionRequired))
        assertEquals(QuickActionTarget.Importer, targetFor(QuickConnectAction.OpenImporter))
        assertEquals(QuickActionTarget.None, targetFor(QuickConnectAction.DisconnectRequested))
        assertEquals(QuickActionTarget.None, targetFor(QuickConnectAction.ConnectRequested("a")))
        assertEquals(QuickActionTarget.None, targetFor(QuickConnectAction.ConfigUnreadable("a")))
    }
}
