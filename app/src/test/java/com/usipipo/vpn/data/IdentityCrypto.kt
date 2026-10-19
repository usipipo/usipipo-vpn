package com.usipipo.vpn.data

/** Sin cifrado: en los tests del almacén solo importa el índice, no el Keystore. */
internal class IdentityCrypto : Crypto {
    override fun encrypt(plain: ByteArray): ByteArray = plain
    override fun decrypt(blob: ByteArray): ByteArray = blob
}
