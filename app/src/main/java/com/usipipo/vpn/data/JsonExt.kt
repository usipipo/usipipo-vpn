package com.usipipo.vpn.data

import org.json.JSONObject

/**
 * Lee un string del JSON devolviendo `null` si la clave falta, si su valor es
 * `null` explícito o si está vacío.
 *
 * No se usa `JSONObject.optString(key, fallback)` porque su fallback no es fiable:
 * en Android devuelve la cadena `"null"` cuando la clave no existe o su valor es
 * null (porque `JSON.toString(null)` es `"null"`), y en JVM devuelve `null`, lo que
 * provoca `NullPointerException` al encadenar `.takeIf { it.isNotEmpty() }`.
 */
fun JSONObject.optStringOrNull(key: String): String? {
    val value = opt(key)
    if (value == null || value === JSONObject.NULL) return null
    val text = value.toString()
    return text.takeIf { it.isNotEmpty() }
}
