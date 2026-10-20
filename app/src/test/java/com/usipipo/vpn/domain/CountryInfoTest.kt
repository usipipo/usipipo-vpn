package com.usipipo.vpn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryInfoTest {

    @Test
    fun `constructor con valores por defecto`() {
        val info = CountryInfo(ip = "1.2.3.4", countryCode = "DE", countryName = "Germany", fetchedAt = 1000L)

        assertEquals("1.2.3.4", info.ip)
        assertEquals("DE", info.countryCode)
        assertEquals("Germany", info.countryName)
        assertEquals(1000L, info.fetchedAt)
        assertFalse(info.error)
    }

    @Test
    fun `constructor con error true`() {
        val info = CountryInfo(ip = "1.2.3.4", countryCode = null, countryName = null, fetchedAt = 1000L, error = true)

        assertTrue(info.error)
        assertNull(info.countryCode)
        assertNull(info.countryName)
    }
}
