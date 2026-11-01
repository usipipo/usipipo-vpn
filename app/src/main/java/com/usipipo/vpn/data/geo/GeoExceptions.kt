package com.usipipo.vpn.data.geo

/**
 * El proveedor principal no tiene token en esta compilación: no se consulta.
 * El respaldo cubre la geolocalización en ese caso.
 */
class MissingTokenException : Exception("Falta el token de IPinfo Lite")

/**
 * El proveedor respondió con un código HTTP de error.
 *
 * @param provider nombre del proveedor (para el registro).
 * @param status código HTTP recibido.
 */
class GeoHttpException(val provider: String, val status: Int) :
    Exception("$provider respondió HTTP $status")

/** La respuesta del proveedor no se pudo interpretar (JSON inválido). */
class GeoResponseException(message: String) : Exception(message)

/**
 * Fallaron los dos proveedores de geolocalización.
 *
 * @param primaryError fallo del proveedor principal.
 * @param fallbackError fallo del proveedor de respaldo.
 */
class GeoProvidersException(
    val primaryError: Throwable,
    val fallbackError: Throwable,
) : Exception(
    "Geolocalización falló: principal: ${describeGeoError(primaryError)}; respaldo: ${describeGeoError(fallbackError)}",
)

/** Describe un fallo sin copiar su mensaje: podría llevar la URL con el token. */
internal fun describeGeoError(error: Throwable): String = when (error) {
    is GeoHttpException -> "${error.provider} respondió HTTP ${error.status}"
    is MissingTokenException -> "falta el token del proveedor principal"
    is RateLimitedException -> "límite de tasa de peticiones"
    is GeoProvidersException -> error.message.orEmpty()
    else -> error::class.simpleName ?: "error desconocido"
}
