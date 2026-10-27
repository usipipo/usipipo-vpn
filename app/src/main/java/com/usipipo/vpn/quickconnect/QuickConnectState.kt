package com.usipipo.vpn.quickconnect

import com.usipipo.vpn.domain.SavedConfig
import com.usipipo.vpn.domain.TunnelState

data class QuickConnectState(
    val tunnel: TunnelState = TunnelState.Disconnected,
    val selectedConfig: SavedConfig? = null,
    val failure: QuickConnectFailure? = null,
)

sealed interface QuickConnectFailure {
    data class ConfigUnreadable(val configId: String) : QuickConnectFailure
}
