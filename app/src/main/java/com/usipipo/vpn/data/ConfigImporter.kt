package com.usipipo.vpn.data

import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.io.IOException

class InvalidConfigException(cause: Throwable? = null) : Exception("Configuración no válida", cause)

/** Valida texto de un .conf (de archivo o QR) con el parser oficial de WireGuard. */
class ConfigImporter {

    /** Devuelve el .conf normalizado (sin comentarios) o [InvalidConfigException]. */
    fun parse(raw: String): Result<String> {
        val text = raw.trim()
        if (text.isEmpty() || text.length > MAX_CHARS) {
            return Result.failure(InvalidConfigException())
        }
        return try {
            val config = Config.parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
            Result.success(config.toWgQuickString())
        } catch (e: BadConfigException) {
            Result.failure(InvalidConfigException(e))
        } catch (e: IOException) {
            Result.failure(InvalidConfigException(e))
        }
    }

    private companion object {
        const val MAX_CHARS = 64 * 1024
    }
}
