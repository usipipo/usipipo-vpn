package com.usipipo.vpn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedConfigCountryTest {

    @Test
    fun `copia con pais conserva campos existentes`() {
        val original = SavedConfig("id-1", "Mi VPN", favorite = true)
        val copy = original.copy(countryCode = "DE", countryName = "Germany")
        assertEquals("DE", copy.countryCode)
        assertEquals("Germany", copy.countryName)
        assertTrue(copy.favorite)
        assertEquals("id-1", copy.id)
    }
}
