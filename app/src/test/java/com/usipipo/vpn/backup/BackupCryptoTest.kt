package com.usipipo.vpn.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cripto de las copias: roundtrip, contraseña incorrecta y sal/IV por archivo. */
class BackupCryptoTest {

    /** Pocas iteraciones: el test no mide el coste del KDF, solo el roundtrip. */
    private val crypto = BackupCrypto(iterations = 1_000)

    @Test
    fun `cifrar y descifrar devuelve el texto original`() {
        val secreto = "[Interface]\nPrivateKey = abc\n"

        val payload = crypto.encrypt("contraseña", secreto.toByteArray())

        assertArrayEquals(secreto.toByteArray(), crypto.decrypt("contraseña", payload))
    }

    @Test
    fun `con la contraseña incorrecta no se descifra`() {
        val payload = crypto.encrypt("contraseña", "secreto".toByteArray())

        try {
            crypto.decrypt("otra contraseña", payload)
            throw AssertionError("se esperaba WrongPasswordException")
        } catch (e: WrongPasswordException) {
            assertFalse(e.message.isNullOrBlank())
        }
    }

    @Test
    fun `cada cifrado usa su propia sal y su propio iv`() {
        val primero = crypto.encrypt("contraseña", "secreto".toByteArray())
        val segundo = crypto.encrypt("contraseña", "secreto".toByteArray())

        // La cabecera (sal + iv) ocupa las primeras 28 posiciones del payload.
        assertFalse(primizadoInicial(primero).contentEquals(primizadoInicial(segundo)))
        assertFalse(primero.contentEquals(segundo))
    }

    @Test
    fun `un payload truncado es una copia corrupta`() {
        try {
            crypto.decrypt("contraseña", ByteArray(10))
            throw AssertionError("se esperaba CorruptBackupException")
        } catch (e: CorruptBackupException) {
            assertTrue(true)
        }
    }

    @Test
    fun `un texto manipulado no se descifra`() {
        val payload = crypto.encrypt("contraseña", "secreto".toByteArray())
        payload[payload.size - 1] = (payload[payload.size - 1] + 1).toByte()

        try {
            crypto.decrypt("contraseña", payload)
            throw AssertionError("se esperaba WrongPasswordException")
        } catch (e: WrongPasswordException) {
            assertTrue(true)
        }
    }

    private fun primizadoInicial(payload: ByteArray): ByteArray =
        payload.copyOfRange(0, BackupCrypto.SALT_BYTES + BackupCrypto.IV_BYTES)
}
