package com.usipipo.vpn.diagnostics

/**
 * Tapa secretos antes de que algo llegue al archivo, a logcat o al informe.
 * Código puro (sin Android) para poder probarlo en la JVM.
 */
object LogRedactor {

    /** Líneas de un .conf: `PrivateKey = ...`, `PresharedKey: ...`. */
    private val keyedSecret = Regex("""(?i)\b(PrivateKey|PresharedKey)\s*[=:]\s*\S+""")

    /** Cualquier cadena con forma de clave de WireGuard: 43 caracteres base64 y un '='. */
    private val keyLike = Regex("""(?<![A-Za-z0-9+/])[A-Za-z0-9+/]{43}=(?![A-Za-z0-9+/=])""")

    fun redact(text: String): String =
        text
            .replace(keyedSecret) { match -> "${match.groupValues[1]} = [oculto]" }
            .replace(keyLike, "[clave oculta]")
}
