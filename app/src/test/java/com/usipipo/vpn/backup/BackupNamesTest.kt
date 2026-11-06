package com.usipipo.vpn.backup

import org.junit.Assert.assertEquals
import org.junit.Test

/** Nombres libres al importar perfiles que chocan con uno existente. */
class BackupNamesTest {

    @Test
    fun `un nombre libre se deja como esta`() {
        assertEquals("Casa", BackupNames.suggestUnique("Casa", taken = emptyList()))
    }

    @Test
    fun `un nombre ocupado recibe un numero entre parentesis`() {
        assertEquals("Casa (2)", BackupNames.suggestUnique("Casa", taken = listOf("Casa")))
    }

    @Test
    fun `si el primer numero tambien esta ocupado se prueba el siguiente`() {
        assertEquals("Casa (3)", BackupNames.suggestUnique("Casa", taken = listOf("Casa", "Casa (2)")))
    }

    @Test
    fun `la comparacion no distingue mayusculas ni espacios`() {
        assertEquals("Oficina (2)", BackupNames.suggestUnique(" Oficina ", taken = listOf("oficina")))
    }
}
