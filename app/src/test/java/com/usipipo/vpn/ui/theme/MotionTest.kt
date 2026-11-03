package com.usipipo.vpn.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Combinación del ajuste de animaciones con la reducción de movimiento del sistema. */
class MotionTest {

    @Test
    fun `desactivar las animaciones las apaga aunque el sistema no las reduzca`() {
        assertTrue(effectiveReduceMotion(animationsEnabled = false, systemReducesMotion = false))
    }

    @Test
    fun `el sistema sigue mandando cuando las animaciones estan activas`() {
        assertTrue(effectiveReduceMotion(animationsEnabled = true, systemReducesMotion = true))
        assertFalse(effectiveReduceMotion(animationsEnabled = true, systemReducesMotion = false))
    }
}
