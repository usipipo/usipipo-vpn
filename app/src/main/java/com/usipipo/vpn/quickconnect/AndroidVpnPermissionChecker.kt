package com.usipipo.vpn.quickconnect

import android.content.Context
import android.net.VpnService

class AndroidVpnPermissionChecker(private val applicationContext: Context) : VpnPermissionChecker {
    override fun isGranted(): Boolean = VpnService.prepare(applicationContext) == null
}
