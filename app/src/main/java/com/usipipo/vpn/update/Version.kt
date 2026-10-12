package com.usipipo.vpn.update

/**
 * Versión de la app en `major.minor.patch`, comparable por componentes.
 * Los componentes ausentes valen 0, así que `1.2` equivale a `1.2.0`.
 *
 * Es un `data class` y no un `value class` a propósito: los value class de Kotlin
 * solo aceptan un parámetro en el constructor primario, así que tres componentes
 * no caben sin empaquetarlos a mano (y pagar rangos límite artificiales).
 */
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {

        /** Acepta `"1.2.3"`, `"v1.2.3"`, `"1.2"`, con espacios alrededor. Si no, null. */
        fun parse(raw: String?): Version? {
            val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val sinPrefijo = when (text.first()) {
                'v', 'V' -> text.drop(1)
                else -> text
            }
            if (sinPrefijo.isEmpty()) return null
            val partes = sinPrefijo.split(".")
            if (partes.isEmpty() || partes.size > 3) return null
            val componentes = IntArray(3)
            partes.forEachIndexed { indice, parte ->
                if (!parte.all { it in '0'..'9' }) return null
                componentes[indice] = parte.toIntOrNull() ?: return null
            }
            return Version(componentes[0], componentes[1], componentes[2])
        }
    }
}
