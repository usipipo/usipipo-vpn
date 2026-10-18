package com.usipipo.vpn.domain

/** Bytes acumulados por el túnel en un instante; [atMs] viene de `SystemClock.elapsedRealtime()`. */
data class TunnelCounters(val rxBytes: Long, val txBytes: Long, val atMs: Long)
