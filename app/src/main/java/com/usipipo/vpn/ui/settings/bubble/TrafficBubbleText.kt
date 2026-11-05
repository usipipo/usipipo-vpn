package com.usipipo.vpn.ui.settings.bubble

import com.usipipo.vpn.ui.details.formatRate
import com.usipipo.vpn.ui.details.uptimeText

/**
 * Texto de la burbuja de tráfico. Función pura, sin Android: se prueba en la JVM.
 *
 * Cuando falta un dato se muestra «—» en lugar de dejar el hueco vacío: la burbuja
 * se dibuja desde el servicio, así que no puede pedirle nada a la pantalla.
 */
object TrafficBubbleText {

    /** Velocidad en bytes por segundo; «—» si todavía no hay muestra. */
    fun speed(bytesPerSecond: Long?): String =
        if (bytesPerSecond == null) SIN_DATO else formatRate(bytesPerSecond)

    /** Tiempo de conexión; «—» si el túnel no da hora de conexión. */
    fun time(sinceMs: Long?, nowMs: Long): String =
        if (sinceMs == null || sinceMs <= 0L) SIN_DATO else uptimeText(sinceMs, nowMs)

    /** País del servidor: nombre si se tiene, si no el código, y si no «—». */
    fun country(countryName: String?, countryCode: String?): String =
        countryName?.takeIf { it.isNotBlank() }
            ?: countryCode?.takeIf { it.isNotBlank() }
            ?: SIN_DATO

    private const val SIN_DATO = "—"
}
