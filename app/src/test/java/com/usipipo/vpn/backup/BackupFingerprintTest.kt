package com.usipipo.vpn.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Detección de duplicados al importar: clave pública, endpoint y nombre. */
class BackupFingerprintTest {

    @Test
    fun `la huella lee clave publica y endpoint del conf`() {
        val huella = BackupFingerprint.of(conf(clave = "PUBLIC-KEY", endpoint = "1.2.3.4:51820"))

        assertEquals("PUBLIC-KEY", huella.publicKey)
        assertEquals("1.2.3.4:51820", huella.endpoint)
    }

    @Test
    fun `sin clave publica la huella la deja vacia`() {
        val huella = BackupFingerprint.of(conf(clave = null, endpoint = "1.2.3.4:51820"))

        assertNull(huella.publicKey)
        assertEquals("1.2.3.4:51820", huella.endpoint)
    }

    @Test
    fun `la misma clave publica es duplicado aunque cambie el nombre`() {
        val esDuplicado = BackupFingerprint.isDuplicate(
            existingName = "Casa",
            existingText = conf(clave = "PUBLIC-KEY", endpoint = "1.1.1.1:51820"),
            incomingName = "Casa copia",
            incomingText = conf(clave = "PUBLIC-KEY", endpoint = "2.2.2.2:51820"),
        )

        assertTrue(esDuplicado)
    }

    @Test
    fun `el mismo endpoint es duplicado`() {
        val esDuplicado = BackupFingerprint.isDuplicate(
            existingName = "Casa",
            existingText = conf(clave = "CLAVE-A", endpoint = "1.2.3.4:51820"),
            incomingName = "Oficina",
            incomingText = conf(clave = "CLAVE-B", endpoint = "1.2.3.4:51820"),
        )

        assertTrue(esDuplicado)
    }

    @Test
    fun `el mismo nombre ignorando mayusculas es duplicado`() {
        val esDuplicado = BackupFingerprint.isDuplicate(
            existingName = "Casa",
            existingText = conf(clave = "CLAVE-A", endpoint = "1.1.1.1:51820"),
            incomingName = "casa",
            incomingText = conf(clave = "CLAVE-B", endpoint = "2.2.2.2:51820"),
        )

        assertTrue(esDuplicado)
    }

    @Test
    fun `perfiles distintos no son duplicados`() {
        val esDuplicado = BackupFingerprint.isDuplicate(
            existingName = "Casa",
            existingText = conf(clave = "CLAVE-A", endpoint = "1.1.1.1:51820"),
            incomingName = "Oficina",
            incomingText = conf(clave = "CLAVE-B", endpoint = "2.2.2.2:51820"),
        )

        assertFalse(esDuplicado)
    }

    private fun conf(clave: String?, endpoint: String): String = buildString {
        append("[Interface]\n")
        append("PrivateKey = clave-privada\n")
        append("[Peer]\n")
        append("PublicKey = ${clave ?: ""}\n")
        append("Endpoint = $endpoint\n")
    }
}
