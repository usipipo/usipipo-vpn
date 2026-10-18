package com.usipipo.vpn.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfSummaryTest {

    private val conf = """
        [Interface]
        PrivateKey = clave-de-prueba
        Address = 10.66.66.2/32, fd42:42:42::2/128

        [Peer]
        PublicKey = otra-clave-de-prueba
        Endpoint = vpn.example.com:51820
        AllowedIPs = 0.0.0.0/0, ::/0
    """.trimIndent()

    @Test
    fun `extrae el endpoint`() {
        assertEquals("vpn.example.com:51820", ConfSummary.parse(conf).endpoint)
    }

    @Test
    fun `detecta que enruta IPv6`() {
        assertTrue(ConfSummary.parse(conf).routesIpv6)
    }

    @Test
    fun `sin la ruta IPv6 no la marca`() {
        val onlyV4 = conf.replace("0.0.0.0/0, ::/0", "0.0.0.0/0")

        assertFalse(ConfSummary.parse(onlyV4).routesIpv6)
    }

    @Test
    fun `sin endpoint devuelve null`() {
        val noEndpoint = conf.lines().filterNot { it.startsWith("Endpoint") }.joinToString("\n")

        assertNull(ConfSummary.parse(noEndpoint).endpoint)
    }

    @Test
    fun `no distingue mayusculas ni espacios en las claves`() {
        val messy = "  endpoint=1.2.3.4:51820  \nallowedips =  ::/0 "

        val summary = ConfSummary.parse(messy)

        assertEquals("1.2.3.4:51820", summary.endpoint)
        assertTrue(summary.routesIpv6)
    }

    @Test
    fun `un texto vacio no rompe nada`() {
        assertEquals(ConfSummary(endpoint = null, routesIpv6 = false), ConfSummary.parse(""))
    }
}
