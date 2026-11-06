package com.usipipo.vpn.backup

/** Lo que identifica a un perfil a efectos de duplicados: clave pública y endpoint. */
data class ConfigFingerprint(val publicKey: String?, val endpoint: String?)

/**
 * Decide si un perfil entrante ya está en el dispositivo.
 *
 * Se comparan la clave pública y el endpoint del `.conf`, y el nombre visible. Con
 * que coincida uno de los tres ya es duplicado: al usuario se le pregunta qué hacer
 * (sobrescribir, mantener ambos u omitir), no se decide por él.
 */
object BackupFingerprint {

    /** Lee `PublicKey` y `Endpoint` del `.conf`. Sin ellos, la huella queda a null. */
    fun of(configText: String): ConfigFingerprint {
        var publicKey: String? = null
        var endpoint: String? = null
        configText.lineSequence().forEach { raw ->
            val separator = raw.indexOf('=')
            if (separator < 0) return@forEach
            val key = raw.substring(0, separator).trim().lowercase()
            val value = raw.substring(separator + 1).trim()
            when (key) {
                "publickey" -> if (publicKey == null && value.isNotEmpty()) publicKey = value
                "endpoint" -> if (endpoint == null && value.isNotEmpty()) endpoint = value
            }
        }
        return ConfigFingerprint(publicKey = publicKey, endpoint = endpoint)
    }

    /** ¿El perfil entrante repite el existente? */
    fun isDuplicate(
        existingName: String,
        existingText: String,
        incomingName: String,
        incomingText: String,
    ): Boolean {
        val existing = of(existingText)
        val incoming = of(incomingText)
        val sameKey = existing.publicKey != null && existing.publicKey == incoming.publicKey
        val sameEndpoint = existing.endpoint != null && existing.endpoint == incoming.endpoint
        val sameName = existingName.equals(incomingName, ignoreCase = true)
        return sameKey || sameEndpoint || sameName
    }
}
