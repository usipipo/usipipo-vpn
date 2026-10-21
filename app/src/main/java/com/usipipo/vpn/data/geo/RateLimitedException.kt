package com.usipipo.vpn.data.geo

import java.io.IOException

/**
 * La API externa rechazó la consulta por límite de tasa (HTTP 429).
 *
 * @param retryAfterSeconds segundos que faltan para que el contador se reinicie.
 *   Si la API no lo comunica, se asume un valor conservador.
 */
class RateLimitedException(val retryAfterSeconds: Long) :
    IOException("Límite de tasa de la API (reintentar en ${retryAfterSeconds}s)")
