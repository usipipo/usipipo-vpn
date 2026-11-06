package com.usipipo.vpn.backup

/**
 * Nombres libres para perfiles importados.
 *
 * El almacén rechaza dos perfiles con el mismo nombre, así que cuando el usuario
 * decide «mantener ambos» hay que proponer uno que esté libre. Se añade un número
 * entre paréntesis y se sube hasta encontrar hueco.
 */
object BackupNames {

    /** Primer nombre libre a partir de [base]; si [base] está libre, se devuelve tal cual. */
    fun suggestUnique(base: String, taken: List<String>): String {
        val limpio = base.trim()
        if (taken.none { it.trim().equals(limpio, ignoreCase = true) }) return limpio
        var numero = 2
        while (true) {
            val propuesta = "$limpio ($numero)"
            if (taken.none { it.trim().equals(propuesta, ignoreCase = true) }) return propuesta
            numero++
        }
    }
}
