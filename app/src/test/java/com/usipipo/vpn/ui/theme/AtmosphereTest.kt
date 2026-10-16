package com.usipipo.vpn.ui.theme

import com.usipipo.vpn.domain.TunnelState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class AtmosphereTest {

    @Test
    fun `cada estado del tunel tiene su fase de ambiente`() {
        assertEquals(AtmospherePhase.Calm, atmospherePhase(TunnelState.Disconnected))
        assertEquals(AtmospherePhase.Drifting, atmospherePhase(TunnelState.Connecting("a")))
        assertEquals(AtmospherePhase.Flowing, atmospherePhase(TunnelState.Connected("a")))
        assertEquals(AtmospherePhase.Alert, atmospherePhase(TunnelState.NoResponse("a")))
        assertEquals(AtmospherePhase.Alert, atmospherePhase(TunnelState.Failed("a")))
        assertEquals(AtmospherePhase.Alert, atmospherePhase(TunnelState.Failed(null)))
    }

    @Test
    fun `el texto mantiene contraste 4 punto 5 sobre cada degradado`() {
        val lightGradients = listOf(
            AtmospherePalette.LIGHT_CALM_TOP to AtmospherePalette.LIGHT_CALM_BOTTOM,
            AtmospherePalette.LIGHT_DRIFTING_TOP to AtmospherePalette.LIGHT_DRIFTING_BOTTOM,
            AtmospherePalette.LIGHT_FLOWING_TOP to AtmospherePalette.LIGHT_FLOWING_BOTTOM,
            AtmospherePalette.LIGHT_ALERT_TOP to AtmospherePalette.LIGHT_ALERT_BOTTOM,
        )
        val darkGradients = listOf(
            AtmospherePalette.DARK_CALM_TOP to AtmospherePalette.DARK_CALM_BOTTOM,
            AtmospherePalette.DARK_DRIFTING_TOP to AtmospherePalette.DARK_DRIFTING_BOTTOM,
            AtmospherePalette.DARK_FLOWING_TOP to AtmospherePalette.DARK_FLOWING_BOTTOM,
            AtmospherePalette.DARK_ALERT_TOP to AtmospherePalette.DARK_ALERT_BOTTOM,
        )
        lightGradients.forEach { (top, bottom) ->
            assertTrue("texto sobre $top", contrast(AtmospherePalette.TEXT_LIGHT, top) >= 4.5)
            assertTrue("texto sobre $bottom", contrast(AtmospherePalette.TEXT_LIGHT, bottom) >= 4.5)
        }
        darkGradients.forEach { (top, bottom) ->
            assertTrue("texto sobre $top", contrast(AtmospherePalette.TEXT_DARK, top) >= 4.5)
            assertTrue("texto sobre $bottom", contrast(AtmospherePalette.TEXT_DARK, bottom) >= 4.5)
        }
    }

    @Test
    fun `el texto secundario mantiene contraste 4 punto 5 donde aparece`() {
        // El texto secundario solo se usa sobre Calm (estado vacío) y Alert (aviso sin
        // respuesta); no se exige sobre Flowing, donde no llega a mostrarse.
        val lightSurfaces = listOf(
            AtmospherePalette.LIGHT_CALM_TOP, AtmospherePalette.LIGHT_CALM_BOTTOM,
            AtmospherePalette.LIGHT_ALERT_TOP, AtmospherePalette.LIGHT_ALERT_BOTTOM,
        )
        val darkSurfaces = listOf(
            AtmospherePalette.DARK_CALM_TOP, AtmospherePalette.DARK_CALM_BOTTOM,
            AtmospherePalette.DARK_ALERT_TOP, AtmospherePalette.DARK_ALERT_BOTTOM,
        )
        lightSurfaces.forEach {
            assertTrue("secundario sobre $it", contrast(AtmospherePalette.TEXT_LIGHT_SECONDARY, it) >= 4.5)
        }
        darkSurfaces.forEach {
            assertTrue("secundario sobre $it", contrast(AtmospherePalette.TEXT_DARK_SECONDARY, it) >= 4.5)
        }
    }

    @Test
    fun `el icono del boton mantiene contraste sobre cada color de estado`() {
        val buttons = listOf(
            AtmospherePalette.BUTTON_LIGHT_IDLE, AtmospherePalette.BUTTON_LIGHT_CONNECTED,
            AtmospherePalette.BUTTON_LIGHT_ERROR, AtmospherePalette.BUTTON_DARK_IDLE,
            AtmospherePalette.BUTTON_DARK_CONNECTED, AtmospherePalette.BUTTON_DARK_ERROR,
        )
        buttons.forEach { color ->
            assertTrue("icono sobre $color", contrast(AtmospherePalette.TEXT_LIGHT, color) >= 4.5)
        }
    }

    // Contraste WCAG 2.1 a partir de constantes ARGB (sin tipos de Compose: testeable en JVM).
    private fun contrast(a: Long, b: Long): Double {
        val l1 = max(luminance(a), luminance(b))
        val l2 = min(luminance(a), luminance(b))
        return (l1 + 0.05) / (l2 + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(v: Long): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }
}
