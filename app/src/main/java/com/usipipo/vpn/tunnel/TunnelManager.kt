package com.usipipo.vpn.tunnel

import com.usipipo.vpn.domain.TunnelCounters
import com.usipipo.vpn.domain.TunnelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Contrato del túnel. La UI no conoce WireGuard: solo este estado y estas dos acciones. */
interface TunnelManager {
    val state: StateFlow<TunnelState>

    /** Arranca el túnel con el texto de un .conf. Requiere el permiso de VPN ya concedido. */
    fun connect(configId: String, configText: String)

    fun disconnect()

    /**
     * Contadores acumulados del túnel, una muestra por segundo mientras alguien recoge el flujo.
     * Por defecto no emite nada (implementaciones de prueba).
     */
    fun counters(): Flow<TunnelCounters> = emptyFlow()
}
