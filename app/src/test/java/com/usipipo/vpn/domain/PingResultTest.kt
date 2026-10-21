package com.usipipo.vpn.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingResultTest {

    @Test
    fun `constructor con ms y sin error`() {
        val result = PingResult(ms = 23L, error = false)

        assertEquals(23L, result.ms)
        assertFalse(result.error)
    }

    @Test
    fun `constructor con ms null y error true`() {
        val result = PingResult(ms = null, error = true)

        assertNull(result.ms)
        assertTrue(result.error)
    }

    @Test
    fun `constructor por defecto error es false`() {
        val result = PingResult(ms = 42L)

        assertEquals(42L, result.ms)
        assertFalse(result.error)
    }
}
