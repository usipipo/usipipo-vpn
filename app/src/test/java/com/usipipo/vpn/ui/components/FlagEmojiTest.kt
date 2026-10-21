package com.usipipo.vpn.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FlagEmojiTest {

    @Test
    fun `codigo de dos letras genera el indicador regional`() {
        assertEquals("\uD83C\uDDE9\uD83C\uDDEA", flagEmoji("DE"))
    }

    @Test
    fun `acepta codigo en minusculas`() {
        assertEquals("\uD83C\uDDE9\uD83C\uDDEA", flagEmoji("de"))
    }

    @Test
    fun `codigo invalido devuelve cadena vacia`() {
        // Regresion: un "null" literal (indice antiguo) no debe generar una bandera falsa.
        assertEquals("", flagEmoji("null"))
        assertEquals("", flagEmoji("D"))
        assertEquals("", flagEmoji("12"))
        assertEquals("", flagEmoji(""))
    }
}
