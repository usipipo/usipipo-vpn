package com.usipipo.vpn.quickconnect

/**
 * Contrato para pedir la conexión del túnel desde cualquier punto de la app
 * (pantalla de Inicio, widget, mosaico de Ajustes rápidos).
 */
interface TunnelConnector {

    /**
     * Pide arrancar el túnel con el perfil [configId].
     *
     * Devuelve `true` si el arranque se aceptó y `false` si el sistema lo denegó
     * (arranque en segundo plano no permitido).
     */
    suspend fun connect(configId: String): Boolean
}
