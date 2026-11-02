package com.usipipo.vpn.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Paletas de color: claves, tokens y ausencia de huecos. */
class PaletteVariantTest {

    @Test
    fun `cada variante tiene su clave estable`() {
        assertEquals("azul_noche", PaletteVariant.AzulNoche.key)
        assertEquals("verde_bosque", PaletteVariant.VerdeBosque.key)
        assertEquals("ambar", PaletteVariant.Ambar.key)
    }

    @Test
    fun `cada clave recupera su variante`() {
        PaletteVariant.entries.forEach { paleta ->
            assertEquals(paleta, PaletteVariant.fromKey(paleta.key))
        }
    }

    @Test
    fun `una clave desconocida vuelve a azul noche`() {
        assertEquals(PaletteVariant.AzulNoche, PaletteVariant.fromKey("otro"))
        assertEquals(PaletteVariant.AzulNoche, PaletteVariant.fromKey(null))
    }

    @Test
    fun `cada paleta usa el color primario del diseno`() {
        assertEquals(0xFFA8CCFF.toInt(), PaletteVariant.AzulNoche.tokens(isDark = false).primary)
        assertEquals(0xFF4C8DFF.toInt(), PaletteVariant.AzulNoche.tokens(isDark = true).primary)
        assertEquals(0xFF2E7D5B.toInt(), PaletteVariant.VerdeBosque.tokens(isDark = false).primary)
        assertEquals(0xFF5BD98A.toInt(), PaletteVariant.VerdeBosque.tokens(isDark = true).primary)
        assertEquals(0xFFC8861E.toInt(), PaletteVariant.Ambar.tokens(isDark = false).primary)
        assertEquals(0xFFF5C451.toInt(), PaletteVariant.Ambar.tokens(isDark = true).primary)
    }

    @Test
    fun `las paletas no comparten el color primario`() {
        val primarios = PaletteVariant.entries.map { it.tokens(isDark = false).primary }

        assertEquals(primarios.size, primarios.toSet().size)
    }

    @Test
    fun `ningun token se queda sin definir`() {
        PaletteVariant.entries.forEach { paleta ->
            listOf(false, true).forEach { oscuro ->
                val tokens = paleta.tokens(isDark = oscuro)
                val sinDefinir = TOKENS.all { tokens.token(it) == 0 }

                assertEquals(false, sinDefinir, "$paleta en oscuro=$oscuro tiene tokens sin definir")
            }
        }
    }

    @Test
    fun `el modo oscuro no usa los tokens del modo claro`() {
        listOf(PaletteVariant.AzulNoche, PaletteVariant.VerdeBosque, PaletteVariant.Ambar).forEach { paleta ->
            assertNotEquals(paleta.tokens(isDark = false), paleta.tokens(isDark = true))
        }
    }

    /** Todos los tokens de la paleta, para comprobar que ninguno se queda a cero. */
    private enum class Token {
        Primary, OnPrimary, PrimaryContainer, Background, Surface, SurfaceVariant, Outline, Secondary, Error,
        StatusIdle, StatusConnected, StatusError,
        PingGood, PingWarn, PingBad, PingUnknown,
        BottomBarContainer, BottomBarActive, BottomBarInactive, BottomBarActiveLabel, BottomBarInactiveLabel,
        CalmTop, CalmBottom, DriftingTop, DriftingBottom, FlowingTop, FlowingBottom, AlertTop, AlertBottom,
    }

    private companion object {
        val TOKENS = Token.entries

        fun PaletteTokens.token(token: Token): Int = when (token) {
            Token.Primary -> primary
            Token.OnPrimary -> onPrimary
            Token.PrimaryContainer -> primaryContainer
            Token.Background -> background
            Token.Surface -> surface
            Token.SurfaceVariant -> surfaceVariant
            Token.Outline -> outline
            Token.Secondary -> secondary
            Token.Error -> error
            Token.StatusIdle -> statusIdle
            Token.StatusConnected -> statusConnected
            Token.StatusError -> statusError
            Token.PingGood -> pingGood
            Token.PingWarn -> pingWarn
            Token.PingBad -> pingBad
            Token.PingUnknown -> pingUnknown
            Token.BottomBarContainer -> bottomBarContainer
            Token.BottomBarActive -> bottomBarActive
            Token.BottomBarInactive -> bottomBarInactive
            Token.BottomBarActiveLabel -> bottomBarActiveLabel
            Token.BottomBarInactiveLabel -> bottomBarInactiveLabel
            Token.CalmTop -> calmTop
            Token.CalmBottom -> calmBottom
            Token.DriftingTop -> driftingTop
            Token.DriftingBottom -> driftingBottom
            Token.FlowingTop -> flowingTop
            Token.FlowingBottom -> flowingBottom
            Token.AlertTop -> alertTop
            Token.AlertBottom -> alertBottom
        }
    }
}
