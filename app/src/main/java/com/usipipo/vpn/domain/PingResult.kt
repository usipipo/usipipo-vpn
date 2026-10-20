package com.usipipo.vpn.domain

/**
 * Resultado de un ping hacia un destino dentro del túnel.
 *
 * @param ms milisegundos de latitud, null si solo se sabe que responde (isReachable) o falló.
 * @param error true si no se pudo obtener una medición válida.
 */
data class PingResult(
    val ms: Long?,
    val error: Boolean = false,
)
