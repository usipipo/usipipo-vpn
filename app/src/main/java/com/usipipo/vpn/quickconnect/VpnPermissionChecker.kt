package com.usipipo.vpn.quickconnect

fun interface VpnPermissionChecker {
    fun isGranted(): Boolean
}
