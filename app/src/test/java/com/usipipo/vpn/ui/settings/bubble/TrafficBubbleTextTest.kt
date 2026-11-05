package com.usipipo.vpn.ui.settings.bubble

import org.junit.Assert.assertEquals
import org.junit.Test

/** Texto de la burbuja: «—» cuando no hay dato, para que nunca quede vacía. */
class TrafficBubbleTextTest {

    @Test
    fun `sin velocidad se muestra el guion`() {
        assertEquals("—", TrafficBubbleText.speed(null))
    }

    @Test
    fun `con velocidad se formatea en bits`() {
        assertEquals("8.00 Mbps", TrafficBubbleText.speed(1_000_000L))
        assertEquals("8 Kbps", TrafficBubbleText.speed(1_000L))
    }

    @Test
    fun `sin hora de conexion se muestra el guion`() {
        assertEquals("—", TrafficBubbleText.time(sinceMs = 0L, nowMs = 5_000L))
        assertEquals("—", TrafficBubbleText.time(sinceMs = null, nowMs = 5_000L))
    }

    @Test
    fun `con hora de conexion se formatea el tiempo`() {
        assertEquals("00:00:05", TrafficBubbleText.time(sinceMs = 1_000L, nowMs = 6_000L))
    }

    @Test
    fun `el pais prefiere el nombre y cae al codigo`() {
        assertEquals("Germany", TrafficBubbleText.country("Germany", "DE"))
        assertEquals("DE", TrafficBubbleText.country(null, "DE"))
        assertEquals("—", TrafficBubbleText.country(null, null))
    }
}
