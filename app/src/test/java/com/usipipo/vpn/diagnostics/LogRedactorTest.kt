package com.usipipo.vpn.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorTest {

    // Claves falsas con la forma de las de WireGuard: 43 caracteres base64 y un '='.
    private val keyA = "A".repeat(43) + "="
    private val keyB = "ab+/" + "Z".repeat(39) + "="

    @Test
    fun `tapa PrivateKey y PresharedKey de un conf`() {
        val result = LogRedactor.redact("PrivateKey = $keyA\nPresharedKey = $keyB")

        assertTrue(result.contains("PrivateKey = [oculto]"))
        assertTrue(result.contains("PresharedKey = [oculto]"))
        assertFalse(result.contains(keyA))
        assertFalse(result.contains(keyB))
    }

    @Test
    fun `no distingue mayusculas ni el separador`() {
        val result = LogRedactor.redact("privatekey: $keyA")

        assertFalse(result.contains(keyA))
        assertTrue(result.contains("[oculto]"))
    }

    @Test
    fun `tapa claves sueltas aunque empiecen por signos base64`() {
        val slashKey = "+/" + "A".repeat(41) + "="
        val result = LogRedactor.redact("peer $keyA y otro $slashKey fin")

        assertEquals("peer [clave oculta] y otro [clave oculta] fin", result)
    }

    @Test
    fun `no toca texto normal ni direcciones IP`() {
        val text = "Endpoint 203.0.113.5:64000 · Estado: Conectado (cfg 1a2b3c4d)"

        assertEquals(text, LogRedactor.redact(text))
    }

    @Test
    fun `no toca cadenas base64 mas largas que una clave`() {
        val text = "A".repeat(60) + "="

        assertEquals(text, LogRedactor.redact(text))
    }
}
