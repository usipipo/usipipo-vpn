package com.usipipo.vpn.data.geo

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryRepositoryLogTest {

    @Test
    fun `la IP completa del mensaje se sustituye por su forma abreviada`() {
        assertEquals("fallo para 1.2.x.x", sanitizeIp("fallo para 1.2.3.4", "1.2.3.4"))
    }

    @Test
    fun `un mensaje vacio se registra como sin detalle`() {
        assertEquals("sin detalle", sanitizeIp(null, "1.2.3.4"))
        assertEquals("sin detalle", sanitizeIp("", "1.2.3.4"))
    }

    @Test
    fun `la abreviatura conserva los dos primeros octetos`() {
        assertEquals("10.5.x.x", abbreviateIp("10.5.0.2"))
        assertEquals("1.2.x.x", abbreviateIp("1.2.3.4"))
    }
}
