package com.usipipo.vpn.ui.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAccessHintTest {

    @Test
    fun `la pista aparece desde android 13`() {
        assertTrue(shouldShowQuickAccessHint(33))
        assertTrue(shouldShowQuickAccessHint(35))
    }

    @Test
    fun `la pista no aparece antes de android 13`() {
        assertFalse(shouldShowQuickAccessHint(32))
        assertFalse(shouldShowQuickAccessHint(26))
    }
}
