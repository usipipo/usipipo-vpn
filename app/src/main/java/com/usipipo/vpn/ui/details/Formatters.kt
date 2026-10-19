package com.usipipo.vpn.ui.details

import java.util.Locale

/** Por encima de este umbral (en bits/s) se muestra en Mbps; evita «1000 Kbps». */
private const val MBPS_THRESHOLD_BITS = 999_500L

/** Velocidad en bytes por segundo, mostrada en bits como en el mockup («60.00 Mbps»). */
fun formatRate(bytesPerSecond: Long, locale: Locale = Locale.getDefault()): String {
    val bits = bytesPerSecond * 8L
    return if (bits >= MBPS_THRESHOLD_BITS) {
        String.format(locale, "%.2f Mbps", bits / 1_000_000.0)
    } else {
        String.format(locale, "%.0f Kbps", bits / 1_000.0)
    }
}

/** `HH:MM:SS`; las horas pueden pasar de 99. */
fun formatUptime(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    return String.format(
        Locale.US,
        "%02d:%02d:%02d",
        totalSeconds / 3_600L,
        (totalSeconds / 60L) % 60L,
        totalSeconds % 60L,
    )
}

/** Sin hora de conexión (`sinceMs` en 0) no se inventa un tiempo. */
fun uptimeText(sinceMs: Long, nowMs: Long): String =
    if (sinceMs <= 0L) "--:--:--" else formatUptime(nowMs - sinceMs)
