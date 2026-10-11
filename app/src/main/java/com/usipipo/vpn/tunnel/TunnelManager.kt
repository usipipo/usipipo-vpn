package com.usipipo.vpn.tunnel

import com.usipipo.vpn.domain.TunnelState
import kotlinx.coroutines.flow.StateFlow

/** Contrato del túnel. La UI no conoce WireGuard: solo este estado y estas dos acciones. */
interface TunnelManager {
    val state: StateFlow<TunnelState>

    /** Arranca el túnel con el texto de un .conf. Requiere el permiso de VPN ya concedido. */
    fun connect(configId: String, configText: String)

    fun disconnect()
}
