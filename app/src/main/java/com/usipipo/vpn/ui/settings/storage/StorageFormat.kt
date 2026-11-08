package com.usipipo.vpn.ui.settings.storage

import java.util.Locale

/**
 * Tamaños legibles para el resumen de almacenamiento.
 *
 * Función pura, sin Android: se prueba en la JVM. La locale se inyecta con el valor
 * por defecto del sistema, igual que `formatRate` en `ui.details`, para que los
 * tests no dependan de la máquina que los ejecuta.
 */
object StorageFormat {

    /** Bytes como texto: `0 B`, `512 B`, `1,5 KB`, `2,5 MB`, `1,0 GB`. */
    fun readable(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes <= 0) return "0 B"
        val unidades = listOf(B, KB, MB, GB)
        var valor = bytes.toDouble()
        var indice = 0
        while (valor >= 1024.0 && indice < unidades.lastIndex) {
            valor /= 1024.0
            indice++
        }
        // `%.1f` muestra el decimal siempre; el patrón `#.#` de DecimalFormat se lo
        // salta cuando la fracción es cero y «1,0 KB» salía como «1 KB».
        val texto = when {
            indice == 0 -> String.format(locale, "%d", bytes)
            valor >= 100.0 -> String.format(locale, "%.0f", valor)
            else -> String.format(locale, "%.1f", valor)
        }
        return "$texto ${unidades[indice]}"
    }

    /** Suma de las partes que se muestran en el resumen. */
    fun total(
        profilesBytes: Long,
        logBytes: Long,
        tempBytes: Long,
    ): Long = profilesBytes + logBytes + tempBytes

    private const val B = "B"
    private const val KB = "KB"
    private const val MB = "MB"
    private const val GB = "GB"
}
