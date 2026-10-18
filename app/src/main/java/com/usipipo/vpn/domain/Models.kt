package com.usipipo.vpn.domain

/** Configuración guardada. El contenido (claves) vive cifrado en el almacén, no aquí. */
data class SavedConfig(val id: String, val name: String, val favorite: Boolean = false)

sealed interface TunnelState {
    data object Disconnected : TunnelState
    data class Connecting(val configId: String) : TunnelState

    /** Túnel arriba y con handshake recibido. */
    data class Connected(val configId: String) : TunnelState

    /** Túnel arriba pero sin handshake todavía (el servidor no responde). */
    data class NoResponse(val configId: String) : TunnelState
    data class Failed(val configId: String?) : TunnelState
}

/** El botón principal desconecta mientras haya un túnel activo o intentándolo. */
val TunnelState.isActive: Boolean
    get() = this is TunnelState.Connecting ||
        this is TunnelState.Connected ||
        this is TunnelState.NoResponse

val TunnelState.activeConfigId: String?
    get() = when (this) {
        TunnelState.Disconnected -> null
        is TunnelState.Connecting -> configId
        is TunnelState.Connected -> configId
        is TunnelState.NoResponse -> configId
        is TunnelState.Failed -> configId
    }
