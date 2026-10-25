package com.usipipo.vpn.quickconnect

sealed interface QuickConnectAction {
    data object DisconnectRequested : QuickConnectAction
    data class ConnectRequested(val configId: String) : QuickConnectAction
    data object VpnPermissionRequired : QuickConnectAction
    data object OpenImporter : QuickConnectAction
    data class ConfigUnreadable(val configId: String) : QuickConnectAction
}
