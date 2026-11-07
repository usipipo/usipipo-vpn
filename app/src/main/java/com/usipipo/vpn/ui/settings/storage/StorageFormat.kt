package com.usipipo.vpn.ui.settings.storage

import java.text.DecimalFormat
import java.util.Locale

/**
 * Tamaños legibles para el resumen de almacenamiento.
 *
 * Función pura, sin Android: se prueba en la JVM. Los decimales usan la locale del
 * sistema porque el texto se muestra tal cual al usuario.
 */
object StorageFormat {

    /** Bytes como texto: `0 B`, `1,5 KB`, `2,5 MB`, `1,0 GB`. */
    fun readable(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val unidades = listOf(B, KB, MB, GB)
        var valor = bytes.toDouble()
        var indice = 0
        while (valor >= 1024.0 && indice < unidades.lastIndex) {
            valor /= 1024.0
            indice++
        }
        val entero = valor.toLong()
        val texto = if (indice == 0 || valor >= 100.0) {
            DecimalFormat("#", SIMBOLOS).format(entero)
        } else {
            DecimalFormat("#.#", SIMBOLOS).format(valor)
        }
        return "$texto ${unidades[indice]}"
    }

    /** Suma de las partes que se muestran en el resumen. */
    fun total(
        profilesBytes: Long,
        logBytes: Long,
        geoCacheBytes: Long,
        pingBytes: Long,
        tempBytes: Long,
    ): Long = profilesBytes + logBytes + geoCacheBytes + pingBytes + tempBytes

    private val SIMBOLOS = DecimalFormatSymbols(Locale.getDefault())
    private const val B = "B"
    private const val KB = "KB"
    private const val MB = "MB"
    private const val GB = "GB"
}
