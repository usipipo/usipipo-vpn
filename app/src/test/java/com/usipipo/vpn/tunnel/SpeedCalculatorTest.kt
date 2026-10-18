package com.usipipo.vpn.tunnel

import com.usipipo.vpn.domain.TunnelCounters
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedCalculatorTest {

    @Test
    fun `una sola muestra no da velocidad`() {
        val calculator = SpeedCalculator()

        assertEquals(Throughput(0L, 0L), calculator.add(TunnelCounters(1_000L, 500L, 0L)))
    }

    @Test
    fun `velocidad constante entre dos muestras`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 0L))

        val result = calculator.add(TunnelCounters(2_000_000L, 500_000L, 1_000L))

        assertEquals(Throughput(2_000_000L, 500_000L), result)
    }

    @Test
    fun `suaviza con la ventana de muestras`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 0L))
        calculator.add(TunnelCounters(1_000_000L, 0L, 1_000L))
        calculator.add(TunnelCounters(1_000_000L, 0L, 2_000L))

        val result = calculator.add(TunnelCounters(4_000_000L, 0L, 3_000L))

        // 4 MB en 3 s, no los 3 MB del último segundo.
        assertEquals(1_333_333L, result.downBps)
    }

    @Test
    fun `descarta las muestras mas alla de la ventana`() {
        val calculator = SpeedCalculator()
        listOf(0L, 1_000_000L, 2_000_000L, 3_000_000L).forEachIndexed { i, rx ->
            calculator.add(TunnelCounters(rx, 0L, i * 1_000L))
        }

        val result = calculator.add(TunnelCounters(10_000_000L, 0L, 4_000L))

        // Ventana de 4 muestras: de 1 MB a 10 MB en 3 s.
        assertEquals(3_000_000L, result.downBps)
    }

    @Test
    fun `contadores que se reinician no dan velocidad negativa`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(5_000_000L, 5_000_000L, 0L))

        val result = calculator.add(TunnelCounters(100L, 100L, 1_000L))

        assertEquals(Throughput(0L, 0L), result)
    }

    @Test
    fun `dos muestras en el mismo instante no dividen entre cero`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 1_000L))

        assertEquals(Throughput(0L, 0L), calculator.add(TunnelCounters(500L, 500L, 1_000L)))
    }

    @Test
    fun `reset olvida las muestras anteriores`() {
        val calculator = SpeedCalculator()
        calculator.add(TunnelCounters(0L, 0L, 0L))
        calculator.add(TunnelCounters(1_000_000L, 0L, 1_000L))

        calculator.reset()

        assertEquals(Throughput(0L, 0L), calculator.add(TunnelCounters(9_000_000L, 0L, 2_000L)))
    }
}
