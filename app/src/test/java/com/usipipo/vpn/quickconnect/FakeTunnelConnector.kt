package com.usipipo.vpn.quickconnect

/** Conector de prueba: devuelve [result] y anota los perfiles que se le han pedido. */
class FakeTunnelConnector(var result: Boolean = true) : TunnelConnector {

    val requestedIds: MutableList<String> = mutableListOf()

    override suspend fun connect(configId: String): Boolean {
        requestedIds += configId
        return result
    }
}
