package com.usipipo.vpn.ui.details

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormattersTest {

    @Test
    fun `una velocidad alta se muestra en Mbps`() {
        assertEquals("60.00 Mbps", formatRate(7_500_000L, Locale.US))
    }

    @Test
    fun `una velocidad baja se muestra en Kbps`() {
        assertEquals("500 Kbps", formatRate(62_500L, Locale.US))
    }

    @Test
    fun `sin trafico se muestra cero`() {
        assertEquals("0 Kbps", formatRate(0L, Locale.US))
    }

    @Test
    fun `justo bajo 1 Mbps no muestra mil Kbps`() {
        assertEquals("1.00 Mbps", formatRate(124_999L, Locale.US))
    }

    @Test
    fun `el tiempo se muestra como horas minutos y segundos`() {
        assertEquals("00:00:00", formatUptime(0L))
        assertEquals("00:28:15", formatUptime(28 * 60_000L + 15_000L))
        assertEquals("100:00:00", formatUptime(100 * 3_600_000L))
    }

    @Test
    fun `un tiempo negativo se muestra como cero`() {
        assertEquals("00:00:00", formatUptime(-5_000L))
    }

    @Test
    fun `sin hora de conexion no se inventa un tiempo`() {
        assertEquals("--:--:--", uptimeText(sinceMs = 0L, nowMs = 999_999L))
    }

    @Test
    fun `con hora de conexion calcula el tiempo transcurrido`() {
        assertEquals("00:01:05", uptimeText(sinceMs = 10_000L, nowMs = 75_000L))
    }
}
