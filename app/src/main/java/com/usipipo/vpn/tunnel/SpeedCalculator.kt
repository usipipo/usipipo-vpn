package com.usipipo.vpn.tunnel

import com.usipipo.vpn.domain.TunnelCounters

/** Velocidad en bytes por segundo. */
data class Throughput(val downBps: Long, val upBps: Long)

/**
 * Convierte contadores acumulados en velocidad, promediando las últimas [window] muestras
 * para que la gráfica no salte. Un contador que baja (reconexión) cuenta como 0.
 */
class SpeedCalculator(private val window: Int = 3) {

    private val samples = ArrayDeque<TunnelCounters>()

    fun add(counters: TunnelCounters): Throughput {
        samples.addLast(counters)
        while (samples.size > window + 1) samples.removeFirst()

        val first = samples.first()
        val last = samples.last()
        val elapsedMs = last.atMs - first.atMs
        if (elapsedMs <= 0L) return Throughput(0L, 0L)

        return Throughput(
            downBps = (last.rxBytes - first.rxBytes).coerceAtLeast(0L) * 1_000L / elapsedMs,
            upBps = (last.txBytes - first.txBytes).coerceAtLeast(0L) * 1_000L / elapsedMs,
        )
    }

    fun reset() = samples.clear()
}
