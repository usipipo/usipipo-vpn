package com.usipipo.vpn.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cripto de las copias: AES-GCM con clave derivada de la contraseña por
 * PBKDF2-HMAC-SHA256.
 *
 * La sal y el IV se generan por archivo y viajan en la cabecera del payload, así
 * que dos cifrados del mismo texto no dan el mismo resultado. AES-GCM es
 * autenticado: si la contraseña es incorrecta o el texto se manipuló, el descifrado
 * falla en lugar de devolver basura.
 *
 * La contraseña no se guarda en ningún sitio: quien la pierde pierde la copia.
 */
class BackupCrypto(private val iterations: Int = DEFAULT_ITERATIONS) {

    /**
     * Formato del payload: `sal (16) || iv (12) || texto cifrado con tag GCM`.
     * Las tres partes tienen longitud fija salvo el texto, así que leer la
     * cabecera es trivial y no hace falta un contenedor.
     */
    fun encrypt(password: String, plaintext: ByteArray): ByteArray {
        val salt = randomBytes(SALT_BYTES)
        val iv = randomBytes(IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        return salt + iv + cipher.doFinal(plaintext)
    }

    /** Lanza [WrongPasswordException] si la contraseña no abre el payload. */
    fun decrypt(password: String, payload: ByteArray): ByteArray {
        if (payload.size <= SALT_BYTES + IV_BYTES) throw CorruptBackupException("Payload demasiado corto")
        val salt = payload.copyOfRange(0, SALT_BYTES)
        val iv = payload.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
        val body = payload.copyOfRange(SALT_BYTES + IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, iv))
        return try {
            cipher.doFinal(body)
        } catch (e: Exception) {
            // GCM no distingue contraseña incorrecta de texto manipulado: para el
            // usuario es el mismo problema ("no se puede abrir"), y no se importa nada.
            throw WrongPasswordException()
        }
    }

    /** La misma contraseña y la misma sal dan la misma clave: por eso la sal viaja en la cabecera. */
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        return SecretKeySpec(factory.generateSecret(spec).encoded, KEY_ALGORITHM)
    }

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { RANDOM.nextBytes(it) }

    companion object {
        /** Bytes de sal aleatoria por archivo. */
        const val SALT_BYTES: Int = 16

        /** Bytes del IV de GCM (12 es el tamaño recomendado para GCM). */
        const val IV_BYTES: Int = 12

        /** Iteraciones de PBKDF2: caras para el atacante, tolerables en el dispositivo. */
        const val DEFAULT_ITERATIONS: Int = 100_000

        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private val RANDOM = SecureRandom()
    }
}
