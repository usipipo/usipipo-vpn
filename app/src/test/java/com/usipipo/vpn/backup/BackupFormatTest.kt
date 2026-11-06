package com.usipipo.vpn.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Detección del formato del documento elegido. */
class BackupFormatTest {

    @Test
    fun `un zip se reconoce por su firma`() {
        val zip = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x08, 0x08)

        assertTrue(BackupFormat.looksLikeZip(zip))
    }

    @Test
    fun `un conf en claro no es un zip`() {
        assertFalse(BackupFormat.looksLikeZip("[Interface]\nPrivateKey = abc\n".toByteArray()))
    }

    @Test
    fun `un fichero vacio o corto no es un zip`() {
        assertFalse(BackupFormat.looksLikeZip(ByteArray(0)))
        assertFalse(BackupFormat.looksLikeZip(byteArrayOf(0x50, 0x4B)))
    }
}
