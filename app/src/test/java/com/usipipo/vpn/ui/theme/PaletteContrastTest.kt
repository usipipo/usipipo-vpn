package com.usipipo.vpn.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {

    @Test
    fun `el texto oscuro mantiene contraste 4 punto 5 sobre fondo y tarjetas`() {
        listOf(
            AtmospherePalette.DARK_BACKGROUND,
            AtmospherePalette.DARK_SURFACE,
            AtmospherePalette.DARK_SURFACE_VARIANT,
        ).forEach {
            assertTrue("texto sobre $it", contrast(AtmospherePalette.TEXT_DARK, it) >= 4.5)
            assertTrue("secundario sobre $it", contrast(AtmospherePalette.TEXT_DARK_SECONDARY, it) >= 4.5)
        }
    }

    @Test
    fun `los botones oscuros mantienen contraste 4 punto 5`() {
        assertTrue(contrast(AtmospherePalette.DARK_ON_PRIMARY, AtmospherePalette.DARK_PRIMARY) >= 4.5)
        assertTrue(contrast(AtmospherePalette.DARK_ON_SECONDARY, AtmospherePalette.DARK_SECONDARY) >= 4.5)
    }

    @Test
    fun `el acento oscuro se lee como texto de la barra inferior`() {
        assertTrue(contrast(AtmospherePalette.DARK_PRIMARY, AtmospherePalette.DARK_BACKGROUND) >= 4.5)
        assertTrue(contrast(AtmospherePalette.DARK_PRIMARY, AtmospherePalette.DARK_SURFACE) >= 4.5)
    }

    @Test
    fun `la barra inferior mantiene el icono activo por encima de 4 punto 5`() {
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >= 4.5,
        )
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >= 4.5,
        )
    }

    @Test
    fun `la barra inferior mantiene la etiqueta activa por encima de 4 punto 5`() {
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_LABEL_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >= 4.5,
        )
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_LABEL_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >= 4.5,
        )
    }

    @Test
    fun `la barra inferior mantiene el icono inactivo por encima de 3 punto 1`() {
        // Los iconos son elementos no textuales: la WCAG pide 3:1 para ellos.
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >= 3.0,
        )
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >= 3.0,
        )
    }

    @Test
    fun `la etiqueta inactiva de la barra se lee como texto`() {
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >= 4.5,
        )
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >= 4.5,
        )
    }

    @Test
    fun `la pestana activa destaca mas que la inactiva`() {
        // Si la inactiva contrasta mas que la activa, el estado seleccionado es el mas
        // apagado y la barra no dice que pestana esta activa.
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK) >
                contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK, AtmospherePalette.BOTTOM_BAR_DARK),
        )
        assertTrue(
            contrast(AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT) >
                contrast(AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT, AtmospherePalette.BOTTOM_BAR_LIGHT),
        )
    }

    @Test
    fun `la barra inferior no pinta igual la pestana activa y la inactiva`() {
        // La distincion activa/inactiva es de tono, no solo de luminancia.
        assertTrue(
            AtmospherePalette.BOTTOM_BAR_ACTIVE_DARK != AtmospherePalette.BOTTOM_BAR_INACTIVE_DARK,
        )
        assertTrue(
            AtmospherePalette.BOTTOM_BAR_ACTIVE_LIGHT != AtmospherePalette.BOTTOM_BAR_INACTIVE_LIGHT,
        )
    }

    private fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val (hi, lo) = if (la >= lb) la to lb else lb to la
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(v: Long): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
