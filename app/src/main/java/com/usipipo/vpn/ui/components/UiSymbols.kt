package com.usipipo.vpn.ui.components

/**
 * Símbolos de UI compartidos.
 *
 * `GLOBE_EMOJI` es el globo terráqueo (U+1F310) que se muestra cuando no se conoce
 * el país del servidor. Antes se usaba U+26E2, que es el símbolo astronómico de Urano.
 */
const val GLOBE_EMOJI: String = "\uD83C\uDF10"

/**
 * Convierte un código ISO 2 letras en emoji de bandera (indicadores regionales).
 * Devuelve cadena vacía si el código no son dos letras ASCII.
 */
fun flagEmoji(countryCode: String): String {
    val code = countryCode.uppercase()
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return ""
    return code.map { String(Character.toChars(it.code - 'A'.code + 0x1F1E6)) }.joinToString("")
}
